package com.nhn.nni.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.nhn.nni.core.Logger;

/**
 * 작업을 threading 및 큐잉 하기 위한 클래스
 * @author 유병우(urunimi@nhn.com)
 *
 */
public class ThreadManager {
	/** 작업 공간 */
	private static ThreadPoolExecutor excutor = null;
	/** 주 작업 대기열 */
	private static BlockingQueue<Runnable> queue = null;
	
	static {
		threadingStart();
	}
	
	/**
	 * 스레드풀의 연산을 시작한다. 동기화에 안전하지 않으므로 너무 자주 호출하지 말 것.
	 */
	synchronized protected static void threadingStart() {
		if (queue == null) {
			queue = new LinkedBlockingQueue<Runnable>(ThreadConfig.THREAD_BUCKET_SIZE);
		}
		if (excutor == null) {
			excutor = new ThreadPoolExecutor(
					ThreadConfig.THREAD_CORE_SIZE, ThreadConfig.THREAD_MAX_SIZE,
					ThreadConfig.THREAD_ALIVE_TIME, TimeUnit.SECONDS,
					queue);
		}
		if (! excutor.prestartCoreThread()) {
			Logger.w("Fail to prestart core thread. It may already Started.");	// 공공연히 알려야 하므로 로그 래핑을 쓰지 않음.
		}
	}
		
	/**
	 * 스레드풀의 연산을 종료한다. 동기화에 안전하지 않으므로 너무 자주 호출하지 말 것.
	 */
	synchronized protected static void threadingEnd() {
		excutor.shutdown();
		excutor = null;
	}
	
	/**
	 * 대기열에 인자의 게스트를 대기시킨다.
	 * 외부에서는 이 메소드 대신 {@link ThreadGuest#execute()}를 사용할 것을 권장함.<br>
	 * @param guest 대기시킬 대상
	 */
	synchronized public static final boolean offer(final Runnable guest) {
		if (guest == null) {
			throw new NullPointerException("guest is null.");
		}
		boolean offerResult = false;
		if (queue.size() < ThreadConfig.THREAD_BUCKET_MAX_SIZE) {
			queue.offer(guest);
			offerResult = true;
		}
		return offerResult;
	}
}

class ThreadConfig {

	/** {@link ThreadHost} 대기열의 초기 크기 */
	public static final int		THREAD_BUCKET_SIZE		= 32;	// 2^5
	/** {@link ThreadHost} 대기열의 최대 크기 */
	public static final int		THREAD_BUCKET_MAX_SIZE	= 1048576;	// 2^20
	/** {@link ThreadHost} 작업소의 크기 */
	public static final int		THREAD_CORE_SIZE		= 1;
	/** {@link ThreadHost} 작업소의 최대 크기 */
	public static final int		THREAD_MAX_SIZE			= 1;
	
	public static final int		THREAD_ALIVE_TIME		= 0;
}
