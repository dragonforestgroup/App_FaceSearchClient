package com.dragonforest.app.facedemo.httpmock;

import com.arcsoft.face.FaceFeature;
import com.dragonforest.app.facedemo.arcsoft.faceserver.CompareResult;

/**
 * 模拟sdk
 *
 * @author 韩龙林
 * @date 2019/8/6 13:40
 */
public class ArcFaceSDK {
    private static ArcFaceSDK instance;

    public static ArcFaceSDK getInstance() {
        if(instance==null){
            instance=new ArcFaceSDK();
        }
        return instance;
    }

    /**
     * 注册人信息
     *
     * <p>作用：将人的信息（特征码，姓名，年龄，性别）存入云端<p/>
     *
     * @param personInfo
     * @return 是否成功
     */
    public boolean registerFace(PersonInfo personInfo){

        // http请求....
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 查找相似度最大的人脸 返回相似度
     *
     * <p>作用：从云端遍历，找出相似度最大的一个比对结果信息<p/>
     *
     * @param faceFeature
     * @return CompareResult（包含userName,similar,trackId）
     */
    public CompareResult searchTopSimilarFace(FaceFeature faceFeature){
        CompareResult compareResult=new CompareResult("韩龙林",0.9f);
        // 模拟网络请求
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return compareResult;
    }
}
