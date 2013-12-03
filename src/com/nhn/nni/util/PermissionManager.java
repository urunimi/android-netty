package com.nhn.nni.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.TelephonyManager;

import com.nhn.nni.core.Logger;

/**
 * WakeLock과 같은 Permission이 필요한 API들을 관리한다.
 * @author 유병우(urunimi@nhn.com)
 */
public class PermissionManager {
	protected PermissionManager() {}
	
	
	public static final boolean checkAndUpdateIntent(Service context, Intent intent) {
		Logger.v(PermissionManager.class.getSimpleName() + ".checkAndUpdateIntent() Action: " + intent.getAction());
		int result = PackageManager.PERMISSION_DENIED;
		List<ResolveInfo> list = queryAllReceivers(context, intent);

		if(list == null) {
			Logger.w(PermissionManager.class.getSimpleName() + ".checkAndUpdateIntent() : There is no receiver to get this Action!!");
		} else {
			for (ResolveInfo info : list) {
				if (info.activityInfo != null) {

					result = context.getPackageManager().checkPermission(intent.getPackage() + ".permission.NNI_MESSAGE",
							info.activityInfo.packageName);
					
					if(result == PackageManager.PERMISSION_GRANTED) {
						intent.addCategory(intent.getPackage());
						Logger.v(info.activityInfo.packageName + ", " + info.activityInfo + ", result: " + result + " (PERMISSION_GRANTED=0, PERMISSION_DENIED=-1)");
						break;
					} else {
						Logger.w(info.activityInfo.packageName + ", " + info.activityInfo + ", result: " + result + " (PERMISSION_GRANTED=0, PERMISSION_DENIED=-1)");
						continue;
					}
				}
			}
		}
		
		return (result == PackageManager.PERMISSION_GRANTED) ? true : false;
	}
	
	/**
	 * 인자의 인텐트를 수신할 수 있는 모든 컴포넌트들을 검색한다.
	 * 
	 * @param context
	 * @param intent
	 * @return 인자의 인텐트를 수신 가능한 요소들의 목록
	 */
	private static final List<ResolveInfo> queryAllReceivers(Context context, Intent intent) {
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryBroadcastReceivers(intent, PackageManager.GET_INTENT_FILTERS);
		return list;
	}
	
	/**
	 * {@link android.os.PowerManager.WakeLock}를 감싸고 있는 Class
	 * @author 유병우(urunimi@nhn.com)
	 */
	public static class WakeLockWrapper {

		private static boolean isPermissionChecked = false;
		private static boolean isPermissionGranted = false;

		volatile private WakeLock wakeLock = null;
		
		private static volatile Map<String, WakeLockWrapper> wakeLockMap = new ConcurrentHashMap<String, WakeLockWrapper>();
		
		/**
		 * @see PermissionManager#getWakeLockInstance(Context, String)
		 * @return 만들어진 Wrapper
		 */
		synchronized public static WakeLockWrapper getWakeLockInstance(Context context, String tag) {
			return getWakeLockInstance(context, WakeLockWrapper.PARTIAL_WAKE_LOCK, tag);
		}

		/**
		 * @see PermissionManager#getWakeLockInstance(Context, String)
		 * @return 만들어진 Wrapper
		 */
		synchronized public static WakeLockWrapper getInstance(Context context) {
			return getWakeLockInstance(context, WakeLockWrapper.PARTIAL_WAKE_LOCK, context.getApplicationInfo().packageName);
		}

		/**
		 * @see PermissionManager#getWakeLockInstance(Context, int, String)
		 * @return 만들어진 Wrapper
		 */
		synchronized public static WakeLockWrapper getWakeLockInstance(Context context, int flags) {
			return getWakeLockInstance(context, flags, context.getApplicationInfo().packageName);
		}

