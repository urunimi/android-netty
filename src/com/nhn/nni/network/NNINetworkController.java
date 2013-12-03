package com.nhn.nni.network;

import java.util.Random;
import java.util.concurrent.Executors;

import javax.crypto.spec.SecretKeySpec;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.nhn.nni.core.Logger;
import com.nhn.nni.core.NNIConstant;
import com.nhn.nni.core.NNIIntent;
import com.nhn.nni.pipeline.NSSAESDecoder;
import com.nhn.nni.pipeline.NSSAESEncoder;
import com.nhn.nni.protocol.HeartbeatRequest;

/**
 * 네트워크 관련 Life cycle을 관리한다.
 * <br>
 * <br>{@link #connectSessionIfItNeeds()} 를 통해 접속이 되어 있지 않을 경우 접속 요청을 한다.
 * <br>위의 함수를 참조하면 알겠지만 크게 Channel 에 write, read 하는 pipeline 으로 {@link NSSAESDecoder} 와 {@link NSSAESEncoder}를 사용하고,
 * <br>네트워크 에서 발생하는 Event 는 {@link NNINetworkEventHandler} 쪽으로 유입되므로 참조할 것.
 * <br>
 * <br>아래의 Jar에서 호출하는 몇가지 함수들은 절대로 건드리면 안된다.
 * <br>{@link #SERVICE_procOnCreate()}
 * <br>{@link #SERVICE_onDestroy()}
 * <br>{@link #SERVICE_informStatusToSubscribers(int)}
 * <br>{@link #SERVICE_handleRequestIntent(Intent)}

 * 
 * @author 유병우(urunimi@nhn.com)
 *
 */
public class NNINetworkController {

	/** request후 response가 30초 이내에 응답이 와야 함 */
	static final long INTERVAL_WAIT_FOR_RESPONSE = 30 * 1000;
	
	static final long INTERVAL_RECONNECT_MINIMUM = 10 * 1000;
	
	static long INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF = INTERVAL_RECONNECT_MINIMUM;
	/** 이 값에 도달하면 서비스를 재시작 해본다.*/
	static final long INTERVAL_RECONNECT_MAXIMUM = 30 * 60 * 1000;
	
	static NNINetworkController instance;
	
	Channel mChannel;

	/**
	 * <b>DO NOT CHANGE THE METHOD NAME AND PARAMETERS</b> <br/>
	 * Because this method is called from other classes by invoking.
	 * {@link java.lang.reflect.Method#invoke(Object, Object...)}
	 */
	public static NNINetworkController getInstance() {
		if (instance == null) {
			instance = new NNINetworkController();
		}
		return instance;
	}

	private NNINetworkController() {}
	
