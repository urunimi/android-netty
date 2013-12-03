package com.nhn.nni.protocol;

import org.json.JSONArray;

import android.os.Build;

import com.nhn.nni.core.NNIProtocol;
import com.nhn.nni.network.NNIDataManager;
import com.nhn.nni.network.NNIDataManager.SubscribeInfo;
import com.nhn.nni.network.NNIServiceHandler;

/**
 * Subscribe Request 패킷을 정의한다.
 *  Client --> Server
 * {"transid":242,"command":3587,"parameter":[1,2,200,"nss2-l:2:200:1347289405478:837",[["nni.naverline.ab7cfbeb-4283-4e3d-bb47-83613250dbbe","naverline",0]]]}
 * @author 유병우(urunimi@nhn.com)
 */
public class SubscribeRequest extends BaseRequest {

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

		JSONArray serviceBlockArray = new JSONArray();
		int serviceCount = 0;

		for (SubscribeInfo subscribeInfo : NNIDataManager.getInstance().getSubscribeMap().values()) {
			if (subscribeInfo.getType() == SubscribeInfo.SUBSCRIBE_TYPE && subscribeInfo.isSuccess() == false) {
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
		
		//시간 정보 전송
		parameterArray.put(System.currentTimeMillis());
		
		String packageName = NNIServiceHandler.getContext().getPackageName();
		//충화전신의 경우 기기정보 업로드를 하면 망에서 끊어버리는 경우가 있다. -> Subscribe 로 옮겼더니 해결
		parameterArray.put(Build.MODEL+";"+Build.MANUFACTURER + ";" + Build.DISPLAY + ";" + Build.VERSION.SDK_INT + ";" + packageName);
		return parameterArray;
	}
}
