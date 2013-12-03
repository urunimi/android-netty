package com.nhn.nni.protocol;

import org.json.JSONException;
import org.json.JSONObject;
import static com.nhn.nni.core.NNIProtocol.*;

/**
 * Server --> Client
 * {"command":36356,"parameter":[0],"transid":242}
 * @author 유병우(urunimi@nhn.com)
 */
public class UnsubscribeResponse extends BaseResponse {

	public UnsubscribeResponse(JSONObject jsonPacket) throws JSONException {
		super(jsonPacket);
		assert (mCommand != COMMAND_UNSUBSCRIBE_SERVER);
	}
}
