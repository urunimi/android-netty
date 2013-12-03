/*
 * @(#) NSSAESDecoder.java Oct 24, 2012 
 *
 * Copyright 2012 NHN Corp. All rights Reserved. 
 * NHN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.nhn.nni.pipeline;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.json.JSONException;

import com.nhn.nni.core.Logger;
import com.nhn.nni.core.NNIConstant;
import com.nhn.nni.network.InvalidRequestException;
import com.nhn.nni.network.NNIServiceHandler;
import com.nhn.nni.protocol.BaseResponse;
import com.nhn.nni.util.PermissionManager.WakeLockWrapper;
import com.nhn.nni.util.SecurityUtil;

/**
 * 기존에 jsonString 을 보내도록 한 것을 가독성을 위해 object로 변경 [2013-03-19, 유병우]
 * 
 * @author 유승현, 유병우
 */
public class NSSAESDecoder extends FrameDecoder {
	// private final Logger log = LoggerFactory.getLogger(NSSAESDecoder.class);
	private Cipher cipher;
	private static final int MAX_BODY_SIZE = 6500000;
	private String algorithm;
	private Key securityKey;

	public static String[] byteToHex;
	static {
		byteToHex = new String[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
		for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
			byteToHex[i - Byte.MIN_VALUE] = String.format("%x", (byte) i);
		}
	}

	public NSSAESDecoder(String algorithm, Key securitykey) {
		super();
		this.algorithm = algorithm;
		this.securityKey = securitykey;

		try {
			cipher = Cipher.getInstance(this.algorithm);
		} catch (NoSuchAlgorithmException e) {
			// 발생할수 없음, 발생하면 버그
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (NoSuchPaddingException e) {
			// 발생할수 없음, 발생하면 버그
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void printPacket(byte[] packet) {
		StringBuffer sb = new StringBuffer();
		sb.append("received = ");
		for (byte bt : packet) {
			sb.append(byteToHex[bt - Byte.MIN_VALUE]);
			sb.append(" ");
		}

		Logger.w(sb.toString());
	}

	private void printChannelBuffer(ChannelBuffer cb) {
		StringBuffer sb = new StringBuffer();
		sb.append("received = ");
		while (cb.readable()) {
			sb.append(byteToHex[cb.readByte() - Byte.MIN_VALUE]);
			sb.append(" ");
		}

		Logger.w(sb.toString());
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws InvalidRequestException, JSONException {

		WakeLockWrapper wakeLock = WakeLockWrapper.getWakeLockInstance(NNIServiceHandler.getContext(), NSSAESDecoder.class.getSimpleName());
		wakeLock.acquire();

		try {
			// 메시지를 읽어도 될만큼 데이터가 도착했는지 검사
			if (buffer.readableBytes() < 4) {
				// log.debug("receiving packet is NOT complete, readable Bytes = "
				// +
				// buffer.readableBytes());
				return null;
			}
			int nBodySize = buffer.getInt(0); // 길이를 읽음
			if (!SecurityUtil.checkMagicCode(nBodySize)) {
				printChannelBuffer(buffer);
				channel.close();
				throw new InvalidRequestException("magic code bit is invalid... code = 0x" + Integer.toHexString(nBodySize));
			}
			int bodyLength = SecurityUtil.getBodyLength(nBodySize);
			if (!SecurityUtil.checkBodyLength(bodyLength)) {
				printChannelBuffer(buffer);
				channel.close();
				throw new InvalidRequestException("body size is invalid, size = " + bodyLength);
			}

			if (buffer.readableBytes() < bodyLength + 4) {
				// log.debug("receiving packet is NOT complete, readable Bytes = "
				// +
				// buffer.readableBytes() + "packet len = " + bodyLength);
				return null;
			}

			// decoding
			buffer.skipBytes(4);

			byte[] recvBuff = new byte[bodyLength - 1];
			buffer.readBytes(recvBuff, 0, bodyLength - 1);
			buffer.skipBytes(1); // java 에서 복호화하는데 마지막 1byte는 필요 없음

			try {
				cipher.init(Cipher.DECRYPT_MODE, securityKey);
			} catch (InvalidKeyException e) {
				// 발생할수 없음, 발생하면 버그
				e.printStackTrace();
				channel.close();
				throw new RuntimeException(e);
			}

			byte[] decryptMessage;
			try {
				decryptMessage = cipher.doFinal(recvBuff);
			} catch (IllegalBlockSizeException e) {
				Logger.w("decrypt fail, cause" + e.getMessage());
				printPacket(recvBuff);
				channel.close();
				throw new InvalidRequestException("decrypt fail");
			} catch (BadPaddingException e) {
				Logger.w("decrypt fail, cause" + e.getMessage());
				printPacket(recvBuff);
				channel.close();
				throw new InvalidRequestException("decrypt fail");
			}

			if (decryptMessage.length < 4) {
				channel.close();
				Logger.w("decripted message should longer than 4 bytes, size = " + decryptMessage.length);
				throw new InvalidRequestException("decripted message should longer than 4 bytes");
			}

			ChannelBuffer cb = new BigEndianHeapChannelBuffer(decryptMessage);
			// int jsonLen = (decryptMessage[0] << 24) + (decryptMessage[1] <<
			// 16) +
			// (decryptMessage[2] << 8) + (decryptMessage[3]);
			int jsonLen = cb.readInt();

			if (jsonLen <= 0 || jsonLen >= MAX_BODY_SIZE) {
				Logger.w("Invalid json len = " + jsonLen);

				try {
					Logger.w("plain text = " + new String(decryptMessage, NNIConstant.ENCODING_UTF8));
				} catch (UnsupportedEncodingException e) {
					// 발생할수 없음, 발생하면 버그
					throw new RuntimeException(e);
				}

				channel.close();
				throw new InvalidRequestException("Invalid json length");
			}

			if (decryptMessage.length < jsonLen + 4) {
				Logger.w("decripted message's length should be [" + (jsonLen + 4) + "]");

				try {
					Logger.w("plain text = " + new String(decryptMessage, NNIConstant.ENCODING_UTF8));
				} catch (UnsupportedEncodingException e) {
					// 발생할수 없음, 발생하면 버그
					throw new RuntimeException(e);
				}

				channel.close();
				throw new InvalidRequestException("decripted message's length should be [" + (jsonLen + 4) + "]");
			}

			String jsonString;
			try {
				jsonString = new String(decryptMessage, 4, jsonLen, NNIConstant.ENCODING_UTF8);
			} catch (UnsupportedEncodingException e) {
				// 발생할수 없음, 발생하면 버그
				throw new RuntimeException(e);
			}

			Logger.i(String.format("READ  [%s]", jsonString));

			return BaseResponse.getInstance(jsonString);
		} finally {
			wakeLock.release();
		}
	}
}
