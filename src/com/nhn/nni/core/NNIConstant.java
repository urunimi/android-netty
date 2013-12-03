package com.nhn.nni.core;

import java.net.InetSocketAddress;

import android.content.Context;

import com.nhn.nni.network.NNIServiceHandler;


public class NNIConstant extends com.nhn.nni.NNIConstant {
	
	/** 프로토콜에서 사용할 버전 이름 6.1.0 부터 Dynamic client ping을 지원한다. 그래서 NNI Global 버전의 경우 앞에 1을 더 첨가한다. */
	public static final String VERSION_NAME = String.valueOf(1) + VERSION_CODE + ".1.0";
	
	public static final boolean USE_ENCRYPTION	= true;
	
//	final static int CHINA_MCC = 460, KOREA_MCC = 450;
	
	static InetSocketAddress nPushServerAddress = null;
	
	/**
	 * NNI Server의 Host, port를 가져오는 함수 <br/>
	 * NNI_JAPAN의 경우 한국, 일본 서비스가 나뉘어진다.
	 *
	 * @return {@link InetSocketAddress}
	 */
	public static InetSocketAddress getServerHost() {
		final Context context = NNIServiceHandler.getContext();
		if (nPushServerAddress == null) {
			switch (NNIConstant.PACKAGE_TYPE) {
			case LINE:
			case GLOBAL:
			default:
//				int countryCode = OperatorWrapper.getInstance(context).getCountryCode();
				switch (NNIConstant.getClientType(context)) {
				case ALPHA: 
					nPushServerAddress = new InetSocketAddress("alpha.nniglobal.naver.com", 5228);
					break;
				
				case BETA: 
					nPushServerAddress = new InetSocketAddress("beta.nniglobal.naver.com", 5228);
					break;
				
				case REAL:
				case REAL_DEBUG:
				default: 
					nPushServerAddress = new InetSocketAddress("nniglobal.naver.com", 5228);
					break;
				}
			}
		}
		Logger.w(NNIConstant.class.getSimpleName() + "getServerHost() nPushServerAddress " + nPushServerAddress);
		return nPushServerAddress;
	}
	
	public static final String ENCODING_UTF8 = "UTF-8";
}
