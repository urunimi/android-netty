package com.nhn.nni.protocol;

import org.json.JSONArray;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.nhn.nni.core.NNIConstant;
import com.nhn.nni.core.NNIProtocol;
import com.nhn.nni.network.NNIServiceHandler;
import com.nhn.nni.util.PermissionManager.OperatorWrapper;

/**
 * Connect Request 패킷을 정의한다.
 *  Client --> Server
 * {"transid":241,"command":3585,"parameter":[1,2,200,1,"430;2;1","5.3.2","SHV-E210S;samsung;IMM76D.E210SKSALJ1;15"]}
 * 
 * @author 유병우(urunimi@nhn.com)
 */
public class ConnectRequest extends BaseRequest {


	@Override
	int setCommand() {
		return NNIProtocol.COMMAND_CONNECT_CLIENT;
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
		JSONArray parameterArray = new JSONArray();
		parameterArray.put(NNIProtocol.PROTOCOL_VERSION);
		parameterArray.put(NNIProtocol.SERVICE_TYPE);
		parameterArray.put(NNIProtocol.DEVICE_TYPE);

		ConnectivityManager connectivityManager = (ConnectivityManager) NNIServiceHandler.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		
		if (activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			parameterArray.put(NNIProtocol.NETWORK_TYPE_WIFI);
		} else {	//그외의 상태는 모두 3G로 인식
			parameterArray.put(NNIProtocol.NETWORK_TYPE_3G);
		}
		
		OperatorWrapper operator = OperatorWrapper.getInstance(NNIServiceHandler.getContext());
		
		if(operator.getCountryCode() >= 0) {
			StringBuilder sb = new StringBuilder();
			String operatorInformation = sb.append(operator.getCountryCode()).append(';').append(operator.getOperatorCode()).append(';').append(operator.getNetworkType()).toString();
			parameterArray.put(operatorInformation);
		} else {
			parameterArray.put("--");
		}
		
		parameterArray.put(NNIConstant.VERSION_NAME);
		return parameterArray;
	}
}
