package com.dragonforest.app.facedemo.model;

/**
 * @author 韩龙林
 * @date 2019/8/8 11:33
 */
public class CompareResult {
    String name;
    float similar;
    int status=0;  // 1 为成功

    public CompareResult(String name, float similar) {
        this.name = name;
        this.similar = similar;
    }

    public CompareResult() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getSimilar() {
        return similar;
    }

    public void setSimilar(float similar) {
        this.similar = similar;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