		/**
		 * {@link WakeLockWrapper}를 리턴.<br/>
		 * 권한이 있는지, 객체의 존재유무를 미리 검사를 하므로 좀 더 자유롭게 사용할 수 있다.
		 * @param context
		 * @param tag 새로 만들어야 할 경우, WakeLock 에 사용할 태그.
		 * @param flags 새로 만들어야 할 경우, WakeLock 에 사용할 flags. {@link android.os.PowerManager}
		 * @return 만들어진 Wrapper
		 */
		synchronized public static WakeLockWrapper getWakeLockInstance(Context context, int flags, String tag) {
			WakeLockWrapper found = wakeLockMap.get(tag);
			if (found == null) {
				found = new WakeLockWrapper(context, tag, flags);
				wakeLockMap.put(tag, found);
			}
			return found;
		}

		/**
		 * 이 매니저가 관리하고 있던 모든 {@link WakeLockWrapper}에 대해 release()를 시도한다.
		 * @return release 가 이루어진 횟수.
		 */
		synchronized public static int releaseAllWakeLocks() {
			int reVal = 0;
			for (WakeLockWrapper locker : wakeLockMap.values()) {
				if (locker.release()) {
					++reVal;
				}
			}
			return reVal;
		}

		/**
		 * 이 매니저가 관리하고 있던 모든 {@link WakeLockWrapper} 객체를 제거한다.
		 * 필요하다면 release 도 같이 이루어짐.
		 * @return 제거한 객체의 개수.
		 */
		synchronized public static int freeAllWakeLocks() {
			int reVal = wakeLockMap.size();
			releaseAllWakeLocks();
			wakeLockMap.clear();
			return reVal;
		}

		/** @see PowerManager#PARTIAL_WAKE_LOCK */
		public static final int PARTIAL_WAKE_LOCK = PowerManager.PARTIAL_WAKE_LOCK;
		/** @see PowerManager#SCREEN_DIM_WAKE_LOCK */
		@SuppressWarnings("deprecation")
		public static final int SCREEN_DIM_WAKE_LOCK = PowerManager.SCREEN_DIM_WAKE_LOCK;
		/** @see PowerManager#FULL_WAKE_LOCK */
		@SuppressWarnings("deprecation")
		public static final int FULL_WAKE_LOCK = PowerManager.FULL_WAKE_LOCK;

		WakeLockWrapper(Context context, String tag, int flags) {
			if (isAvailable(context)) {
				wakeLock = ((PowerManager)context.getSystemService(Context.POWER_SERVICE))
					.newWakeLock(flags, tag);
			}
		}

		/*
		 * 이하 논리에서,
		 * 같은 객체에 대해 요청을 하거나 상태에 맞지 않은 요청이 들어왔을 때,
		 * 요청을 실질적으로 처리하지 않았음에도 불구하고
		 * 아무런 제약 없이 메소드는 정상 종료를 하도록 구성되어 있다.
		 * 
		 * 물론 외부에서 이런 내용을 몰라도 되겠지만,
		 * 필요에 따라 알 수도 있어야 한다.
		 * 
		 * 예를 들어 비동기 루틴에서 각자의 필요에 따라 락을 잡았을 때,
		 * 한 쪽이 먼저 요청하고 먼저 종료해버린다고 하자.
		 * 이 경우 나중에 종료하는 쪽은 후반부에 락 없이 진행해야 한다.
		 * 최소한 각 루틴이 이러한 상황을 알 수는 있어야 한다.
		 * 
		 * 따라서 각 요청에 대해 리턴값을 이용하여 자신의 동작 여부를 보고하게 한다.
		 * */

		/**
		 * @return 이 호출에 의해 aquire 가 수행되었다면 true.
		 * @see WakeLock#acquire()
		 */
		public boolean acquire() {
			if (wakeLock != null && wakeLock.isHeld() == false) {
				wakeLock.acquire();
				return true;
			}
			return false;
		}

		/**
		 * @return 이 호출에 의해 aquire 가 수행되었다면 true.
		 * @see WakeLock#acquire(long)
		 */
		public boolean acquire(long timeout) {
			if (wakeLock != null /*&& wakeLock.isHeld() == false*/) { //이 경우엔 isHeld를 확인하면 무조건 acquire인 상태로 리턴 된다.
				wakeLock.acquire(timeout);
				return true;
			}
			return false;
		}

