package com.hovans.netty.tcpsample.ui;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.hovans.netty.tcpsample.R;
import com.hovans.netty.tcpsample.service.NettyIntent;

public class HomeActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

	public void onStartClick(View view) {
		startService(new Intent(NettyIntent.ACTION_CONNECT_SESSION));
	}

	public void onStopClick(View view) {
		startService(new Intent(NettyIntent.ACTION_DISCONNECT_SESSION));
	}
}
