/*
 * @(#) SecurityUtils.java Oct 12, 2012 
 *
 * Copyright 2012 NHN Corp. All rights Reserved. 
 * NHN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.nhn.nni.util;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.nhn.nni.core.Logger;

/**
 * @author hangsucho
 * 
 */
public class SecurityUtil {
//	private static final Logger LOG = LoggerFactory.getLogger(SecurityUtils.class);
	private static final int CS_MAGIC_BIT = 0x5;
	private static final int MAX_BODY_SIZE = 1000000;

	/**
	 *  전달된 메시지를 암호화 한다.
	 *  
	 *  TODO Exception handling
	 * @param message
	 * @return
	 */
	public static byte[] encryptMessage(byte[] message, Key secureKey, String algorithm) {
		Cipher cipher;
		byte[] encryptMessage = null;
		try {
			cipher = Cipher.getInstance(algorithm);
			cipher.init(Cipher.ENCRYPT_MODE, secureKey);
			encryptMessage = cipher.doFinal(message);
		} catch (NoSuchAlgorithmException e) {
			//throw e;
		} catch (NoSuchPaddingException e) {
			//throw e;
		} catch (InvalidKeyException e) {

		} catch (IllegalBlockSizeException e) {

		} catch (BadPaddingException e) {

		}

		return encryptMessage;
	}

	/**
	 *  암호화된  메시지를 복호화 한다.
	 *  
	 *  TODO Exception handling
	 *  
	 * @param encryptedMessage
	 * @return
	 */
	public static byte[] decryptMessage(byte[] encryptedMessage, Key secureKey, String algorithm) {

		Cipher cipher;
		byte[] decryptMessage = null;
		try {
			cipher = Cipher.getInstance(algorithm);
			cipher.init(Cipher.DECRYPT_MODE, secureKey);
			decryptMessage = cipher.doFinal(encryptedMessage);
		} catch (NoSuchAlgorithmException e) {
			//throw e;
		} catch (NoSuchPaddingException e) {
			//throw e;
		} catch (InvalidKeyException e) {

		} catch (IllegalBlockSizeException e) {

		} catch (BadPaddingException e) {

		}

		return decryptMessage;
	}

	public static boolean checkMagicCode(int bodySize) {

		if (getMagicBit(bodySize) != CS_MAGIC_BIT) {
			Logger.w("ClientSession> Magic Code is invalid");
			return false;
		}

		if (getMagicCode(bodySize) != generateMagicCode(bodySize)) {
			Logger.w("ClientSession> Magic Code is invalid");
			return false;
		}

		return true;
	}

	public static boolean checkBodyLength(int bodyLength) {

		// 길이가 맞는지 검사
		if (bodyLength < 0) {
			Logger.w("ClientSession> Body Size is invalid ");
			return false;
		}
		if (bodyLength > MAX_BODY_SIZE) {
			Logger.w("ClientSession> Body Size is too big ");
			return false;
		}

		return true;
	}

	/**
	 * Magic Code 에서 일반 숫자로 변경
	 * 
	 * @param bodySize
	 * @return
	 */
	public static int getBodyLength(int bodySize) {
		int len = getBodySize(bodySize); // Magic Code 에서 일반 숫자로 변경
		return len;
	}

	public static int generateMagicCode(int nInput) {
		int a = nInput & 0x00000055;
		int output = 0x50 | (a & 0x01) | ((a & 0x04) >> 1) | ((a & 0x10) >> 2) | ((a & 0x40) >> 3);
		return output;
	}

	private static int getMagicCode(int value) {
		return ((value & 0xff000000) >> 24);
	}

	private static int getMagicBit(int value) {
		return ((value & 0xf0000000) >> 28);
	}

	private static int getBodySize(int value) {
		return (value & ~0xff000000);
	}

}
