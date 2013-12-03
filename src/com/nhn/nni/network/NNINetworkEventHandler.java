package com.nhn.nni.network;

import static com.nhn.nni.network.NNINetworkController.INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF;
import static com.nhn.nni.network.NNINetworkController.INTERVAL_RECONNECT_MINIMUM;

import java.util.HashMap;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.json.JSONArray;

import android.app.Service;
import android.content.Intent;

import com.nhn.nni.core.Logger;
import com.nhn.nni.core.NNIIntent;
import com.nhn.nni.network.NNIDataManager.SubscribeInfo;
import com.nhn.nni.protocol.ConnectRequest;
import com.nhn.nni.protocol.ConnectResponse;
import com.nhn.nni.protocol.HeartbeatResponse;
import com.nhn.nni.protocol.NotificationRequest;
import com.nhn.nni.protocol.NotificationResponse;
import com.nhn.nni.protocol.SubscribeRequest;
import com.nhn.nni.protocol.SubscribeResponse;
import com.nhn.nni.util.PermissionManager;
import com.nhn.nni.util.PermissionManager.WakeLockWrapper;

/**
 * 네트워크 관련 이벤트가 발생하면 여기 있는 callback 이 호출 된다. 여기서는 아래의 항목들에 대해 관리한다.
 * <p>
 * - 새로운 채널이 연결될 때 {@link #channelConnected(ChannelHandlerContext, ChannelStateEvent)}
 * <br/> - 채널에서 새로운 메시지가 유입될 때 {@link #messageReceived(ChannelHandlerContext, MessageEvent)}
 * <br/> - Exception 이 발생 했을 때 {@link #exceptionCaught(ChannelHandlerContext, ExceptionEvent)}
 * <br/> - 채널이 닫힐 때 {@link #channelClosed(ChannelHandlerContext, ChannelStateEvent)}
 * <br/>
 * <p>
 * @author 유병우(urunimi@nhn.com)
 */
public class NNINetworkEventHandler extends SimpleChannelHandler {
	
	/** Notification 이 중복으로 올 경우 삭제할 수 있도록 Event Id를 관리한다. */
	volatile HashMap<String,String> lastEventIds = new HashMap<String, String>();

