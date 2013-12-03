package com.nhn.nni.protocol;

import org.json.JSONException;
import org.json.JSONObject;
import static com.nhn.nni.core.NNIProtocol.*;

/**
 * Server --> Client
 * {"command":36355,"parameter":[0],"transid":242}
 * @author 유병우(urunimi@nhn.com)
 */
public class SubscribeResponse extends BaseResponse {

	public SubscribeResponse(JSONObject jsonPacket) throws JSONException {
		super(jsonPacket);
		assert (mCommand != COMMAND_SUBSCRIBE_SERVER);
	}
}
