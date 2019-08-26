package com.dragonforest.app.faceclient;

import android.app.ActionBar;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.dragonforest.app.faceclient.arcsoft.faceserver.CompareResult;
import com.dragonforest.app.faceclient.arcsoft.faceserver.FaceServer;
import com.dragonforest.app.faceclient.arcsoft.model.DrawInfo;
import com.dragonforest.app.faceclient.arcsoft.model.FacePreviewInfo;
import com.dragonforest.app.faceclient.arcsoft.util.ConfigUtil;
import com.dragonforest.app.faceclient.arcsoft.util.DrawHelper;
import com.dragonforest.app.faceclient.arcsoft.util.camera.CameraHelper;
import com.dragonforest.app.faceclient.arcsoft.util.camera.CameraListener;
import com.dragonforest.app.faceclient.arcsoft.util.face.FaceHelper;
import com.dragonforest.app.faceclient.arcsoft.util.face.FaceListener;
import com.dragonforest.app.faceclient.arcsoft.util.face.RequestFeatureStatus;
import com.dragonforest.app.faceclient.arcsoft.widget.FaceRectView;
import com.dragonforest.app.faceclient.util.ArcUtil;
import com.faceclient.sdk.ArcFaceSDK;
import com.faceclient.sdk.beans.Person;
import com.faceclient.sdk.beans.Result;
import com.faceclient.sdk.beans.Vender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 人脸识别/人脸搜索
 * 无需按钮 自动检测
 */
public class FaceAutoRecognizeActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener {

    /**
     * 相机预览控件
     */
    private View previewView;
    /**
     * 绘制人脸控件
     */
    private FaceRectView faceRectView;
    private Button btn_recognize;
    private Switch switchLivenessDetect;

    // 数据
    private FaceEngine faceEngine;
    private FaceHelper faceHelper;
    private int afCode = -1;
    private static final int MAX_DETECT_NUM = 10;

    private String currentName = "";
    private String currentSimilar = "";

    /**
     * 活体检测的开关
     */
    private boolean livenessDetect = false;

    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;

    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();
    private List<CompareResult> compareResultList;

    private static final float SIMILAR_THRESHOLD = 0.8F;

    private Integer rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;

    /**
     * 识别人脸状态码，检测通过
     */
    private static final int RECOGNIZE_STATUS_READY = 0;
    /**
     * 识别人脸状态码，检测通过
     */
    private static final int RECOGNIZE_STATUS_PASS = 2;
    /**
     * 识别人脸状态码，检测未通过
     */
    private static final int RECOGNIZE_STATUS_NOT_PASS = 1;
    /**
     * 最大检测次数，超过则检测失败
     */
    private static final int MAX_RECOGNIZE_COUNT = 5;
    private int recognizeStatus = RECOGNIZE_STATUS_READY;
    private int recognizeCount = 0;


