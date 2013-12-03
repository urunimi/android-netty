package com.nhn.nni.network;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Process;

import com.nhn.nni.NNIConstant.ClientType;
import com.nhn.nni.NNIMessageService;
import com.nhn.nni.core.Logger;
import com.nhn.nni.core.NNIConstant;
import com.nhn.nni.core.NNIIntent;
import com.nhn.nni.network.NNIDataManager.SubscribeInfo;
import com.nhn.nni.util.PermissionManager;
import com.nhn.nni.util.PermissionManager.WakeLockWrapper;
import com.nhn.nni.util.ServiceUtil;
import com.nhn.nni.util.ThreadManager;
import static com.nhn.nni.core.NNIIntent.*;


/**
 * DEX안에서의 서비스 관련 로직을 수행한다. 내부적으로 Service객체를 가지고 관리한다.
 * 
 * @author 유병우(urunimi@nhn.com)
 * 
 */
public class NNIServiceHandler {
	
	static final long INTERVAL_RESTART_SERVICE = 60 * 1000;

	/** {@link NNIMessageService} 객체 */
	static Service serviceContext;
	static boolean isConnectAlreadyScheduled = false;

	/**
	 * 여기서 서비스 객체를 세팅한다.
	 * 
	 * @param serviceContext
	 */
	static synchronized void setServiceContext(Service serviceContext) {
		NNIServiceHandler.serviceContext = serviceContext;
	}

	/**
	 * {@link Service} 객체가 필요하거나, {@link Context} 가 필요할 땐 무조건 이 함수를 이용하도록 한다.
	 * 
	 * @return {@link #serviceContext}
	 */
	public static synchronized Service getContext() {
		if (serviceContext == null) {
			Logger.w("Service context is null");
			restartService();
			return null;
		} else {
			return serviceContext;
		}
	}

	/**
	 * {@link Service#onCreate()}
	 */
	public static synchronized void onCreate() {
		ThreadManager.offer(new Runnable() {

			@Override
			public void run() {
				WakeLockWrapper wakeLock = WakeLockWrapper.getInstance(getContext());
				wakeLock.acquire();
				try {
					NNINetworkController.getInstance().connectSessionIfItNeeds();
				} finally {
					wakeLock.release();
				}
			}
		});
	}

	/**
	 * {@link Service#onDestroy()}
	 */
	public static synchronized void onDestroy() {
		WakeLockWrapper wakeLock = WakeLockWrapper.getInstance(getContext());
		wakeLock.acquire();
		try {
			ServiceUtil.stopSchedule(serviceContext, ACTION_HEARTBEAT);
			ServiceUtil.stopSchedule(serviceContext, ACTION_CHECK_SESSION);
			ServiceUtil.stopSchedule(serviceContext, ACTION_CONNECT_SESSION);
			ServiceUtil.stopSchedule(serviceContext, ACTION_RESTART_SERVICE);
			serviceContext = null;
		} finally {
			wakeLock.release();
		}
	}

