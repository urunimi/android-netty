package com.nhn.nni.protocol;

import org.json.JSONArray;

import com.nhn.nni.core.NNIProtocol;
import com.nhn.nni.network.NNIDataManager;
import com.nhn.nni.network.NNIDataManager.SubscribeInfo;

/**
 * Unsubscribe Request 패킷을 정의한다.
 *  Client --> Server
 * {"transid":242,"command":3588,"parameter":[1,2,200,"nss2-l:2:200:1347289405478:837",[["nni.naverline.ab7cfbeb-4283-4e3d-bb47-83613250dbbe","naverline",0]]]}
 * @author 유병우(urunimi@nhn.com)
 */
public class UnsubscribeRequest extends BaseRequest {


	@Override
	int setCommand() {
		return NNIProtocol.COMMAND_SUBSCRIBE_CLIENT;
	}
	
	@Override
	JSONArray setParameters() {
		JSONArray parameterArray = new JSONArray();
		parameterArray.put(NNIProtocol.PROTOCOL_VERSION);
		parameterArray.put(NNIProtocol.SERVICE_TYPE);
		parameterArray.put(NNIProtocol.DEVICE_TYPE);

		if (NNIDataManager.getInstance().getConnectionId() == null
				|| NNIDataManager.getInstance().getConnectionId().length() == 0) {
			return null;
		}
		parameterArray.put(NNIDataManager.getInstance().getConnectionId());

		int serviceCount = 0;
		JSONArray serviceBlockArray = new JSONArray();

		for (SubscribeInfo subscribeInfo : NNIDataManager.getInstance().getSubscribeMap().values()) {
			if (subscribeInfo.getType() == SubscribeInfo.UNSUBSCRIBE_TYPE && subscribeInfo.isSuccess() == false) {
				JSONArray serviceArray = new JSONArray();
				serviceArray.put(subscribeInfo.getTargetId());
				serviceArray.put(subscribeInfo.getServiceId());
				serviceArray.put(NNIProtocol.CATEGORY_ID_ZERO);
				serviceBlockArray.put(serviceArray);
				subscribeInfo.setTransId(mTransId);
				serviceCount++;
			}
		}

		parameterArray.put(serviceBlockArray);
		if (serviceCount == 0) {
			return null;
		}

		return parameterArray;
	}
}
