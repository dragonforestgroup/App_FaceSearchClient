package com.dragonforest.app.facedemo.httpmock;

import com.arcsoft.face.FaceFeature;

/**
 * @author 韩龙林
 * @date 2019/8/6 10:59
 */
public class PersonInfo {
    FaceFeature faceFeature;
    String name;
    int groupId;
    String sex;

    public FaceFeature getFaceFeature() {
        return faceFeature;
    }

    public void setFaceFeature(FaceFeature faceFeature) {
        this.faceFeature = faceFeature;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
}
