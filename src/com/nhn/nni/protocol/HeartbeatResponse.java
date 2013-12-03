package com.nhn.nni.protocol;

import org.json.JSONException;
import org.json.JSONObject;
import static com.nhn.nni.core.NNIProtocol.*;

/**
 * Server --> Client
 * {"command":34897,"parameter":[],"transid":2106}
 * @author 유병우(urunimi@nhn.com)
 *
 */
public class HeartbeatResponse extends BaseResponse {

	public HeartbeatResponse(JSONObject jsonPacket) throws JSONException {
		super(jsonPacket);
		assert (mCommand != COMMAND_HEALTHCHECK_SERVER);
	}
}
