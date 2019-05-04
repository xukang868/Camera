package com.xiaohan.cameratest.utils;

import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import com.xiaohan.cameratest.MyApplication;


public class Nv21ToBitmapUtils {
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private static  Nv21ToBitmapUtils nv21ToBitmapUtils;
    public static Nv21ToBitmapUtils getInstance(){
        if(nv21ToBitmapUtils==null){
            synchronized (Nv21ToBitmapUtils.class){
                if(nv21ToBitmapUtils==null){
                    nv21ToBitmapUtils = new Nv21ToBitmapUtils();
                }
            }
        }
        return nv21ToBitmapUtils;
    }
    public  Bitmap nv21ToBitmap(byte[] nv21, int width, int height){
        rs = RenderScript.create(MyApplication.context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
        in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(nv21);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);
        return bmpout;
    }
}
