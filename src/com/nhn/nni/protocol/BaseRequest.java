package com.nhn.nni.protocol;

import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.nhn.nni.core.NNIProtocol.*;

public abstract class BaseRequest {
	
	final static AtomicInteger TRANS_ID_INDEXER = new AtomicInteger(22);
	
	int mCommand = setCommand();
	int mTransId = TRANS_ID_INDEXER.getAndIncrement();
	
	JSONArray mParameters;
	
	abstract int setCommand();
	abstract JSONArray setParameters();
	
	@Override
	public String toString() {
		JSONObject jsonPacket = new JSONObject();
		mParameters = setParameters();
		try {
			jsonPacket.put(PROTOCOL_COMMAND, mCommand);
			jsonPacket.put(PROTOCOL_TRANSACTIONID, mTransId);
			if(mParameters != null) {
				jsonPacket.put(PROTOCOL_PARAMETER, mParameters);
			}
			return jsonPacket.toString();
		} catch (JSONException e) {
			return null;
		}
	}
}
