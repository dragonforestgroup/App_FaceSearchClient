package com.dragonforest.app.faceclient.application;

import android.app.Application;
import android.util.Log;

import com.dragonforest.app.faceclient.config.Configuration;
import com.dragonforest.app.faceclient.util.ArcUtil;
import com.dragonforest.app.faceclient.util.SharedPreferenceUtil;
import com.faceclient.sdk.ArcFaceSDK;

/**
 * @author 韩龙林
 * @date 2019/8/23 13:10
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initFaceServer();
    }

    private void initFaceServer() {
        String ip = SharedPreferenceUtil.get(this, SharedPreferenceUtil.KEY_FACESERVER_IP);
        String port = SharedPreferenceUtil.get(this, SharedPreferenceUtil.KEY_FACESERVER_PORT);
        if (ip == null || "".equals(ip)) {
            ip = Configuration.FACESERVER_IP;
            port = Configuration.FACESERVER_PORT;
        }
        ArcFaceSDK.getInstance().init(ip, port);
        Log.e(getClass().getSimpleName(), "初始化faceServer完成，ip:" + ip + ",port:" + port);
    }
}
