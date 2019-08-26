package com.dragonforest.app.facedemo.utils;

import android.util.Log;

import com.dragonforest.app.facedemo.model.CompareResult;

import org.json.JSONObject;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author 韩龙林
 * @date 2019/8/8 09:58
 */
public class ArcHttpUtil {

    private static ArcHttpUtil instance;

    private ArcHttpUtil() {
    }

    public static ArcHttpUtil getInstance() {
        if (instance == null) {
            instance = new ArcHttpUtil();
        }
        return instance;
    }

    private String ip = "127.0.0.1";
    private String port = "8080";
    private String baseUrl = "http://" + ip + ":" + port;

    public void init(String ip, String port) {
        this.ip = ip;
        this.port = port;
        this.baseUrl = "http://" + ip + ":" + port;
    }

    /**
     * 上传图片 返回相似度
     *
     * @param path
     * @param flag
     * @return
     */
    public CompareResult searchFaceByFile(String path, int flag) {
        CompareResult result = new CompareResult();

        String url = baseUrl + "/compare";
        File file = new File(path);
        if (!file.exists()) {
            Log.e("TAG", "文件不存在，无法上传");
            result.setStatus(-100);
            return result;
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("flag", "1")
                .addFormDataPart("bytes", file.getName(), RequestBody.create(MediaType.parse("mutipart/form-data"), file))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                result.setStatus(1);
                String responseJson = response.body().string();
                JSONObject jsonObject = new JSONObject(responseJson);
                String name = jsonObject.getString("name");
                float similar = (float) jsonObject.getDouble("similar");
                result.setName(name);
                result.setSimilar(similar);
                return result;
            } else {
                Log.e("TAG", "返回失败");
                result.setStatus(0);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", "出现异常");
            result.setStatus(-1000);
            return null;
        }
    }

    /**
     * 上传特征 返回相似度
     *
     * @param feature
     * @param flag
     * @return
     */
    public CompareResult searchFaceByFeature(byte[] feature, int flag) {
        CompareResult result = new CompareResult();

        String url = baseUrl + "/compare";
        if (feature==null) {
            Log.e("TAG","特征为空，不能上传");
            return result;
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("flag", "2")
                .addFormDataPart("bytes", "", RequestBody.create(MediaType.parse("mutipart/form-data"), feature))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                result.setStatus(1);
                String responseJson = response.body().string();
                JSONObject jsonObject = new JSONObject(responseJson);
                String name = jsonObject.getString("name");
                float similar = (float) jsonObject.getDouble("similar");
                result.setName(name);
                result.setSimilar(similar);
                return result;
            } else {
                Log.e("TAG", "返回失败");
                result.setStatus(0);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", "出现异常");
            result.setStatus(-1000);
            return null;
        }
    }

    public boolean registerFaceByFile(String path, String name,int groupId,int featureType, int flag) {
        Boolean isRegisted = false;

        String url = baseUrl + "/register";
        File file = new File(path);
        if (!file.exists()) {
            Log.e("TAG", "文件不存在，无法上传");
            return false;
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("name", name)
                .addFormDataPart("flag", "1")
                .addFormDataPart("bytes", file.getName(), RequestBody.create(MediaType.parse("mutipart/form-data"), file))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseJson = response.body().string();
                if ("1".equals(responseJson)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                Log.e("TAG", "返回失败");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", "出现异常");
            return false;
        }
    }

    public boolean registerFaceByFeature(byte[] feature, String name,int groupId,int featureType, int flag) {
        Boolean isRegisted = false;

        String url = baseUrl + "/register";
        if (feature==null) {
            Log.e("TAG", "特征码为空，无法上传");
            return false;
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("name", name)
                .addFormDataPart("flag", "2")
                .addFormDataPart("groupId",groupId+"")
                .addFormDataPart("featureType",1+"")
                .addFormDataPart("bytes", "feature", RequestBody.create(MediaType.parse("mutipart/form-data"), feature))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseJson = response.body().string();
                if ("1".equals(responseJson)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                Log.e("TAG", "返回失败");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", "出现异常");
            return false;
        }
    }
}
