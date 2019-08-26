package com.dragonforest.app.facedemo;

import android.util.Log;

import com.dragonforest.app.facedemo.model.CompareResult;
import com.dragonforest.app.facedemo.utils.ArcHttpUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testRecognizeHttpUpload() {
        CompareResult result=ArcHttpUtil.getInstance().searchFace("D:\\projects\\eclipse_workspace\\ArcFaceTest\\src\\imgs\\2.jpg",1);
        System.out.println("result:"+result);
    }

    @Test
    public void testRegisterHttpUpload() {
        boolean isRegistered = ArcHttpUtil.getInstance().registerFace("D:\\projects\\eclipse_workspace\\ArcFaceTest\\src\\imgs\\2.jpg", "测试", 1);
        System.out.println("result:"+isRegistered);
    }
}