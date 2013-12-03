package com.nhn.nni.protocol;

import org.json.JSONArray;

import com.nhn.nni.core.NNIProtocol;

/**
 * Notification Response 패킷을 정의한다.
 *  Client --> Server
 *  {"transid":27,"command":36357,"parameter":[1,"0cf16ad49bddd708d78d9fda824f775c","naverline",0,"nni.naverline.580fcc19-ff56-4b05-827c-78ee91c6d84f"]}
 * @author 유병우(urunimi@nhn.com)
 */
public class NotificationResponse extends BaseRequest {

	String mEventId;
	String mServiceId;
	String mTargetId;

	@Override
	int setCommand() {
		return NNIProtocol.COMMAND_PUSHEVENTRES_CLIENT;
	}
	
	public NotificationResponse(String eventId, String serviceId, String targetId) {
		mEventId = eventId;
		mServiceId = serviceId;
		mTargetId = targetId;
	}
	
	@Override
	JSONArray setParameters() {
		JSONArray parameterArray = new JSONArray();
		parameterArray.put(NNIProtocol.PROTOCOL_VERSION);
		parameterArray.put(mEventId);
		parameterArray.put(mServiceId);
		parameterArray.put(NNIProtocol.CATEGORY_ID_ZERO);
		parameterArray.put(mTargetId);
		return parameterArray;
	}
}
