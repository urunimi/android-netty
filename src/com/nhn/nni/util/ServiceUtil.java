package com.nhn.nni.util;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.nhn.nni.core.Logger;

/**
 * Service의 life cycle 관리를 지원한다.
 * @author 유병우(urunimi@nhn.com)
 *
 */
public final class ServiceUtil {
	
	/**
	 * Intent extra의 이름. 알람 스케쥴을 시작한 시간 정보. long Extra
	 */
	public static final String SCHEDULE_TIME = "scheduleTime";

	/**
	 * 일정 기간(delay) 뒤에 서비스가 할 일(action)을 예약.
	 * 대상 서비스는 Intent action 을 받을 것이다.
	 * 이전에 동일한 action 예약이 있었다면 그것은 취소된다.
	 * @param service 대상 서비스
	 * @param action 서비스가 받을 intent action
	 * @param delay 예약 기간.
	 */
	public static void startSchedule(Service service, String action, long delay) {
		stopSchedulePrivate(service, action);
		
		long now = System.currentTimeMillis();
		
		Logger.d("ServiceUtil: Scheduling Action \"" + action + "\" with delay " + (delay/1000) + "sec");
		Intent i = new Intent();
		i.setClass(service, service.getClass());
		i.setAction(action);
		i.putExtra(SCHEDULE_TIME, android.os.SystemClock.elapsedRealtime());
		PendingIntent pi = PendingIntent.getService(service, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager)service.getSystemService(Context.ALARM_SERVICE);
		alarmMgr.set(AlarmManager.RTC_WAKEUP, now + delay, pi);
	}
	
	/**
	 * 일정 기간(interval) 뒤부터 일정 간격(interval)으로 서비스가 할 일(action)을 지정.
	 * 대상 서비스는 Intent action 을 받을 것이다.
	 * 이전에 동일한 action 예약이 있었다면 그것은 취소된다.
	 * @param service 대상 서비스
	 * @param action 서비스가 받을 intent action
	 * @param interval 주기. 호출 시점부터 이 주기 후에 첫 action 이 발생한다.
	 */
	public static void repeatSchedule(Service service, String action, long interval) {
		repeatSchedule(service, action, AlarmManager.RTC_WAKEUP, interval);
	}
	
	/**
	 * 일정 기간(interval) 뒤부터 일정 간격(interval)으로 서비스가 할 일(action)을 지정.
	 * 대상 서비스는 Intent action 을 받을 것이다.
	 * 이전에 동일한 action 예약이 있었다면 그것은 취소된다.
	 * @param service 대상 서비스
	 * @param action 서비스가 받을 intent action
	 * @param type	{@link AlarmManager#RTC_WAKEUP} or {@link AlarmManager#RTC}
	 * @param interval 주기. 호출 시점부터 이 주기 후에 첫 action 이 발생한다.
	 */
	public static void repeatSchedule(Service service, String action, int type, long interval) {
		stopSchedulePrivate(service, action);
		
		Logger.d("ServiceUtil: Starting Action \"" + action + "\", with interval " + (interval/1000) + "sec");
		long now = System.currentTimeMillis();
		
		Intent i = new Intent();
		i.setClass(service, service.getClass());
		i.setAction(action);
		i.putExtra(SCHEDULE_TIME, android.os.SystemClock.elapsedRealtime());
		PendingIntent pi = PendingIntent.getService(service, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager)service.getSystemService(Context.ALARM_SERVICE);
		alarmMgr.setRepeating(type,  now + interval, interval, pi);
	}
	
	/**
	 * 서비스가 하려고 예약한 일(action)을 취소.
	 * @param service 대상 서비스
	 * @param action 취소할 intent action
	 * @see #startSchedule(Service, String, long)
	 * @see #repeatSchedule(Service, String, long)
	 */
	public static void stopSchedule(Service service, String action) {
		Logger.d("ServiceUtil: Stopping Action \"" + action + "\"");
		stopSchedulePrivate(service, action);
	}
	
	static void stopSchedulePrivate(Service service, String action) {
		Intent i = new Intent();
		i.setClass(service, service.getClass());
		i.setAction(action);
		PendingIntent pi = PendingIntent.getService(service, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager)service.getSystemService(Context.ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}
	
	/**
	 * 서비스 객체가 살아 있는지 검사하기 위한 함수
	 * @param serviceClass 대상 서비스
	 * @return 대상 서비스의 실행 유무
	 * @throws SecurityException From {@link ActivityManager#getRunningServices(int)}
	 */
	public static boolean isRunning(Context context, Class<? extends Service> serviceClass)
	throws SecurityException {
		/*
		 * 인자가 null 이면 NullPointerException 이 나올 것이다.
		 * 그 처리는 호출자의 몫이고 알파테스트 이내에 포착할 문제다.
		 * 라이브러리 메소드가 그것에 대해 안전 리턴을 보장할 필요는 없음.
		 * */
		String serviceName = serviceClass.getName();	// 대상 이름 특정.
		if(serviceName != null) {
			ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			List<RunningServiceInfo> infos = manager.getRunningServices(Integer.MAX_VALUE);	// 리스트 얻기
			if(infos != null) {
				for (RunningServiceInfo info : infos) {	// 순회하면서 찾는다.
					if(info == null || info.service == null) continue;
					if (serviceName.equals(info.service.getClassName())) {	// 찾았으면 끝
						return true;
					}
				}
			}
		}
		
		return false;	// 못 찾은 채로 순회 종료
	}
}
