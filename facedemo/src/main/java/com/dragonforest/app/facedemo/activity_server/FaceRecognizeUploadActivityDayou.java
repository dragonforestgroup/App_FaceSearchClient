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
import com.dayouzc.e2e.core.callback.JsonCallback;
import com.dayouzc.e2eplatform.common.sdk.FileManageSDK;
import com.dayouzc.e2eplatform.core.dto.common.ResponseData;
import com.dayouzc.e2eplatform.core.dto.file.E2eDocDTO;
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
import com.dragonforest.app.facedemo.utils.ArcHttpUtil;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;

import java.io.ByteArrayOutputStream;
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

public class FaceRecognizeUploadActivityDayou extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener {

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

    private String currentName="";

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
     * 识别人脸状态码，准备识别
     */
    private static final int RECOGNIZE_STATUS_READY = 0;
    /**
     * 识别人脸状态码，识别中
     */
    private static final int RECOGNIZE_STATUS_PROCESSING = 1;
    /**
     * 识别人脸状态码，识别结束（无论成功失败）
     */
    private static final int RECOGNIZE_STATUS_DONE = 2;
    private int recognizeStatus = RECOGNIZE_STATUS_DONE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognize_upload);
        initView();
    }

    private void initView() {
        previewView = findViewById(R.id.texture_preview);
        //在布局结束后才做初始化操作
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        faceRectView = findViewById(R.id.face_rect_view);
        switchLivenessDetect = findViewById(R.id.switch_liveness_detect);
        btn_recognize = findViewById(R.id.btn_recognize);
        btn_recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRecognize();
            }
        });
        compareResultList = new ArrayList<>();
    }

    private void clickRecognize() {
        if (recognizeStatus == RECOGNIZE_STATUS_DONE) {
            recognizeStatus = RECOGNIZE_STATUS_READY;
            Toast.makeText(this, "开始识别", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        faceEngine = new FaceEngine();
//        afCode = faceEngine.init(this, FaceEngine.ASF_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(this),
//                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_LIVENESS);
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
            public void onFaceFeatureInfoGet(@Nullable FaceFeature faceFeature, final Integer requestId) {

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
                        .currentTrackId(ConfigUtil.getTrackId(FaceRecognizeUploadActivityDayou.this.getApplicationContext()))
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
                searchFace(nv21, camera,facePreviewInfoList);
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

    private void searchFace(final byte[] nv21, final Camera camera, final List<FacePreviewInfo> facePreviewInfoList) {
        if (recognizeStatus == RECOGNIZE_STATUS_READY&& facePreviewInfoList != null && facePreviewInfoList.size() > 0) {
            recognizeStatus = RECOGNIZE_STATUS_PROCESSING;
            int width = camera.getParameters().getPreviewSize().width;
            int height = camera.getParameters().getPreviewSize().height;

            String path=getFilesDir() + File.separator + "myFace.jpg";
            boolean isSaved = saveToFile(path, nv21,width, height, facePreviewInfoList);
            if(!isSaved){
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                return ;
            }
            String url = "http://172.16.17.98:8080/rbsvr/accounts/faces/recognize/images";
            OkGo.<ResponseData<List<E2eDocDTO>>>post(url)//
                    .tag(this)//
                    .isMultipart(true)
                    .params("file", new File(path))//
                    .params("action", "uploadFile")//
                    .params("actionVer", "V1.0")//
                    .params("ver", "V1.0")//
                    .params("respType", "XML")//
                    .execute(new JsonCallback<ResponseData<List<E2eDocDTO>>>() {
                        @Override
                        public void onStart(Request<ResponseData<List<E2eDocDTO>>, ? extends Request> request) {
                            super.onStart(request);
                            Toast.makeText(FaceRecognizeUploadActivityDayou.this, "onStart()", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Response<ResponseData<List<E2eDocDTO>>> response) {
                            super.onError(response);
                            Toast.makeText(FaceRecognizeUploadActivityDayou.this, "onError()", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onSuccess(Response<ResponseData<List<E2eDocDTO>>> response) {
                            ResponseData<List<E2eDocDTO>> responseData = response.body();
                            Toast.makeText(FaceRecognizeUploadActivityDayou.this, "onSuccess()", Toast.LENGTH_SHORT).show();
//                        E2EDocDTONew e2EDocDTONew = new E2EDocDTONew();
                            if (responseData != null) {
                                if ("10000".equals(responseData.getStatus())) {
//                                List<E2eDocDTO> mE2e = (List<E2eDocDTO>) responseData.getResult();
//                                E2eDocDTO e2eDocDTO = (E2eDocDTO) mE2e.get(0);
//                                e2EDocDTONew.setDocId(e2eDocDTO.getDocId());
//                                e2EDocDTONew.setDocName(e2eDocDTO.getDocName());
//                                e2EDocDTONew.setFileName(e2eDocDTO.getFileName());
//                                e2EDocDTONew.setFileNameExt(e2eDocDTO.getFileNameExt());
//                                e2EDocDTONew.setStatus(e2eDocDTO.getStatus());
//                                e2EDocDTONew.setFileSize(e2eDocDTO.getFileSize());
//                                e2EDocDTONew.setFilePath(filePath);
//                                itemDocDTOS.add(e2EDocDTONew);
//                                setResponseData(itemDocDTOS);
                                } else {
//                                ToastUtils.showToast(MyApplication.getmInstance(), responseData.getMsg());
                                }
                            } else {
//                            ToastUtils.showToast(MyApplication.getmInstance(), "服务器数据异常!");
                            }
                        }

                        @Override
                        public void onFinish() {
                            super.onFinish();
                        }

                        @Override
                        public void uploadProgress(Progress progress) {
                            super.uploadProgress(progress);

                        }
                    });
        }
    }

    /**
     * 上传文件
     *
     * @param path
     */
    private void upLoadFile(String path,String url) {
        OkGo.<ResponseData<List<E2eDocDTO>>>post(url)//
                .tag(this)//
                .isMultipart(true)
                .params("file", new File(path))//
                .params("action", "uploadFile")//
                .params("actionVer", "V1.0")//
                .params("ver", "V1.0")//
                .params("respType", "XML")//
                .execute(new JsonCallback<ResponseData<List<E2eDocDTO>>>() {
                    @Override
                    public void onStart(Request<ResponseData<List<E2eDocDTO>>, ? extends Request> request) {
                        super.onStart(request);
//                        pd = new ProgressDialog(mContext);
//                        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                        pd.setMessage("正在上传");
//                        pd.setCancelable(false);
//                        pd.show();
                    }

                    @Override
                    public void onError(Response<ResponseData<List<E2eDocDTO>>> response) {
                        super.onError(response);
                    }

                    @Override
                    public void onSuccess(Response<ResponseData<List<E2eDocDTO>>> response) {
                        ResponseData<List<E2eDocDTO>> responseData = response.body();
//                        E2EDocDTONew e2EDocDTONew = new E2EDocDTONew();
                        if (responseData != null) {
                            if ("10000".equals(responseData.getStatus())) {
//                                List<E2eDocDTO> mE2e = (List<E2eDocDTO>) responseData.getResult();
//                                E2eDocDTO e2eDocDTO = (E2eDocDTO) mE2e.get(0);
//                                e2EDocDTONew.setDocId(e2eDocDTO.getDocId());
//                                e2EDocDTONew.setDocName(e2eDocDTO.getDocName());
//                                e2EDocDTONew.setFileName(e2eDocDTO.getFileName());
//                                e2EDocDTONew.setFileNameExt(e2eDocDTO.getFileNameExt());
//                                e2EDocDTONew.setStatus(e2eDocDTO.getStatus());
//                                e2EDocDTONew.setFileSize(e2eDocDTO.getFileSize());
//                                e2EDocDTONew.setFilePath(filePath);
//                                itemDocDTOS.add(e2EDocDTONew);
//                                setResponseData(itemDocDTOS);
                            } else {
//                                ToastUtils.showToast(MyApplication.getmInstance(), responseData.getMsg());
                            }
                        } else {
//                            ToastUtils.showToast(MyApplication.getmInstance(), "服务器数据异常!");
                        }
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                    }

                    @Override
                    public void uploadProgress(Progress progress) {
                        super.uploadProgress(progress);

                    }
                });
        //此处为HttpMultipartPost方式上传，有BUG NoSuchFileException
           /*     if (!"".equals(E2EAppClientContext.getToken())) {
            HttpMultipartPost httpMultipartPost = new HttpMultipartPost(FileListActivity.this, mHandler, path, url);
            httpMultipartPost.execute("");
        } else {
            ToastUtils.showToast(MyApplication.getmInstance(), "token 为空 请重新登录!");
        }*/
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

    private byte[] nv21_2_jpeg(byte[] nv21, int width, int height) {
        try {
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, bos);
            byte[] jpeg = bos.toByteArray();
            return jpeg;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
                    currentName));
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
