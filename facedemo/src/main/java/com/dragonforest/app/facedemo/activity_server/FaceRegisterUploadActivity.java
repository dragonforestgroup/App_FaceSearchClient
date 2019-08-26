package com.dragonforest.app.facedemo.activity_server;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.arcsoft.face.util.ImageUtils;
import com.dragonforest.app.facedemo.R;
import com.dragonforest.app.facedemo.arcsoft.faceserver.CompareResult;
import com.dragonforest.app.facedemo.arcsoft.faceserver.FaceServer;
import com.dragonforest.app.facedemo.arcsoft.model.DrawInfo;
import com.dragonforest.app.facedemo.arcsoft.model.FacePreviewInfo;
import com.dragonforest.app.facedemo.arcsoft.util.ConfigUtil;
import com.dragonforest.app.facedemo.arcsoft.util.DrawHelper;
import com.dragonforest.app.facedemo.arcsoft.util.camera.CameraHelper;
import com.dragonforest.app.facedemo.arcsoft.util.camera.CameraListener;
import com.dragonforest.app.facedemo.arcsoft.util.face.FaceHelper;
import com.dragonforest.app.facedemo.arcsoft.util.face.FaceListener;
import com.dragonforest.app.facedemo.arcsoft.widget.FaceRectView;
import com.dragonforest.app.facedemo.dialog.RegisterDialog;
import com.dragonforest.app.facedemo.httpmock.ArcFaceSDK;
import com.dragonforest.app.facedemo.httpmock.PersonInfo;
import com.dragonforest.app.facedemo.utils.ArcHttpUtil;

import java.io.File;
import java.io.FileOutputStream;
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
 * 人脸注册 上传图像
 */
public class FaceRegisterUploadActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener {

    /**
     * 相机预览控件
     */
    private View previewView;
    /**
     * 绘制人脸控件
     */
    private FaceRectView faceRectView;
    private Button btn_register;
    private Switch switchLivenessDetect;

    // 数据
    private FaceEngine faceEngine;
    private FaceHelper faceHelper;
    private int afCode = -1;
    private static final int MAX_DETECT_NUM = 10;

    String currentName="";

    /**
     * 当前人的信息
     */
    private PersonInfo personInfo;

    /**
     * 活体检测的开关
     */
    private boolean livenessDetect = true;

    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;

    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();
    private List<CompareResult> compareResultList;

    private static final float SIMILAR_THRESHOLD = 0.8F;

    private Integer rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;