	/** NNI 서버에 접속 하는 함수. 이미 접속 되어 있을 경우 새로 연결을 맺지는 않는다. 
	 * <br> {@link #checkNniConnection(boolean)}을 통해 연결이 되어 있는지 검사.
	 * <br> {@link #INTERVAL_WAIT_FOR_RESPONSE} 만큼 연결 결과를 기다림.*/
	void connectSessionIfItNeeds() {
		if(checkNniConnection(false) == false) {
			NNIServiceHandler.startSchedule(NNIIntent.ACTION_CHECK_SESSION, INTERVAL_WAIT_FOR_RESPONSE);
			NNIServiceHandler.stopSchedule(NNIIntent.ACTION_HEARTBEAT);
			
			NioClientSocketChannelFactory factory = new NioClientSocketChannelFactory( 
					Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

			ClientBootstrap bootstrap = new ClientBootstrap(factory);
			
			// Set up the pipeline factory.
			bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
				public ChannelPipeline getPipeline() throws Exception {				
					return Channels.pipeline(
							new NSSAESDecoder("AES/ECB/NoPadding", new SecretKeySpec("sejsgyshgtmjyksy".getBytes(), "AES")), 
							new NNINetworkEventHandler()
							,new NSSAESEncoder("AES/ECB/NoPadding", new SecretKeySpec("sejsgyshgtmjyksy".getBytes(), "AES"))
					);
				}
			});

			// Bind and start to accept incoming connections.
			ChannelFuture future = bootstrap.connect(NNIConstant.getServerHost());
			future.awaitUninterruptibly();
			mChannel = future.getChannel();
		}
	}
	
	/** 연결을 다시 맺어야 할 경우 connection 을 닫는다.
	 */
	void disconnectSessionIfItNeeds() {
		if(checkNniConnection(false) == true) {
			ChannelFuture future = mChannel.disconnect();
			future.awaitUninterruptibly();
		}
	}
	
	/** Session 의 연결상태를 확인한다. 필요할 경우 HeartBeat을 전송.*/
	boolean checkNniConnection(boolean isSendingHeartbeat) {
		boolean result = false;
		if(mChannel != null && mChannel.isConnected() == true) {
			if(isSendingHeartbeat) {
				NNIServiceHandler.startSchedule(NNIIntent.ACTION_CHECK_SESSION, INTERVAL_WAIT_FOR_RESPONSE);
				mChannel.write(new HeartbeatRequest());
			}
			result = true;
		}
		Logger.v(NNINetworkController.class.getSimpleName() + ".checkNniConnection(), mChannel: " + mChannel + ", result: " + result);
		return result;
	}
	
	/** 재접속 요청을 스케쥴링한다. 무한 재접속으로 빠지지 않도록 Exponential Backoff를 구현 
	 * 여기서 {@link NNIServiceHandler#isConnectAlreadyScheduled} 를 검사해서 이미 scheduled 되어 있으면 그냥 리턴*/
	void scheduleToReconnect() {
		if(NNIServiceHandler.isConnectAlreadyScheduled == true) {
			return;
		}

        //Random Integer 를 더해서 서버에 접속 부하를 줄인다.
		if(INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF < INTERVAL_RECONNECT_MAXIMUM) {
            INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF += new Random().nextInt(1000);
			Logger.i(String.format("%s.scheduleToReconnect() delay: %dsec", NNINetworkController.class.getSimpleName(), INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF/1000));

			NNIServiceHandler.startSchedule(NNIIntent.ACTION_CONNECT_SESSION, INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF);
			INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF *=2;
			NNIServiceHandler.isConnectAlreadyScheduled = true;
		} else {
			NNIServiceHandler.scheduleToRestartService();
		}
	}
	
	/**
	 * <b>DO NOT CHANGE THE METHOD NAME AND PARAMETERS</b> <br/>
	 * Because this method is called from other classes by invoking.
	 * {@link java.lang.reflect.Method#invoke(Object, Object...)}
	 */
	public void SERVICE_informStatusToSubscribers(int status) {
		NNIServiceHandler.informStatusToSubscribers(status);
	}
	
	
	/**
	 * <b>DO NOT CHANGE THE METHOD NAME AND PARAMETERS</b> <br/>
	 * Because this method is called from other classes by invoking.
	 * {@link java.lang.reflect.Method#invoke(Object, Object...)}
	 */
	public void setServiceContext(Context context) {
		NNIServiceHandler.setServiceContext((Service) context);
	}

	/**
	 * <b>DO NOT CHANGE THE METHOD NAME AND PARAMETERS</b> <br/>
	 * Because this method is called from other classes by invoking.
	 * {@link java.lang.reflect.Method#invoke(Object, Object...)}
	 */
	public void SERVICE_procOnCreate() {
		NNIServiceHandler.onCreate();
	}

	/**
	 * <b>DO NOT CHANGE THE METHOD NAME AND PARAMETERS</b> <br/>
	 * Because this method is called from other classes by invoking.
	 * {@link java.lang.reflect.Method#invoke(Object, Object...)}
	 */
	public void SERVICE_onDestroy() {
		NNIServiceHandler.onDestroy();
	}

	/**
	 * <b>DO NOT CHANGE THE METHOD NAME AND PARAMETERS</b> <br/>
	 * Because this method is called from other classes by invoking.
	 * {@link java.lang.reflect.Method#invoke(Object, Object...)}
	 */
	public void SERVICE_handleRequestIntent(Intent intent) {
		NNIServiceHandler.onStartCommand(intent);
	}
}
