package com.dragonforest.app.faceclient.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.VersionInfo;
import com.dragonforest.app.faceclient.arcsoft.common.Constants;

/**
 * @author 韩龙林
 * @date 2019/8/23 14:22
 */
public class ArcUtil {
    private static final int MAX_DETECT_NUM = 10;
    FaceEngine faceEngine = null;

    static ArcUtil instance;

    public static ArcUtil getInstance() {
        if (instance == null) {
            instance = new ArcUtil();
        }
        return instance;
    }

    private ArcUtil() {
        faceEngine = new FaceEngine();
    }

    /**
     * 激活引擎
     *
     * @param context
     * @return
     */
    public boolean activeEngine(Context context) {
        boolean isEngineActive=false;
        int activeCode = faceEngine.activeOnline(context, Constants.APP_ID, Constants.SDK_KEY);
        if (activeCode == ErrorInfo.MOK) {
            // 激活成功
            isEngineActive = true;
            Toast.makeText(context, "引擎激活成功", Toast.LENGTH_SHORT).show();
        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
            // 已经激活
            isEngineActive = true;
            Toast.makeText(context, "引擎激活成功", Toast.LENGTH_SHORT).show();
        } else {
            // 激活失败
            isEngineActive = false;
            Toast.makeText(context, "引擎激活失败", Toast.LENGTH_SHORT).show();
        }
        return isEngineActive;
    }

    /**
     * 初始化引擎
     */
    public boolean initEngine(Context context) {
        int afCode = -1;
        afCode = faceEngine.init(context, FaceEngine.ASF_DETECT_MODE_VIDEO, FaceEngine.ASF_OP_0_HIGHER_EXT,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_LIVENESS);
        VersionInfo versionInfo = new VersionInfo();
        faceEngine.getVersion(versionInfo);
        Log.i("TAG", "initEngine:  init: " + afCode + "  version:" + versionInfo);

        if (afCode != ErrorInfo.MOK) {
            return false;
        }
        return true;
    }

    public FaceEngine getFaceEngine() {
        return faceEngine;
    }
}
