package com.nhn.nni.protocol;

import static com.nhn.nni.core.NNIProtocol.COMMAND_CONNECT_SERVER;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Server --> Client
 * {"command":36353,"parameter":[0,"nss2-l:2:200:1347289405478:837",300,1800],"transid":241}
 * @author 유병우(urunimi@nhn.com)
 */
public class ConnectResponse extends BaseResponse {
	

	public ConnectResponse(JSONObject jsonPacket) throws JSONException {
		super(jsonPacket);
		assert (mCommand != COMMAND_CONNECT_SERVER);
	}
}