	@SuppressWarnings("deprecation")
	static void handleIntent(Intent intent) {
		/* 서비스 실행 도중에 Client Type을 바꿀수 있도록 한다. */
		ClientType type = ClientType.fromString(intent.getStringExtra(NNIIntent.EXTRA_APPLICATION_CLIENT_TYPE));
		if (type != null) {
			ClientType oldType = NNIConstant.setClientType(type);
			Logger.i(NNIServiceHandler.class.getSimpleName() + ": change Clinet type from " + oldType + ", to " + type);
			return;
		}

		if (NNIIntent.ACTION_REGISTER.equals(intent.getAction())) {
			
			PendingIntent registerIntent = intent.getParcelableExtra(NNIIntent.EXTRA_APPLICATION_PENDING_INTENT);
			String serviceId = intent.getStringExtra(NNIIntent.EXTRA_APPLICATION_SERVICE_ID);
			String packageName = intent.getStringExtra(NNIIntent.EXTRA_APPLICATION_PACKAGE_NAME);

			if (registerIntent != null) {
				boolean needRegistration = !intent.getBooleanExtra(NNIIntent.EXTRA_APPLICATION_KEEPALIVE, false);

				NNIDataManager dataInstance = NNIDataManager.getInstance();

				String targetIdFromIntent = intent.getStringExtra(NNIIntent.EXTRA_TARGET_ID);

				// Target ID 갱신 과정..
				if (targetIdFromIntent != null && targetIdFromIntent.startsWith("nni.")) {
					Logger.d(NNIServiceHandler.class.getSimpleName() + ": Target ID is Received from a Service Application. The ID is " + targetIdFromIntent);

					for (int i = 0; i < 2; i++) {
						targetIdFromIntent = targetIdFromIntent.substring(targetIdFromIntent.indexOf('.') + 1);
					}

					String targetIdRefreshed = dataInstance.refreshTargetId(getContext(), targetIdFromIntent);

					// 앱이 가지고 있는 ID와 기존의 ID가 다르므로 앱에게 구독정보 갱신하도록 해야함!
					if (targetIdFromIntent.equals(targetIdRefreshed) == false) {
						Logger.i(NNIServiceHandler.class.getSimpleName() + ": Target ID is different! Registration process will proceed.");
						needRegistration = true;
					}
				}
				
				int beforeSize = dataInstance.getSubscribeMap().size();

				SubscribeInfo subscribeInfo = dataInstance.addSubscribeInfo(serviceId, new SubscribeInfo(serviceId, packageName), true);
				
				Logger.v(NNIServiceHandler.class.getSimpleName() + ". Registration process...serviceId: " + serviceId + ", needRegistration: " + needRegistration);

				if (needRegistration == true) {

					if (subscribeInfo == null) {
						subscribeInfo = NNIDataManager.getInstance().getSubscribeInfo(serviceId);
					}

					if (subscribeInfo != null) {
						Intent resultIntent = new Intent(NNIIntent.ACTION_FROM_NNI_REGISTRATION);
						resultIntent.setPackage(subscribeInfo.getPackageName());
						if (PermissionManager.checkAndUpdateIntent(getContext(), resultIntent)) {
							resultIntent.putExtra(NNIIntent.EXTRA_TARGET_ID, subscribeInfo.getTargetId());
							Logger.i(getCurrentBatteryLevel() + "Send Subscribe Info TargetId : " + subscribeInfo.getTargetId() + " To "
									+ resultIntent.toString());
							getContext().sendBroadcast(resultIntent);
						}
					}
				}

				int afterSize = dataInstance.getSubscribeMap().size();
				
				Logger.v(NNIServiceHandler.class.getSimpleName() + ". Need re-subscribe? beforeSize: " + beforeSize + ", afterSize: " + afterSize);
				
				if(beforeSize != afterSize) {
					NNINetworkController.getInstance().disconnectSessionIfItNeeds();
				}

				NNINetworkController.getInstance().connectSessionIfItNeeds();
			}
			
		} else if (NNIIntent.ACTION_UNREGISTER.equals(intent.getAction())) {
			PendingIntent unregisterIntent = intent.getParcelableExtra(NNIIntent.EXTRA_APPLICATION_PENDING_INTENT);
			String serviceId = intent.getStringExtra(NNIIntent.EXTRA_APPLICATION_SERVICE_ID);
			String packageName = intent.getStringExtra(NNIIntent.EXTRA_APPLICATION_PACKAGE_NAME);
			if (packageName == null) {
				packageName = unregisterIntent.getTargetPackage();
			}

			if (unregisterIntent != null) {
				SubscribeInfo subscribeInfo = NNIDataManager.getInstance().getSubscribeInfo(serviceId);
				if (subscribeInfo != null) {
					Logger.v(NNIServiceHandler.class.getSimpleName() + "Unregistration process...serviceId: " + serviceId);
					if (subscribeInfo.isSuccess() && subscribeInfo.getType() == SubscribeInfo.SUBSCRIBE_TYPE) {
						subscribeInfo.setType(SubscribeInfo.UNSUBSCRIBE_TYPE);

						// if (getCurrentNetworkStatus() ==
						// NNIConstant.NETWORK_STATUS_CONNECTED_NPUSH) {
						// unsubscribeAll();
						// }
					} else {
						NNIDataManager.getInstance().removeSubscribeInfo(serviceId);
					}
					Intent resultIntent = new Intent().setAction(NNIIntent.ACTION_FROM_NNI_REGISTRATION);
					resultIntent.setPackage(subscribeInfo.getPackageName());
					if (PermissionManager.checkAndUpdateIntent(getContext(), resultIntent)) {
						resultIntent.putExtra(NNIIntent.EXTRA_IS_UNREGISTERED, true);
						resultIntent.putExtra(NNIIntent.EXTRA_TARGET_ID, subscribeInfo.getTargetId());
						Logger.i(getCurrentBatteryLevel() + "Send Unsubscribe Info TargetId : " + subscribeInfo.getTargetId() + " To "
								+ resultIntent.toString());
						getContext().sendBroadcast(resultIntent);
					}
				}
			}
		} else if (NNIIntent.ACTION_GET_STATE.equals(intent.getAction())) {
			PendingIntent registerIntent = intent.getParcelableExtra(NNIIntent.EXTRA_APPLICATION_PENDING_INTENT);
			String packageName = intent.getStringExtra(NNIIntent.EXTRA_APPLICATION_PACKAGE_NAME);
			if (packageName == null) {
				packageName = registerIntent.getTargetPackage();
			}

			if (registerIntent != null) {
				Intent resultIntent = new Intent().setAction(NNIIntent.ACTION_FROM_NNI_EVENT).addCategory(packageName);
				String currentState = "NNI ";
				
				if(NNINetworkController.getInstance().checkNniConnection(false)) {
					currentState += "NETWORK_STATUS_CONNECTED_NPUSH";
				} else {
					currentState += "NETWORK_STATUS_DISCONNECTED";
				}
				
				resultIntent.putExtra(NNIIntent.EXTRA_EVENT_ID, currentState);
				getContext().sendBroadcast(resultIntent);
			}
		} else if(NNIIntent.ACTION_HEARTBEAT.equals(intent.getAction())) {
			if(NNINetworkController.getInstance().checkNniConnection(true) == false) {
				NNINetworkController.getInstance().connectSessionIfItNeeds();
			}
		} else if(NNIIntent.ACTION_CHECK_SESSION.equals(intent.getAction())) {
			NNINetworkController.getInstance().scheduleToReconnect();
		} else if(NNIIntent.ACTION_RESTART_SERVICE.equals(intent.getAction())) {
			restartService();
		} else if(NNIIntent.ACTION_CONNECT_SESSION.equals(intent.getAction())) {
			isConnectAlreadyScheduled = false;
			NNINetworkController.getInstance().connectSessionIfItNeeds();
		}
	}

