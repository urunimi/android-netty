package com.nhn.nni.core;

public class NNIProtocol {
	/**
	 * NSS(C++)에서의 정의 - 참고사항
	 * const int32_t REQ_CRS_CONNECT = 0x0E01;
	 * const int32_t RES_SRC_CONNECT = 0x8E01;
	 * 
	 * const int32_t REQ_CRS_DISCONNECT = 0x0E02;
	 * 
	 * const int32_t REQ_CRS_SUBSCRIBE = 0x0E03;
	 * const int32_t RES_SRC_SUBSCRIBE = 0x8E03;
	 * 
	 * const int32_t REQ_CRS_UNSUBSCRIBE = 0x0E04;
	 * const int32_t REQ_CRS_UNSUBSCRIBE_NSS = 0x0E06;
	 * 
	 * const int32_t RES_SRC_UNSUBSCRIBE = 0x8E04;
	 * 
	 * const int32_t NOTI_SRC_PUSHEVENT= 0x0E05; 
	 * const int32_t NOTI_CRS_PUSHEVENT= 0x8E05;
	 * 
	 * const int32_t REQ_SRC_HEALTHCHECK= 0x0850;
	 * const int32_t RES_CRS_HEALTHCHECK= 0x8850;
	 * 
	 * const int32_t REQ_CRS_CLIENT_HEALTHCHECK= 0x0851;
	 * const int32_t RES_SRC_CLIENT_HEALTHCHECK= 0x8851;
	 */

	/**
	 * COMMAND_XXXX NNI클라이언트와의 미리 정의된 명령
	 */
	public static final int COMMAND_CONNECT_CLIENT = 0x0E01; // 3585
	public static final int COMMAND_CONNECT_SERVER = 0x8E01; // 36353

	public static final int COMMAND_DISCONNECT_CLIENT = 0x0E02; // 3586

	public static final int COMMAND_SUBSCRIBE_CLIENT = 0x0E03; // 3587
	public static final int COMMAND_SUBSCRIBE_SERVER = 0x8E03; // 36355

	public static final int COMMAND_UNSUBSCRIBE_CLIENT = 0x0E04; // 3588
	public static final int COMMAND_UNSUBSCRIBE_SERVER = 0x8E04; // 36356

	public static final int COMMAND_HEALTHCHECK_CLIENT = 0x0851; // 2129
	public static final int COMMAND_HEALTHCHECK_SERVER = 0x8851; // 34897

	public static final int COMMAND_PUSHEVENT_SERVER = 0x0E05; // 3589
	public static final int COMMAND_PUSHEVENTRES_CLIENT = 0x8E05; // 36357

	public static final int COMMAND_HEALTHCHECK_SERVERPING_CLIENT = 0x0850; // 2128
	public static final int COMMAND_HEALTHCHECK_SERVERPING_SERVER = 0x8850; // 34896

	public static final int COMMAND_RELAYEVENT_SERVER = 0x00C8; // 200

	/**
	 * NIMM 관련 
	 */
	public static final String NIMM_METHODNANE_RELAY = "relay";
	public static final String NIMM_METHODNANE_RELAYEVENT = "relayEvent";
	public static final String NIMM_METHODNANE_CHECKCONECTION = "checkConnection";	
	public static final String NIMM_METHODNANE_SERVERINFO = "serverinfo";

	/**
	 * PROTOCOL 관련
	 */
	public static final int PROTOCOL_VERSION = 1;
	public static final int TRANSACTION_ID_ZERO = 0;
	public static final int CATEGORY_ID_ZERO = 0;
	public static final String SENDER_NPUSH = "npush";
	
	
	final public static int SERVICE_TYPE = 2; //(PC = 0, web = 1, mobile=2)
	final public static int DEVICE_TYPE = 200; //(pc = 0, web=1, ipod=100, iphone = 101, ipad=102, android=200, win-mobile=300,…)

	final public static String PROTOCOL_COMMAND = "command";
	final public static String PROTOCOL_TRANSACTIONID = "transid";
	final public static String PROTOCOL_PARAMETER = "parameter";
	
	final public static int NETWORK_TYPE_3G = 0;
	final public static int NETWORK_TYPE_WIFI = 1;
}