		/**
		 * @return 이 호출에 의해 release 가 수행되었다면 true.
		 * @see WakeLock#release()
		 */
		public boolean release() {
			if (wakeLock != null && wakeLock.isHeld() == true) {
				wakeLock.release();
				return true;
			}
			return false;
		}

		/**
		 * WakeLock에 관련된 permission이 있는지 체크
		 * @param context
		 * @return 권한이 있으면 true
		 */
		synchronized public static boolean isAvailable(Context context) {
			if (isPermissionChecked == false) {
				isPermissionChecked = true;
				PackageManager packageManager = context.getPackageManager();
				if (PackageManager.PERMISSION_GRANTED
				== packageManager.checkPermission(android.Manifest.permission.WAKE_LOCK, context.getPackageName())) {
					isPermissionGranted = true;
				}
			}
			return isPermissionGranted;
		}
	}

	public static class OperatorWrapper {

		String mName = "This is not an operator name";
		int mMCC = -1;
		int mMNC = -1;
		int mNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
		
		private static volatile OperatorWrapper operator;
		
		synchronized public static OperatorWrapper getInstance(Context context) {

			if (operator == null) {
				operator = new OperatorWrapper(context);
			}

			return operator;
		}

		OperatorWrapper(Context context) {
			if (OperatorWrapper.isAvailable(context) == true) {
				try {
					TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
					
					mName = tm.getNetworkOperatorName();
					String mNumber = tm.getNetworkOperator();
					mNetworkType = tm.getNetworkType();
					mMCC = Integer.parseInt(mNumber.substring(0, 3));
					mMNC = Integer.parseInt(mNumber.substring(3));
					
					Logger.i(getClass().getSimpleName() + ": Operator information is set. Name:" + mName + ", MCC:" + mMCC + ", MNC:" + mMNC + ", NetworkType:" + mNetworkType);
				} catch (Exception e) {
					Logger.e(e);
				}
			}
		}

		public String getName() {
			return mName;
		}
		
		public int getNetworkType() {
			return mNetworkType;
		}
		
		/**
		 * 국가 코드
		 * @return 
		 */
		public int getCountryCode() {
			return mMCC;
		}

		/**
		 * 통신사 코드
		 * @return
		 */
		public int getOperatorCode() {
			return mMNC;
		}

		synchronized private static boolean isAvailable(Context context) {
			PackageManager packageManager = context.getPackageManager();
			if (PackageManager.PERMISSION_GRANTED
				== packageManager.checkPermission(android.Manifest.permission.READ_PHONE_STATE,
				context.getPackageName())) {
				return true;
			}
			return false;
		}
		
		//http://mcclist.com/mobile-network-codes-country-codes.asp
//		String JAPAN_MCC = "440";// jp
//		int[] DOCOMO_MNC = { 1, 2, 3, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 26, 27, 28, 29,
//				30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 58, 60, 61, 62, 63, 65, 66, 67, 68, 69, 87, 99 };
//		String KOREA_MCC = "450";
//		int[] KT_MNC = { 0, 2, 4, 8 };
//		int[] SKT_MNC = { 3, 5 };
//		int[] LGT_MNC = { 6 };
//
//		Object[][] FIX_CLIENTLIST = {
//		// mcc(String), mnc(int[]), pingInterval(int)- seconds
//		{ JAPAN_MCC, DOCOMO_MNC, 14 * 60 },// NTT Docomo의 경우 14분으로 변경함
//		// {KOREA_MCC, KT_MNC, 8 * 60},//TEST용 KT의 경우 6분으로 설정
//		// {KOREA_MCC, SKT_MNC, 10 * 60},//TEST용 SKT의 경우 10분으로 설정
//		// {KOREA_MCC, LGT_MNC, 9 * 60},//TEST용 LGT의 경우 9분으로 설정
//		};
	}
}