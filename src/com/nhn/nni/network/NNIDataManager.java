package com.nhn.nni.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import com.nhn.nni.core.Logger;
import com.nhn.nni.core.NNIIntent;
import com.nhn.nni.util.PermissionManager.OperatorWrapper;

public class NNIDataManager {
	protected static final String PREF_NPUSH_KEY = "npush_config_pref";
	protected static final String PREF_TARGET_ID = "targetId";
	protected static final String PREF_MAXSTANDBYTIME_ID = "maxStandbyTime";
	protected static final String PREF_SUBSCRIBEINFO_JSON = "subscribeJson";

	protected static final int DEFAULT_STANDBYTIME = 30 * 60;

	protected static NNIDataManager instance;

	protected String mConnectionId;
	protected String mTargetId;
	protected final Map<String, SubscribeInfo> mSubscribeMap;
	// 20110317_chyjae
	protected int mPingInterval;
	protected int mMaxStandbyConnTime;
	protected Context mContext;

	public static NNIDataManager getInstance() {
		if (instance == null) {
			instance = new NNIDataManager();
		}
		return instance;
	}

	private NNIDataManager() {
		mSubscribeMap = new ConcurrentHashMap<String, SubscribeInfo>();

		mContext = NNIServiceHandler.getContext();

		if (mTargetId == null) { // 파일에서 로드해보기
			SharedPreferences pref = mContext.getSharedPreferences(PREF_NPUSH_KEY, Context.MODE_PRIVATE);
			mTargetId = pref.getString(PREF_TARGET_ID, null);
		}

		SharedPreferences pref = mContext.getSharedPreferences(PREF_NPUSH_KEY, Context.MODE_PRIVATE);
		loadSubscribeInfo(mContext, pref);
	}

	public int parseConnectedInfo(JSONArray parameterArray) {
		int errorCode = 0;
		try {
			int parameterIndex = 0;
			@SuppressWarnings("unused")
			int parameterSize = parameterArray.length();
			errorCode = parameterArray.getInt(parameterIndex++);
			String connectionId = parameterArray.getString(parameterIndex++);
			setConnectionId(connectionId);

			/** 6.1.0 version부터 삭제되고 서버에서 내려주는 주기로 client ping */
			int pingInterval = parameterArray.getInt(parameterIndex++);
			setPingInterval(pingInterval);

			int maxStandbyConnTime = parameterArray.getInt(parameterIndex++);
			setMaxStandbyConnTime(maxStandbyConnTime);

			Context context = NNIServiceHandler.getContext();
			saveMaxStandbyTime(context);

			Logger.d(String.format("NPushConnectedData.parseConnectedInfo err(%d) connectionId(%s) pingInterval(%d) standbyConnTime(%d)", errorCode,
					connectionId, pingInterval, maxStandbyConnTime));
		} catch (Exception e) {
			Logger.e(e);
		}
		return errorCode;

	}

	public String getConnectionId() {
		return mConnectionId;
	}

	public void setConnectionId(String connectionId) {
		this.mConnectionId = connectionId;
	}

	public String getTargetId() {
		if (mTargetId == null) { // 없을 경우 새로 생성에서 세팅
			mTargetId = generateDeviceId();
			if (mContext != null) {
				setTargetId(mContext, mTargetId);
			}
		}
		return mTargetId;
	}

	public void setTargetId(Context context, String targetId) {
		this.mTargetId = targetId;
		SharedPreferences pref = context.getSharedPreferences(PREF_NPUSH_KEY, Context.MODE_PRIVATE);
		pref.edit().putString(PREF_TARGET_ID, targetId).commit();
	}

	/**
	 * 이 함수 이전에 먼저 {@link #NPushConnectedData()}가 호출 되서 SharedPreferences 안의
	 * targetID정보는 읽어봤어야 한다.
	 * 
	 * @param context
	 * @param targetIdFromIntentTruncated
	 *            받은 TargetID는 DeviceID가 아닌 앱 정보를 담고 있으므로 이를 제거 한 후 넘겨주어야 한다.
	 * @return
	 */
	public String refreshTargetId(Context context, String targetIdFromIntentTruncated) {
		if (mTargetId == null) { // 없을 경우 받은 TargetID를 세팅
			Logger.i(getClass().getSimpleName() + ": Target ID is null! Received Target ID will be the one. New ID is " + targetIdFromIntentTruncated);
			setTargetId(context, targetIdFromIntentTruncated);
		}
		return mTargetId;
	}