    /**
     * 注册人脸状态码，准备注册
     */
    private static final int REGISTER_STATUS_READY = 0;
    /**
     * 注册人脸状态码，注册中
     */
    private static final int REGISTER_STATUS_PROCESSING = 1;
    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    private static final int REGISTER_STATUS_DONE = 2;
    private int registerStatus = REGISTER_STATUS_DONE;
    private RegisterDialog registerDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_register_upload);
        initView();
    }

    private void initView() {
        previewView = findViewById(R.id.texture_preview);
        //在布局结束后才做初始化操作
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        faceRectView = findViewById(R.id.face_rect_view);
        switchLivenessDetect = findViewById(R.id.switch_liveness_detect);
        btn_register = findViewById(R.id.btn_register);
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRegister();
            }
        });

        compareResultList = new ArrayList<>();
    }

    /**
     * 点击注册
     */
    private void clickRegister() {
        if (personInfo == null) {
            // 显示输入信息
            if (registerDialog == null) {
                registerDialog = new RegisterDialog(this);
                registerDialog.setCancelable(false);
                registerDialog.setOnRegisterListener(new RegisterDialog.OnRegisterListener() {
                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onRegister(String name, String sex, int groupId) {
                        personInfo = new PersonInfo();
                        personInfo.setGroupId(groupId);
                        personInfo.setSex(sex);
                        personInfo.setName(name);
                        currentName=name;
                        Toast.makeText(FaceRegisterUploadActivity.this, "设置完成", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            registerDialog.show();
        } else {
            if (registerStatus == REGISTER_STATUS_DONE) {
                registerStatus = REGISTER_STATUS_READY;
            }
        }
    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        faceEngine = new FaceEngine();
        afCode = faceEngine.init(this, FaceEngine.ASF_DETECT_MODE_VIDEO, FaceEngine.ASF_OP_0_HIGHER_EXT,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_LIVENESS);
        VersionInfo versionInfo = new VersionInfo();
        faceEngine.getVersion(versionInfo);
        Log.i("TAG", "initEngine:  init: " + afCode + "  version:" + versionInfo);

        if (afCode != ErrorInfo.MOK) {
            Toast.makeText(this, "初始化失败", Toast.LENGTH_SHORT).show();
        }
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
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId) {
                Log.e("TAG", "onFaceFeatureInfoGet(),thread:" + Thread.currentThread().getName());

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
                        .currentTrackId(ConfigUtil.getTrackId(FaceRegisterUploadActivity.this.getApplicationContext()))
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
                registerFace(nv21,camera,facePreviewInfoList);
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

    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        List<DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE,
                    liveness == null ? LivenessInfo.UNKNOWN : liveness,
                    currentName));
        }
        drawHelper.draw(faceRectView, drawInfoList);
    }

    private void registerFace(final byte[] nv21, final Camera camera,final List<FacePreviewInfo> facePreviewInfoList) {
        if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList.size() > 0) {
            registerStatus = REGISTER_STATUS_PROCESSING;
            Observable.create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(ObservableEmitter<Boolean> emitter) {
                    int width = camera.getParameters().getPreviewSize().width;
                    int height = camera.getParameters().getPreviewSize().height;

                    String path=getFilesDir() + File.separator + "myFace.jpg";
                    boolean isSaved = saveToFile(path, nv21,width, height, facePreviewInfoList);
                    // TODO: 2019/8/6 服务器端注册人脸并返回
                    if (isSaved) {
                        boolean isRegisted= ArcHttpUtil.getInstance().registerFaceByFile(path,currentName,personInfo.getGroupId(),1, 1);
                        if(!isRegisted){
                            emitter.onError(new Exception("similar is null,error in http"));
                        }else{
                            emitter.onNext(isRegisted);
                        }
                    } else {
                        emitter.onError(new Exception("保存失败,图像中无人脸信息"));
                    }
                    boolean success = true;
                    emitter.onNext(success);
                }
            })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Boolean success) {
                            String result = success ? "register success!" : "register failed!";
                            Toast.makeText(FaceRegisterUploadActivity.this, result, Toast.LENGTH_SHORT).show();
                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(FaceRegisterUploadActivity.this, "register failed!"+e.getMessage(), Toast.LENGTH_SHORT).show();
                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    private void registerFace(final FaceFeature feature) {
        if (registerStatus == REGISTER_STATUS_READY) {
            registerStatus = REGISTER_STATUS_PROCESSING;
            personInfo.setFaceFeature(feature);
            Observable.create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(ObservableEmitter<Boolean> emitter) {
                    // TODO: 2019/8/6 服务器端注册人脸并返回
                    boolean success = ArcFaceSDK.getInstance().registerFace(personInfo);
                    emitter.onNext(success);
                }
            })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Boolean success) {
                            String result = success ? "register success!" : "register failed!";
                            Toast.makeText(FaceRegisterUploadActivity.this, result, Toast.LENGTH_SHORT).show();
                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(FaceRegisterUploadActivity.this, "register failed!", Toast.LENGTH_SHORT).show();
                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onComplete() {
                            Toast.makeText(FaceRegisterUploadActivity.this, "register complete!", Toast.LENGTH_SHORT).show();
                            registerStatus = REGISTER_STATUS_DONE;
                        }
                    });
        }
    }

    /**
     * 保存人脸图片
     *
     * @param nv21
     * @param width
     * @param height
     * @param facePreviewInfoList
     */
    private boolean saveToFile(String path,byte[] nv21, int width, int height, List<FacePreviewInfo> facePreviewInfoList) {
        try {
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            if (facePreviewInfoList != null && facePreviewInfoList.size() > 0) {
                FaceInfo faceInfo = facePreviewInfoList.get(0).getFaceInfo();
//        //为了美观，扩大rect截取注册图
//        Rect cropRect = getBestRect(width, height, faceInfo.getRect());
//        if (cropRect == null) {
//            return false;
//        }
                File file = new File(path);
                FileOutputStream fosImage = new FileOutputStream(file);
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, fosImage);
                fosImage.close();
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

                //判断人脸旋转角度，若不为0度则旋转注册图
                boolean needAdjust = false;
                if (bitmap != null) {
                    switch (faceInfo.getOrient()) {
                        case FaceEngine.ASF_OC_0:
                            break;
                        case FaceEngine.ASF_OC_90:
                            bitmap = ImageUtils.rotateBitmap(bitmap, 90);
                            needAdjust = true;
                            break;
                        case FaceEngine.ASF_OC_180:
                            bitmap = ImageUtils.rotateBitmap(bitmap, 180);
                            needAdjust = true;
                            break;
                        case FaceEngine.ASF_OC_270:
                            bitmap = ImageUtils.rotateBitmap(bitmap, 270);
                            needAdjust = true;
                            break;
                        default:
                            break;
                    }
                }
                if (needAdjust) {
                    fosImage = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fosImage);
                    fosImage.close();
                }
                return true;
            } else {
                // 无人脸信息
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
