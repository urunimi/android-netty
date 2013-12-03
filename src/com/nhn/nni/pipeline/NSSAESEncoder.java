/*
 * @(#) NSSAESEncoder.java Oct 24, 2012 
 *
 * Copyright 2012 NHN Corp. All rights Reserved. 
 * NHN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.nhn.nni.pipeline;

import java.security.Key;

import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.nhn.nni.core.Logger;
import com.nhn.nni.core.NNIConstant;
import com.nhn.nni.network.NNIServiceHandler;
import com.nhn.nni.util.PermissionManager.WakeLockWrapper;
import com.nhn.nni.util.SecurityUtil;

/**
 * 기존에 jsonString 을 받도록 한 것을 가독성을 위해 object로 변경 [2013-03-19, 유병우]
 * @author 유승현, 유병우
 */
public class NSSAESEncoder extends SimpleChannelHandler {
	private String algorithm;
	private Key securitykey;
	
	public NSSAESEncoder(String algorithm, Key securitykey) {
		this.algorithm = algorithm;
		this.securitykey = securitykey;
	}

	private int requiredBlock(int total, int blocksize) {
		if (0 == total) {
			return 0;
		}

		return ((total - 1) / blocksize) + 1;
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
		
		WakeLockWrapper wakeLock = WakeLockWrapper.getWakeLockInstance(NNIServiceHandler.getContext(), NSSAESEncoder.class.getSimpleName());
		wakeLock.acquire();

		try {
			String jsonString = event.getMessage().toString();

			Logger.i(String.format("WRITE [%s]", jsonString));
			
			// 암호화 작업

			byte[] decryptMessage = jsonString.getBytes(NNIConstant.ENCODING_UTF8);
			int blksz = requiredBlock(decryptMessage.length + 4, 16);
			byte padsz = (byte)(blksz * 16 - (decryptMessage.length + 4));

			ChannelBuffer decryptBuffer = new BigEndianHeapChannelBuffer(blksz * 16);
			decryptBuffer.writeInt(decryptMessage.length);
			decryptBuffer.writeBytes(decryptMessage);

			byte[] encryptMessage = SecurityUtil.encryptMessage(decryptBuffer.array(), securitykey, algorithm);

			int sendLen = encryptMessage.length + 1;
			int lenMagic2 = sendLen | (SecurityUtil.generateMagicCode(sendLen) << 24);

			ChannelBuffer cb = new BigEndianHeapChannelBuffer(encryptMessage.length + 5);
			cb.writeInt(lenMagic2);
			cb.writeBytes(encryptMessage);
			cb.writeByte(padsz);

			// 마지막 byte는 NSS 서버에서 아래와 같이 1byte를 더 보낸다.
			/*
			 * out의 length는 항상 16n+1 byte이다. 마지막 1 byte는 padding size를 나타내며 마지막
			 * 16byte block중 뒤 쪽의 padding size만큼은 유효한 data가 아님을 나타낸다. e.g.) 13 byte를
			 * encoding하면 전체 17 byte가 나오며 padding byte는 3이 된다.
			 */

			Channels.write(ctx, event.getFuture(), cb);
		} finally {
			wakeLock.release();
		}
	}
}