	private void saveMaxStandbyTime(Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_NPUSH_KEY, Context.MODE_PRIVATE);
		if (getMaxStandbyConnTime() == pref.getInt(PREF_MAXSTANDBYTIME_ID, 0)) {
			return;
		}
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt(PREF_MAXSTANDBYTIME_ID, getMaxStandbyConnTime());
		editor.commit();
		Logger.d("Save PREF_MAXSTANDBYTIME_ID");
	}

	private void syncSubscribeInfo() {
		Context context = NNIServiceHandler.getContext();
		if (context == null) {
			return;
		}
		try {
			SharedPreferences pref = context.getSharedPreferences(PREF_NPUSH_KEY, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = pref.edit();

			if (mSubscribeMap.size() == 0) {
				editor.remove(PREF_SUBSCRIBEINFO_JSON);
				editor.commit();
				return;
			}

			JSONObject jsonObj = new JSONObject();
			jsonObj.put("subscribeArray", new JSONArray());
			JSONArray subscribeArray = jsonObj.getJSONArray("subscribeArray");

			for (SubscribeInfo info : mSubscribeMap.values()) {
				JSONArray subscribeInfo = new JSONArray();
				subscribeInfo.put(info.getServiceId());
				subscribeInfo.put(info.getPackageName());
				subscribeArray.put(subscribeInfo);
			}

			Logger.d("syncSubscribeInfo : " + jsonObj.toString());
			editor.putString(PREF_SUBSCRIBEINFO_JSON, jsonObj.toString());
			editor.commit();
		} catch (Exception e) {

		}
	}

	private void loadSubscribeInfo(Context context, SharedPreferences pref) {
		String subscribeJson = pref.getString(PREF_SUBSCRIBEINFO_JSON, null);
		int maxStandbyTime = pref.getInt(PREF_MAXSTANDBYTIME_ID, 0);
		this.setMaxStandbyConnTime(maxStandbyTime);

		if (subscribeJson == null) {
			return;
		}
		try {
			JSONObject jsonObj = new JSONObject(subscribeJson);
			JSONArray subscribeArray = jsonObj.getJSONArray("subscribeArray");
			int subscribeCount = subscribeArray.length();
			for (int i = 0; i < subscribeCount; i++) {
				JSONArray subscribeInfo = subscribeArray.getJSONArray(i);
				String serviceId = subscribeInfo.getString(0);
				String packageName = subscribeInfo.getString(1);

				addSubscribeInfo(serviceId, new SubscribeInfo(serviceId, packageName), false);
			}

		} catch (Exception e) {
			Logger.e(e);
		}
	}

	// 20110317_chyjae[[
	public int getPingInterval() {
		return mPingInterval;
	}

	public void setPingInterval(int pingInterval) {
		this.mPingInterval = pingInterval;
	}

	// ]]20110317_chyjae
	public int getMaxStandbyConnTime() {
		if (mMaxStandbyConnTime == 0) {
			return DEFAULT_STANDBYTIME;
		}
		return mMaxStandbyConnTime;
	}

	public void setMaxStandbyConnTime(int maxStandbyConnTime) {
		this.mMaxStandbyConnTime = maxStandbyConnTime;
	}

	public SubscribeInfo addSubscribeInfo(String key, SubscribeInfo info, boolean saveSharedPref) {
		if (key != null) {
			// nniclient_self가 들어간 경우에 이를 무시하기 위하여 추가함
			if (key.contains(NNIIntent.Self)) {
				return null;
			}
			// 알파벳과 숫자가 그리고 "_" 아닌 경우 이를 무시함
			if (Pattern.matches("^[a-zA-Z0-9_.]*$", key) == false) {
				return null;
			}
		}

		SubscribeInfo subscribeInfo = mSubscribeMap.get(key);
		if (subscribeInfo == null || subscribeInfo.getType() == SubscribeInfo.UNSUBSCRIBE_TYPE
				|| mSubscribeMap.get(key).packageName.equals(info.packageName) == false) {
			mSubscribeMap.put(key, info);
			if (saveSharedPref) {
				syncSubscribeInfo();
			}
			return info;
		}
		return null;
	}

	public SubscribeInfo removeSubscribeInfo(String key) {
		if (mSubscribeMap.containsKey(key) == true) {
			mSubscribeMap.remove(key);
			syncSubscribeInfo();
		}
		return null;
	}

	public SubscribeInfo getSubscribeInfo(String key) {
		return mSubscribeMap.get(key);
	}

	public Map<String, SubscribeInfo> getSubscribeMap() {
		return mSubscribeMap;
	}

	public void initSubscribeMap() {
		if (mSubscribeMap.isEmpty()) {
			return;
		}

		for (SubscribeInfo subscribeInfo : mSubscribeMap.values()) {
			subscribeInfo.setSuccess(false);
			subscribeInfo.setTransId(-1);
		}
	}

	public List<SubscribeInfo> getSubscribeInfoListByTransId(int transId) {
		if (mSubscribeMap.isEmpty()) {
			return null;
		}

		List<SubscribeInfo> subscribeListByTransId = new ArrayList<SubscribeInfo>();

		for (SubscribeInfo subscribeInfo : mSubscribeMap.values()) {
			if (subscribeInfo.getTransId() == transId) {
				subscribeListByTransId.add(subscribeInfo);
			}
		}

		return subscribeListByTransId;
	}

	static public class SubscribeInfo {
		public static final int SUBSCRIBE_TYPE = 0;
		public static final int UNSUBSCRIBE_TYPE = 1;

		String serviceId;
		String packageName;
		boolean isSuccess = false;
		int transId = -1;
		int type = SUBSCRIBE_TYPE;

		public SubscribeInfo(String serviceId, String packageName) {
			super();
			this.serviceId = serviceId;
			this.packageName = packageName;
			isSuccess = false;
			transId = -1;
			type = SUBSCRIBE_TYPE;
		}

		public String getServiceId() {
			return serviceId;
		}

		public String getPackageName() {
			return packageName;
		}

		public boolean isSuccess() {
			return isSuccess;
		}

		public void setSuccess(boolean isSuccess) {
			this.isSuccess = isSuccess;
		}

		public int getTransId() {
			return transId;
		}

		public void setTransId(int transId) {
			this.transId = transId;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
			this.isSuccess = false;
		}

		public String getTargetId() {
			return NNIDataManager.getInstance().getServiceTargetId(getServiceId());
		}

	}

	public String getServiceTargetId(String serviceId) {
		return "nni." + serviceId + "." + getTargetId();
	}

	/**
	 * Device토큰을 발급하는 부분. 만약 제대로 UUID로 토큰이 만들어지지 않을 경우 임의로 생성한다.
	 * 
	 * @return
	 */
	private String generateDeviceId() {
		String uuidString = UUID.randomUUID().toString();

		if (isValidDeviceId(uuidString) == false) {
			Logger.w(getClass().getSimpleName() + ".generateDeviceId(): Created UUID is not Valid!!");

			String almostUniqueId = System.currentTimeMillis() + Build.CPU_ABI + Build.FINGERPRINT;
			String encodedId = Base64.encodeToString(almostUniqueId.getBytes(), Base64.DEFAULT);
			StringBuilder sb = new StringBuilder();
			uuidString = sb.append(encodedId.substring(0, 8)).append('-').append(encodedId.substring(8, 12)).append('-').append(encodedId.substring(12, 16))
					.append('-').append(encodedId.substring(16, 20)).append('-').append(encodedId.substring(20, 32)).toString();
		}

		// 여기서부터는 Valid 한것으로 가정하고 국가코드를 붙인다.
		int countryCode = OperatorWrapper.getInstance(NNIServiceHandler.getContext()).getCountryCode();
		if (countryCode >= 100 && countryCode < 1000) {
			// 이 경우 국가코드는
			// valid하다.(http://en.wikipedia.org/wiki/Mobile_Country_Code_(MCC))
			// 참조
			uuidString = new StringBuilder().append(countryCode).append('.').append(uuidString).toString();
		}

		return uuidString;
	}

	/**
	 * NPush G/W에서 이 구조에 맞지 않는 토큰은 무효화 하므로 꼭 valid한지 검사해야 한다.
	 * 
	 * @param deviceId
	 * @return
	 */
	static boolean isValidDeviceId(final String deviceId) {
		Pattern ptn = Pattern.compile("\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}");
		Matcher mtchr = ptn.matcher(deviceId);
		return mtchr.matches();
	}
}