    /**
     * 时间间隔 即隔多长时间检测一次
     */
    private static final long TIME_INTERVAL = 1000 * 5;
    /**
     * 上一次检测的时间点
     */
    private long lastDetectTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_auto_recognize);
        initView();
        setToolbarTitle("人脸识别（自动，" + TIME_INTERVAL / 1000 + "s/1次，尝试" + MAX_RECOGNIZE_COUNT + "次）");
    }

    private void setToolbarTitle(String title) {
        setTitle(title);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    private void initView() {
        previewView = findViewById(R.id.texture_preview);
        //在布局结束后才做初始化操作
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        faceRectView = findViewById(R.id.face_rect_view);
        btn_recognize = findViewById(R.id.btn_recognize);
        btn_recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLeftFace();
                Toast.makeText(FaceAutoRecognizeActivity.this, "重新开始检测", Toast.LENGTH_SHORT).show();
            }
        });
        switchLivenessDetect = findViewById(R.id.switch_liveness_detect);
        switchLivenessDetect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                livenessDetect = isChecked;
                clearLeftFace();
            }
        });
        compareResultList = new ArrayList<>();
    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        faceEngine = ArcUtil.getInstance().getFaceEngine();
    }

    /**
     * 销毁引擎
     */
    private void unInitEngine() {
        if (afCode == ErrorInfo.MOK) {
            afCode = faceEngine.unInit();
            Log.i("TAG", "unInitEngine: " + afCode);
        }
    }

    private void initCamera() {
        final FaceListener faceListener = new FaceListener() {
            @Override
            public void onFail(Exception e) {
                Log.e("TAG", "onFail: " + e.getMessage());
            }

            //请求FR的回调
            @Override
            public void onFaceFeatureInfoGet(@Nullable FaceFeature faceFeature, final Integer requestId) {
                if (faceFeature != null) {
                    // 不做活体检测的情况，直接搜索
                    if (!livenessDetect) {
                        searchFace(faceFeature.getFeatureData());
                    }
                    // 活体检测通过，搜索特征
                    else if (livenessMap.get(requestId) != null && livenessMap.get(requestId) == LivenessInfo.ALIVE) {
                        searchFace(faceFeature.getFeatureData());
                    }
                }
            }

        };


        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height, previewView.getWidth(), previewView.getHeight(), displayOrientation
                        , cameraId, isMirror, false, false);
                Log.i("TAG", "onCameraOpened: " + drawHelper.toString());
                faceHelper = new FaceHelper.Builder()
                        .faceEngine(faceEngine)
                        .frThreadNum(MAX_DETECT_NUM)
                        .previewSize(previewSize)
                        .faceListener(faceListener)
                        .currentTrackId(ConfigUtil.getTrackId(FaceAutoRecognizeActivity.this.getApplicationContext()))
                        .build();
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }
                List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21);
                if (facePreviewInfoList != null && faceRectView != null && drawHelper != null) {
                    drawPreviewInfo(facePreviewInfoList);
                }
                if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
                    clearLeftFace();
                }
                if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && previewSize != null) {
                    for (int i = 0; i < facePreviewInfoList.size(); i++) {
                        if (livenessDetect) {
                            livenessMap.put(facePreviewInfoList.get(i).getTrackId(), facePreviewInfoList.get(i).getLivenessInfo().getLiveness());
                        }
                        /**
                         * 对于每个人脸，若状态为空或者为失败，则请求FR（可根据需要添加其他判断以限制FR次数），
                         * FR回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer)}中回传
                         */
                        if (requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == null
                                || requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == RequestFeatureStatus.FAILED) {
                            faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
                        }
                    }
                }

            }

            @Override
            public void onCameraClosed() {
                Log.i("TAG", "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i("TAG", "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i("TAG", "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(rgbCameraID != null ? rgbCameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
        cameraHelper.start();
    }

    /**
     * 删除已经离开的人脸
     */
    private void clearLeftFace() {
        livenessMap.clear();
        currentName = "";
        currentSimilar = "";
        recognizeCount = 0;
        recognizeStatus = RECOGNIZE_STATUS_READY;
    }

    /**
     * 搜索人脸
     *
     * @param feature
     */
    private void searchFace(final byte[] feature) {
        long currentDetectTime = System.currentTimeMillis();
        if (recognizeStatus == RECOGNIZE_STATUS_READY
                && (currentDetectTime - lastDetectTime) > TIME_INTERVAL) {
            if (recognizeCount > MAX_RECOGNIZE_COUNT) {
                currentName = "检测未通过";
                recognizeStatus = RECOGNIZE_STATUS_NOT_PASS;
                return;
            }
            lastDetectTime = currentDetectTime;
            recognizeCount++;
            Observable.create(new ObservableOnSubscribe<String>() {
                @Override
                public void subscribe(ObservableEmitter<String> emitter) {
                    if (feature != null) {
                        Result result = ArcFaceSDK.getInstance().searchFaceByFeature(-1, feature, Vender.ARCSOFT, "v1.1", "MX001");
                        if (result == null) {
                            emitter.onError(new Exception("result is null.http error"));
                        } else if (result.getCode() != 0) {
                            emitter.onError(new Exception(result.getMsg()));
                        } else {
                            Person person = (Person) result.getData();
                            float similar = result.getSimilar();
                            currentName = person.getUname();
                            currentSimilar = "(" + similar + ")";
                            emitter.onNext("找到最接近的人，姓名：" + person.getUname() + ",相似度：" + similar);
                            if (similar > SIMILAR_THRESHOLD) {
                                recognizeStatus = RECOGNIZE_STATUS_PASS;
                                currentSimilar = "(" + similar + ")" + "已通过";
                            }
                        }
                    } else {
                        currentName = "unknow";
                        currentSimilar = "(0.0)";
                        emitter.onError(new Exception("保存失败,图像中无人脸信息"));
                    }
                }
            })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<String>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(String msg) {
                            Toast.makeText(FaceAutoRecognizeActivity.this, msg, Toast.LENGTH_SHORT).show();
                            Log.e(getClass().getSimpleName(), "查询结果：" + msg);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(FaceAutoRecognizeActivity.this, "识别失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(getClass().getSimpleName(), "识别失败：" + e.getMessage());
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        List<DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
//            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE,
//                    liveness == null ? LivenessInfo.UNKNOWN : liveness,
//                    name == null ? String.valueOf(facePreviewInfoList.get(i).getTrackId()) : name));
            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE,
                    liveness == null ? LivenessInfo.UNKNOWN : liveness,
                    currentName + currentSimilar));
        }
        drawHelper.draw(faceRectView, drawInfoList);
    }

    @Override
    public void onGlobalLayout() {
        initEngine();
        initCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }

        //faceHelper中可能会有FR耗时操作仍在执行，加锁防止crash
        if (faceHelper != null) {
            synchronized (faceHelper) {
                unInitEngine();
            }
            ConfigUtil.setTrackId(this, faceHelper.getCurrentTrackId());
            faceHelper.release();
        } else {
            unInitEngine();
        }
        FaceServer.getInstance().unInit();
    }
}
