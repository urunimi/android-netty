package com.nhn.nni.core;

import com.nhn.nni.NNIConstant;

public class NNIIntent extends com.nhn.nni.NNIIntent {
	public static final String ACTION_HEARTBEAT = NNIConstant.PACKAGE_TYPE.toString() + ".intent.HEARTBEAT";
	public static final String ACTION_CHECK_SESSION = NNIConstant.PACKAGE_TYPE.toString() + ".intent.CHECK_SESSION";
	public static final String ACTION_CONNECT_SESSION = NNIConstant.PACKAGE_TYPE.toString() + ".intent.CONNECT_SESSION";
	public static final String ACTION_RESTART_SERVICE = NNIConstant.PACKAGE_TYPE.toString() + ".intent.RESTART_SERVICE";
}
