package com.dragonforest.app.facedemo.application;

import android.app.Application;

import com.dragonforest.app.facedemo.utils.ArcHttpUtil;
import com.lzy.okgo.OkGo;

/**
 * @author 韩龙林
 * @date 2019/8/8 10:48
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ArcHttpUtil.getInstance().init("192.168.100.153","8080");
        OkGo.getInstance().init(this);
    }
}
