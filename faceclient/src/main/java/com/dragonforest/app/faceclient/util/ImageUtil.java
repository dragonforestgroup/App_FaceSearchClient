package com.dragonforest.app.faceclient.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * 图片工具类
 *
 * @author 韩龙林
 * @date 2019/8/19 15:15
 */
public class ImageUtil {

    private static ImageUtil instance;

    public static ImageUtil getInstance() {
        if (instance == null) {
            instance = new ImageUtil();
        }
        return instance;
    }

    /**
     * 从文件中加载图片文件 并加载到内存
     * 压缩到指定的像素
     *
     * @param path
     * @param dstWidth
     * @param dstHeight
     * @return
     */
    public Bitmap compressLoadFromFile(String path, int dstWidth, int dstHeight) {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        // 先进行采样率压缩 避免从文件中读入oom， 但是不能压缩到精确的值
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int outWidth = options.outWidth;
        int outHeight = options.outHeight;
        int sampleSize = calculateSimpleSize(outWidth, outHeight, dstWidth, dstHeight);
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);

        // 在进行双线性采样率压缩，可以压缩到精确的值
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, true);
        return scaledBitmap;
    }

    /**
     * 从文件中加载图片文件 并加载到内存
     * 保持长宽比
     *
     * @param path
     * @param dstWidth
     * @param dstHeight
     * @return
     */
    public Bitmap compressLoadFromFileCompat(String path, int dstWidth, int dstHeight) {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        // 先进行采样率压缩 避免从文件中读入oom， 但是不能压缩到精确的值
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int outWidth = options.outWidth;
        int outHeight = options.outHeight;
        int sampleSize = calculateSimpleSize(outWidth, outHeight, dstWidth, dstHeight);
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);

        return bitmap;
    }

    /**
     * 压缩磁盘图片文件 并保存到图片
     * 压缩到指定像素
     *
     * @param srcPath
     * @param dstPath
     * @param dstHeight
     * @param dstWidth
     * @return
     */
    public boolean compressImageFile(String srcPath, String dstPath, int dstHeight, int dstWidth) {
        Bitmap bitmap = compressLoadFromFile(srcPath, dstWidth, dstHeight);
        if (bitmap == null) {
            return false;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(dstPath));
            fos.write(bos.toByteArray());
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 计算压缩比
     *
     * @param outWidth
     * @param outHeight
     * @param dstWidth
     * @param dstHeight
     * @return
     */
    private int calculateSimpleSize(int outWidth, int outHeight, int dstWidth, int dstHeight) {
        if (dstHeight != 0 && dstWidth != 0) {
            return Math.min(outWidth / dstWidth, outHeight / dstHeight);
        }
        return 1;
    }

    /**
     * nv21（摄像头采集格式） 转化为jpeg格式
     * @param nv21
     * @param width
     * @param height
     * @return
     */
    public byte[] nv21_2_jpeg(byte[] nv21, int width, int height) {
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
}
