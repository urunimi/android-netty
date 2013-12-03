package com.nhn.nni.protocol;

import org.json.JSONException;
import org.json.JSONObject;
import static com.nhn.nni.core.NNIProtocol.*;

/**
 * Server --> Client
 * {"command":3589,"parameter":[[1,"0cf16ad49bddd708d78d9fda824f775c","naverline",0,"npush","{\"w\":\"、q\",\"content\":\"RECEIVE_MESSAGE\",\"v\":\"NHST_Fああああ\",\"t\":\"1\",\"r\":\"237\",\"m\":\"re1a3f09eca282d7b7b7c119594a30adb\",\"k\":\"MT\",\"i\":\"3335113\",\"btnType\":\"2\"}"]],"transid":0}
 * @author Hovan
 *
 */
public class NotificationRequest extends BaseResponse {

	public NotificationRequest(JSONObject jsonPacket) throws JSONException {
		super(jsonPacket);
		assert (mCommand != COMMAND_PUSHEVENT_SERVER);
	}
}
