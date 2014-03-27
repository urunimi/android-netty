package com.hovans.netty.tcpsample.service;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import android.app.Service;
import android.content.Intent;
import android.os.PowerManager;

import com.hovans.android.log.LogByCodeLab;
import com.hovans.android.service.ServiceUtil;
import com.hovans.android.service.WorkerService;
import com.hovans.netty.tcpsample.network.NetworkEventHandler;
import com.hovans.netty.tcpsample.network.ChannelDecoder;
import com.hovans.netty.tcpsample.network.ChannelEncoder;
import com.hovans.netty.tcpsample.util.ThreadManager;
import com.hovans.netty.tcpsample.util.WakeLockWrapper;


/**
 * This class is the main class that controls the network connection.
 * 
 * @author Hovan Yoo
 * 
 */
public class NettyService extends WorkerService {

	static boolean isConnectAlreadyScheduled = false;
	static final String SERVER_URL = "http://localhost";
	static final int SERVER_PORT = 8080;
	Channel mChannel;


	@Override
	public String getWorkerTag() {
		return NettyService.class.getSimpleName();
	}

	/**
	 * {@link Service#onCreate()}
	 */
	public void onCreate() {
		super.onCreate();

		ThreadManager.offer(new Runnable() {

			@Override
			public void run() {
				PowerManager.WakeLock wakeLock = WakeLockWrapper.getWakeLockInstance(NettyService.this, getWorkerTag());
				wakeLock.acquire();
				try {
					NioClientSocketChannelFactory factory = new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

					ClientBootstrap bootstrap = new ClientBootstrap(factory);

					// Set up the pipeline factory.
					bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
						public ChannelPipeline getPipeline() throws Exception {
							return Channels.pipeline(
								new ChannelDecoder(NettyService.this),
								new NetworkEventHandler(NettyService.this)
								, new ChannelEncoder(NettyService.this)
							);
						}
					});

					// Bind and start to accept incoming connections.
					ChannelFuture future = bootstrap.connect(new InetSocketAddress(SERVER_URL, SERVER_PORT));
					future.awaitUninterruptibly();
					mChannel = future.getChannel();
				} finally {
					wakeLock.release();
				}
			}
		});
	}

	@Override
	public void onWorkerRequest(Intent intent, int i) {
		if (NettyIntent.ACTION_CONNECT_SESSION.equals(intent.getAction())) {
			if(mChannel != null) {
				disconnectSessionIfItNeeds();
			}

			connectSessionIfItNeeds();
		} else if(NettyIntent.ACTION_HEARTBEAT.equals(intent.getAction())) {
			if(checkConnection() == false) {
				connectSessionIfItNeeds();
			}
		} else if(NettyIntent.ACTION_CHECK_SESSION.equals(intent.getAction())) {
			scheduleToReconnect();
		} else if(NettyIntent.ACTION_DISCONNECT_SESSION.equals(intent.getAction())) {
			disconnectSessionIfItNeeds();
		}
	}


	/** Session 의 연결상태를 확인한다. 필요할 경우 HeartBeat을 전송.*/
	boolean checkConnection() {
		boolean result = false;
		if(mChannel != null && mChannel.isConnected() == true) {
			//If it needs you should send a packet through the channel.
			result = true;
		}
		LogByCodeLab.v(NettyService.class.getSimpleName() + ".checkConnection(), mChannel: " + mChannel + ", result: " + result);
		return result;
	}

	/** 연결을 다시 맺어야 할 경우 connection 을 닫는다.
	 */
	void disconnectSessionIfItNeeds() {
		if(checkConnection() == true) {
			ChannelFuture future = mChannel.disconnect();
			future.awaitUninterruptibly();
		}
	}


	/** Schedule a reconnect event by using {@link android.app.AlarmManager}*/
	public void scheduleToReconnect() {
		if(NettyService.isConnectAlreadyScheduled == true) {
			return;
		}

		//Random Integer 를 더해서 서버에 접속 부하를 줄인다.
		if(INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF < INTERVAL_RECONNECT_MAXIMUM) {
			INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF += new Random().nextInt(1000);
			LogByCodeLab.i(String.format("%s.scheduleToReconnect() delay: %dsec", NettyService.class.getSimpleName(), INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF/1000));

			ServiceUtil.startSchedule(this, NettyIntent.ACTION_CONNECT_SESSION, INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF);
			INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF *=2;
			NettyService.isConnectAlreadyScheduled = true;
		}
	}

	void connectSessionIfItNeeds() {
		if(checkConnection() == false) {
			ServiceUtil.startSchedule(this, NettyIntent.ACTION_CHECK_SESSION, INTERVAL_WAIT_FOR_RESPONSE);
			ServiceUtil.stopSchedule(this, NettyIntent.ACTION_HEARTBEAT);
		}
	}

	/** request후 response가 30초 이내에 응답이 와야 함 */
	static final long INTERVAL_WAIT_FOR_RESPONSE = 30 * 1000;

	static final long INTERVAL_RECONNECT_MINIMUM = 10 * 1000;

	static long INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF = INTERVAL_RECONNECT_MINIMUM;
	/** 이 값에 도달하면 서비스를 재시작 해본다.*/
	static final long INTERVAL_RECONNECT_MAXIMUM = 30 * 60 * 1000;
}