	/**
	 * {@link Service#onStartCommand(Intent, int, int)}
	 */
	public static synchronized void onStartCommand(final Intent intent) {
		ThreadManager.offer(new Runnable() {

			@Override
			public void run() {
				WakeLockWrapper wakeLock = WakeLockWrapper.getInstance(getContext());
				wakeLock.acquire();
				try {
					handleIntent(intent);
				} finally {
					wakeLock.release();
				}
			}
		});
	}

	public static void informStatusToSubscribers(int status) {
		WakeLockWrapper wakeLock = WakeLockWrapper.getInstance(getContext());
		wakeLock.acquire();
		try {
			Logger.d("informStatusToSubscribers : status=" + status);

			for (SubscribeInfo subscribeInfo : NNIDataManager.getInstance().getSubscribeMap().values()) {
				if (subscribeInfo.getType() == SubscribeInfo.SUBSCRIBE_TYPE) {
					Intent informIntent = new Intent(NNIIntent.ACTION_FROM_NNI_EVENT);
					informIntent.addCategory(subscribeInfo.getPackageName());
					informIntent.putExtra(NNIIntent.EXTRA_EVENT_ID, "NNI Information");
					informIntent.putExtra(NNIIntent.EXTRA_EVENT_STATE, status);
					serviceContext.sendBroadcast(informIntent);

					Logger.v("informStatusToSubscribers : broadcast status=" + status + " to " + subscribeInfo.getPackageName());
				}
			}
		} finally {
			wakeLock.release();
		}
	}
	
	public static void sendEventToSubscribers(String eventId) {
		Context serviceContext = NNIServiceHandler.getContext();
		if (serviceContext == null) {
			return;
		}
		Intent resultIntent = new Intent().setAction(NNIIntent.ACTION_FROM_NNI_EVENT);
		resultIntent.putExtra(NNIIntent.EXTRA_EVENT_ID, eventId);
		serviceContext.sendBroadcast(resultIntent);
	}

	public static String getCurrentBatteryLevel() {
		Intent bat = serviceContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		String battLevel = "[batt:" + bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) + "%] ";
		return battLevel;
	}
	
	public static void stopSchedule(String action) {
		ServiceUtil.stopSchedule(getContext(), action);
	}
	
	public static void startSchedule(String action, long delay) {
		ServiceUtil.startSchedule(getContext(), action, delay);
	}
	
	public static void repeatSchedule(String action, long interval) {
		ServiceUtil.repeatSchedule(getContext(), action, interval);
	}
	
	public static void scheduleToRestartService() {
		startSchedule(NNIIntent.ACTION_RESTART_SERVICE, INTERVAL_RESTART_SERVICE);
	}
	
	/**
	 * 안전하게 서비스를 재시작하기 위해 Service Context를 검사한 다음 stop 해준다.
	 */
	private static void restartService() {
		Logger.w("Service Process is going to DIE !!!");
		if (serviceContext != null) {
			serviceContext.stopSelf();
		} else {
			Process.killProcess(Process.myPid());
		}
	}
}
