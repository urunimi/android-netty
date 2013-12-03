package com.nhn.nni.protocol;

import static com.nhn.nni.core.NNIProtocol.COMMAND_CONNECT_SERVER;
import static com.nhn.nni.core.NNIProtocol.COMMAND_HEALTHCHECK_SERVER;
import static com.nhn.nni.core.NNIProtocol.COMMAND_PUSHEVENT_SERVER;
import static com.nhn.nni.core.NNIProtocol.COMMAND_SUBSCRIBE_SERVER;
import static com.nhn.nni.core.NNIProtocol.COMMAND_UNSUBSCRIBE_SERVER;
import static com.nhn.nni.core.NNIProtocol.PROTOCOL_COMMAND;
import static com.nhn.nni.core.NNIProtocol.PROTOCOL_PARAMETER;
import static com.nhn.nni.core.NNIProtocol.PROTOCOL_TRANSACTIONID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseResponse {
	
	int mCommand;
	int mTransId;
	
	JSONArray mParameters;
	
	public BaseResponse(JSONObject jsonPacket) throws JSONException {
		mCommand = jsonPacket.getInt(PROTOCOL_COMMAND);
		mTransId = jsonPacket.getInt(PROTOCOL_TRANSACTIONID);
		mParameters = jsonPacket.getJSONArray(PROTOCOL_PARAMETER);
	}
	
	@Override
	public String toString() {
		JSONObject jsonPacket = new JSONObject();
		try {
			jsonPacket.put(PROTOCOL_COMMAND, mCommand);
			jsonPacket.put(PROTOCOL_TRANSACTIONID, mTransId);
			jsonPacket.put(PROTOCOL_PARAMETER, mParameters);
			return jsonPacket.toString();
		} catch (JSONException e) {
			return null;
		}
	}
	
	public static Object getInstance(String jsonString) throws JSONException {
		JSONObject packetJson = new JSONObject(jsonString);
		
		switch(packetJson.getInt(PROTOCOL_COMMAND)) {
		case COMMAND_CONNECT_SERVER : {
			return new ConnectResponse(packetJson);
		}
		case COMMAND_SUBSCRIBE_SERVER : {
			return new SubscribeResponse(packetJson);
		}
		case COMMAND_UNSUBSCRIBE_SERVER: {
			return new UnsubscribeResponse(packetJson);
		}
		case COMMAND_HEALTHCHECK_SERVER: {
			return new HeartbeatResponse(packetJson);
		}
		case COMMAND_PUSHEVENT_SERVER: {
			return new NotificationRequest(packetJson);
		}
		default: {
			return jsonString;
		}
		}
	}
	
	public JSONArray getParameters() {
		return mParameters;
	}
}