	/** 최초 연결 수입시 호출된다. */
	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		Logger.w(String.format("%s.channelConnected()", NNINetworkEventHandler.class.getSimpleName()));
		super.channelConnected(ctx, e);
		ctx.getChannel().write(new ConnectRequest());
	}

	/** 서버에서 보내주는 메시지 Object를 수신한다. */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		WakeLockWrapper wakeLock = WakeLockWrapper.getWakeLockInstance(NNIServiceHandler.getContext(), NNINetworkEventHandler.class.getSimpleName());
		wakeLock.acquire();
		try {
			super.messageReceived(ctx, e);
			Object message = e.getMessage();
			
			if(message == null) return;
			
			Logger.w(String.format("%s.messageReceived() Received Packet is %s", NNINetworkEventHandler.class.getSimpleName(), message.getClass().getSimpleName()));
			if (message instanceof ConnectResponse) {
				
				onConnectReceived(ctx, (ConnectResponse) message);
			
			} else if (message instanceof SubscribeResponse) {
				
				onSubscribeReceived(ctx, (SubscribeResponse) message);
				
			} else if (message instanceof NotificationRequest) {
				
				onNotificationReceived(ctx, (NotificationRequest) message);
				
			} else if (message instanceof HeartbeatResponse) {
				
				//Heartbeat response 를 받았으므로 재접속 스케쥴을 취소
				NNIServiceHandler.stopSchedule(NNIIntent.ACTION_CHECK_SESSION);
			}
		} finally {
			wakeLock.release();
		}
	}
	
	/** 주로 Disconnected 와 같은 에러가 이쪽으로 유입된다. */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		Logger.w(String.format("%s.exceptionCaught() Exception: %s", NNINetworkEventHandler.class.getSimpleName(), Logger.getStringFromThrowable(e.getCause())));
		super.exceptionCaught(ctx, e);
		
		if(ctx.getChannel() != null && ctx.getChannel().isOpen()) {
			ctx.getChannel().close();
		} else {
			NNINetworkController.getInstance().scheduleToReconnect();
		}
	}

	/** 채널이 닫힐 때 reconnect를 예약한다. */
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		Logger.w(String.format("%s.channelClosed()", NNINetworkEventHandler.class.getSimpleName()));
		super.channelClosed(ctx, e);
		NNINetworkController.getInstance().scheduleToReconnect();
	}

	/**
	 * Connect Response 에 대해 처리. 먼저 Connect 패킷에 대해 분석 하고 문제가 있을 경우 Exception 던짐
	 * @param ctx
	 * @param message
	 */
	void onConnectReceived(ChannelHandlerContext ctx, ConnectResponse message) {
		NNIServiceHandler.stopSchedule(NNIIntent.ACTION_CHECK_SESSION);
		NNIServiceHandler.stopSchedule(NNIIntent.ACTION_CONNECT_SESSION);
		NNIServiceHandler.stopSchedule(NNIIntent.ACTION_RESTART_SERVICE);
		INTERVAL_RECONNECT_EXPONENTIAL_BACKOFF = INTERVAL_RECONNECT_MINIMUM;
		
		switch (NNIDataManager.getInstance().parseConnectedInfo(message.getParameters())) {
		case 0: {
			ctx.getChannel().write(new SubscribeRequest());
			NNIServiceHandler.startSchedule(NNIIntent.ACTION_CHECK_SESSION, NNINetworkController.INTERVAL_WAIT_FOR_RESPONSE);
			break;
		}
		default:
			throw new RuntimeException("Fail to Connect to NNI Server");
		}
		//NNI 연결 되었음을 Broadcast
		NNIServiceHandler.sendEventToSubscribers("NNI Connected");
	}
	
	void onSubscribeReceived(ChannelHandlerContext ctx, SubscribeResponse message) {
		NNIServiceHandler.stopSchedule(NNIIntent.ACTION_CHECK_SESSION);
		//NNI 구독 되었음을 Broadcast
		NNIServiceHandler.sendEventToSubscribers("NNI Subscribe Success");
		NNIServiceHandler.repeatSchedule(NNIIntent.ACTION_HEARTBEAT, NNIDataManager.getInstance().getPingInterval() * 1000);
	}
	
	void onNotificationReceived(ChannelHandlerContext ctx, NotificationRequest notificationPacket) {
		Service serviceContext = NNIServiceHandler.getContext();
		if (serviceContext == null) {
			return;
		}

		try {
			JSONArray parameterArray = notificationPacket.getParameters();

			int paramLen = parameterArray.length();
			for (int i = 0; i < paramLen; i++) {
				JSONArray notiArray = parameterArray.getJSONArray(i);
				int parameterCount = 0;
				@SuppressWarnings("unused")
				int protocolVersion = notiArray.getInt(parameterCount++);
				String eventId = notiArray.getString(parameterCount++);
				String serviceId = notiArray.getString(parameterCount++);
				int categoryId = notiArray.getInt(parameterCount++);
				String sender = notiArray.getString(parameterCount++);
				String message = notiArray.getString(parameterCount++);
				

				String lastEventId = lastEventIds.get(serviceId);
				
				if (eventId.equals(lastEventId) == true) {
					continue;
				}
				
				lastEventIds.put(serviceId, eventId);

				Intent notiIntent = new Intent().setAction(NNIIntent.ACTION_FROM_NNI_MESSAGE);
				notiIntent.putExtra(NNIIntent.PARAM_EVENT_ID, eventId);
				notiIntent.putExtra(NNIIntent.PARAM_SERVICE_ID, serviceId);
				notiIntent.putExtra(NNIIntent.PARAM_CATEGORY_ID, categoryId);
				notiIntent.putExtra(NNIIntent.PARAM_FROM, sender);
				notiIntent.putExtra(NNIIntent.PARAM_MESSAGE, message);
				
				SubscribeInfo subscribeInfo = NNIDataManager.getInstance().getSubscribeInfo(serviceId);
				
				notiIntent.setPackage(subscribeInfo.getPackageName());

				boolean isValid = PermissionManager.checkAndUpdateIntent(serviceContext, notiIntent);

//				String logString = "[Recv] " + NNINetworkController.getInstance().getCurrentBatteryLevel()
//						+ getClass().getSimpleName() + "Nni.onNotiPushEvent() " + " eventId: " + eventId
//						+ ", serviceId: " + serviceId + ", categoryId: " + categoryId + ", sender: " + sender;

				if (isValid) {
					serviceContext.sendBroadcast(notiIntent);
//					Logger.i(logString);
//				} else {
//					Logger.e(logString);
				}

				// NOTI에 대한 ACK를 날려준다.
				String targetId = NNIDataManager.getInstance().getServiceTargetId(serviceId);
				
				ctx.getChannel().write(new NotificationResponse(eventId, serviceId, targetId));
			}

		} catch (Exception e) {
			Logger.e(e);
		}
	}
}
