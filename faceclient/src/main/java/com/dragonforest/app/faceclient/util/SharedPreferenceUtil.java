package com.dragonforest.app.faceclient.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author 韩龙林
 * @date 2019/8/23 15:00
 */
public class SharedPreferenceUtil {
    public static final String FILE_NAME="faceClientDemo";

    public static final String KEY_FACESERVER_IP="faceServerIp";
    public static final String KEY_FACESERVER_PORT="faceServerPort";

    public static void set(Context context,String key,String value){
        SharedPreferences sp=context.getSharedPreferences(FILE_NAME,Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putString(key,value);
        edit.commit();
    }

    public static String get(Context context,String key){
        SharedPreferences sp=context.getSharedPreferences(FILE_NAME,Context.MODE_PRIVATE);
        String value = sp.getString(key, "");
        return value;
    }

}
