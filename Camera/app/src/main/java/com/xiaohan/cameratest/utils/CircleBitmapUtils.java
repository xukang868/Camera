package com.xiaohan.cameratest.utils;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;

/**
 * Time: 2019/2/26 21:05
 * Author: xiaohan
 * Des: New Class
 * Company: HIKVISION
 */
public class CircleBitmapUtils {
    public static Bitmap circleBitmap(FaceDetector.Face[] faces, Bitmap bitmapDetect) {
        Bitmap bitmap = null;
        Rect faceRect = new Rect();
        for (int i = 0; i < faces.length; i++) {
            FaceDetector.Face face = faces[i];
            if (face != null) {
                float eyesDistance = face.eyesDistance();
                PointF pointF = new PointF();
                face.getMidPoint(pointF);
                int left = Math.max((int) (pointF.x - 1.5 * eyesDistance), 0);
                int top = Math.max((int) (pointF.y - 2 * eyesDistance), 0);
                int right = Math.min((int) (pointF.x + 1.5 * eyesDistance), bitmapDetect.getWidth());
                int bottom = Math.min((int) (pointF.y + 2 * eyesDistance), bitmapDetect.getHeight());
                faceRect.set(left, top, right, bottom);
                bitmap = clipFaceRect(bitmapDetect, faceRect);
            }
        }
        return bitmap;
    }
    private static Bitmap clipFaceRect(Bitmap bitmap, Rect faceRect) {
        if (faceRect == null || bitmap == null) return null;
        return Bitmap.createBitmap(bitmap, faceRect.left, faceRect.top, faceRect.width(), faceRect.height());
    }
}
