package com.nhn.nni.protocol;

import org.json.JSONArray;

import com.nhn.nni.core.NNIProtocol;

/**
 * Connect Request 패킷을 정의한다.
 *  Client --> Server
 * {"transid":2106,"command":2129}
 * 
 * @author 유병우(urunimi@nhn.com)
 */
public class HeartbeatRequest extends BaseRequest {


	@Override
	int setCommand() {
		return NNIProtocol.COMMAND_HEALTHCHECK_CLIENT;
	}
	
	/**
	 * Type : Description
	 * Number : Protocol Version
	 * Number : Service Type (PC = 0, web = 1, mobile=2) 현재 '2' 만 사용
	 * Number : deviceType (pc = 0, web=1, ipod=100, iphone = 101, ipad=102, android=200, win-mobile=300,…) 현재 '200' 만 사용
	 * Number : 3G=0, WIFI=1 (default=0), 파라메터가 없을 수 있음
	 * String : 통신사 정보, 'MCC;MNC;Network-Type' 형식으로 들어 있음
	 * 			Network-Type 값이 13이면 LTE로 접속, 파라메터가 없을 수 있음 
	 * String : 버전 정보, "6.0.0" 또는 "6.0.0-RC2" 와 같은 형식, 파라메터가 없을 수 있음
	 * String : 클라이언트에서 보내는 정보, 로직에는 사용하지 않고 로그에만 남긴다, 파라메터가 없을 수 있음
	 * 
	 * eg. [1,2,200,1,"430;2;1","5.3.2","SHV-E210S;samsung;IMM76D.E210SKSALJ1;15"]
	 */
	@Override
	JSONArray setParameters() {
		return null;
	}
}
