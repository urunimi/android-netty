/*
 * @(#) NSSAESEncoder.java Oct 24, 2012 
 *
 * Copyright 2012 NHN Corp. All rights Reserved. 
 * NHN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.hovans.netty.tcpsample.network;

import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import android.content.Context;
import android.os.PowerManager;

import com.hovans.android.log.LogByCodeLab;
import com.hovans.netty.tcpsample.service.NettyService;
import com.hovans.netty.tcpsample.util.WakeLockWrapper;

/**
 * 기존에 jsonString 을 받도록 한 것을 가독성을 위해 object로 변경 [2013-03-19, 유병우]
 * @author 유승현, 유병우
 */
public class ChannelEncoder extends SimpleChannelHandler {

	Context mContext;

	public ChannelEncoder(Context context) {
		super();
		mContext = context;
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
		
		PowerManager.WakeLock wakeLock = WakeLockWrapper.getWakeLockInstance(mContext, ChannelEncoder.class.getSimpleName());
		wakeLock.acquire();

		try {
			String jsonString = event.getMessage().toString();

			LogByCodeLab.i(String.format("WRITE [%s]", jsonString));

			ChannelBuffer cb = new BigEndianHeapChannelBuffer(24);
			cb.writeBytes(new byte[24]);

			Channels.write(ctx, event.getFuture(), cb);
		} finally {
			wakeLock.release();
		}
	}
}
