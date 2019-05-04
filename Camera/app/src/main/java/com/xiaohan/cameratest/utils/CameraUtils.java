package com.xiaohan.cameratest.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.xiaohan.cameratest.view.FaceRectView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


public class CameraUtils implements Camera.PreviewCallback {
    private static final String TAG = "CameraUtils";
    private SurfaceView mSurfaceView;
    private FaceRectView mFaceRectView;
    private static CameraUtils mCameraUtils;
    private SurfaceHolder mSurfaceViewHolder;
    private int cameraPosition = 0; //0 后置    1 前置
    private Camera mCamera;
    private int CAMERA_WIDHT = 640;
    private int CAMERA_HEIGHT = 480;
    private boolean detectFace = false;
    private MediaRecorder mediaRecorder;
    /***录制视频的videoSize*/
    private int height = 480, width = 640;
    /***保存的photo的height ,width*/
    private int heightPhoto = 480, widthPhoto = 640;

    public static CameraUtils getInstance() {
        if (mCameraUtils == null) {
            synchronized (CameraUtils.class) {
                if (mCameraUtils == null) {
                    mCameraUtils = new CameraUtils();
                }
            }
        }
        return mCameraUtils;
    }

    public void initCamera(SurfaceView surfaceView, FaceRectView faceRectView) {
        this.mSurfaceView = surfaceView;
        this.mFaceRectView = faceRectView;
        mSurfaceViewHolder = mSurfaceView.getHolder();
        mSurfaceViewHolder.setFormat(PixelFormat.OPAQUE);
    }

    public void startCamera() {
        if (mSurfaceViewHolder != null) {
            mSurfaceViewHolder.addCallback(new SurfaceHolderCB());
        }
    }

    public void stopCamera() {
        stopPreview();
    }

    private void startPreview() {
        if (mCamera == null) {
            mCamera = Camera.open(cameraPosition);
            Camera.Parameters parameters = setParameters(mCamera, cameraPosition);
            mCamera.setDisplayOrientation(90);

            try {
                if (cameraPosition == 0) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); //后置必须聚焦设置
                }
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(mSurfaceViewHolder);
                if (mCamera != null) {
                    mCamera.setPreviewCallback(this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
            mCamera.cancelAutoFocus();//聚焦
            mCamera.startFaceDetection();
            mCamera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
                @Override
                public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                    if (faces.length > 0) {
                        int score = faces[0].score;
                        Log.i(TAG, "onFaceDetection: score " + score);
                        detectFace = true;
                        mFaceRectView.drawFaceRects(faces, mSurfaceView, cameraPosition);
                    } else {
                        detectFace = false;
                        mFaceRectView.clearRect();
                    }
                }
            });
        }
    }

    /**
     * 切换前后相机
     */
    public void changeCamera(CurrentCameraPositionInterface cameraPositionInterface) {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        if (numberOfCameras >= 2) {
            if (cameraPosition == 0) { //现在为后置，变成为前置
                Camera.getCameraInfo(1, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) { //CAMERA_FACING_FRONT 前置方位  CAMERA_FACING_BACK 后置方位
                    if (mCamera != null) {
                        stopPreview();
                    }
                    cameraPosition = 1;
                    startPreview();
                }
            } else if (cameraPosition == 1) {//前置更改为后置相机
                Camera.getCameraInfo(0, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    if (mCamera != null) {
                        stopPreview();
                    }
                    cameraPosition = 0;
                    startPreview();
                }
            }
        }
        cameraPositionInterface.cameraPosition(cameraPosition);
    }


    private class SurfaceHolderCB implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            startPreview();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stopPreview();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (detectFace) {
            Bitmap bmp = Nv21ToBitmapUtils.getInstance().nv21ToBitmap(data, CAMERA_WIDHT, CAMERA_HEIGHT);
            Matrix matrix = new Matrix();
            if (cameraPosition == 1) {
                matrix.postRotate(270);
//                matrix.postTranslate(-1,0);
            } else {
                matrix.postRotate(90);
            }
            bmp = bmp.createBitmap(bmp, 0, 0,
                    bmp.getWidth(), bmp.getHeight(), matrix, true);
            mCurrentDetectFaceInterface.detectFace(bmp);
        }
    }

    private void stopPreview() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.setPreviewDisplay(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.stopFaceDetection();
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 开启闪光灯
     */
    public void turnFlash() {
        try {
            if (mCamera == null || mCamera.getParameters() == null
                    || mCamera.getParameters().getSupportedFlashModes() == null) {
                return;
            }
            Camera.Parameters parameters = mCamera.getParameters();
            String mode = parameters.getFlashMode();
            if (Camera.Parameters.FLASH_MODE_OFF.equals(mode)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else if (Camera.Parameters.FLASH_MODE_TORCH.equals(mode)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 调节预览的焦距
     *
     * @param maxValue
     * @param currentValue
     */
    public void changZoom(int maxValue, int currentValue) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            int maxZoom = parameters.getMaxZoom();
            Log.i(TAG, "changZoom: " + maxZoom);
            float setZoom = ((float) maxZoom / maxValue) * currentValue;
            parameters.setZoom((int) setZoom);
            mCamera.setParameters(parameters);
        }
    }

    /**
     * @param path 保存的路径
     * @param name 录像视频名称(包含后缀)
     */
    public void startRecord(String path, String name) {
        if (mCamera == null)
            return;
        //解锁摄像头并将其分配给MediaRecorder
        mCamera.unlock();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(mCamera);

        //指定用于录制的输入源
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        //mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
        //设置配置文件，或者定义输出格式，音频视频编码器，帧速以及输出尺寸
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        // 设置录制的视频编码h263 h264
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
        mediaRecorder.setVideoEncodingBitRate(700 * 1024);
//        设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
        mediaRecorder.setVideoSize(width, height);
//        mediaRecorder.setVideoFrameRate(24);  //容易报错 还有上面的setVideoSize 都是需要底层支持
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        //指定一个输出文件
        mediaRecorder.setOutputFile(path + File.separator + name);
        File file1 = new File(path + File.separator + name);
        if (file1.exists()) {
            file1.delete();
        }
        //预览视频流，在指定了录制源和输出文件后，在prepare前设置
        mediaRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());

        /***不设置时，录制的视频总是倒着，翻屏导致视频上下翻滚*/
        if (cameraPosition == 1) {//前置相机
            mediaRecorder.setOrientationHint(180);
        } else if (cameraPosition == 0) {
            mediaRecorder.setOrientationHint(0);
        }

        try {
            //准备录制
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        mediaRecorder.release();
        mCamera.release();
        mediaRecorder = null;
        SystemClock.sleep(200);
        if (mCamera != null) {
            mCamera = Camera.open();
            mediaRecorder = new MediaRecorder();
            doChange(mSurfaceView.getHolder());
        }

    }

    /**
     * 拍照使用
     *
     * @param photoPath
     * @param photoName
     */
    public void takePicture(String photoPath, String photoName, TakePictureSuccess mTakePictureSuccess) {
        File file = new File(photoPath);
        if (!file.exists()) {
            file.mkdir();
        }
        Camera.ShutterCallback shutter = new Camera.ShutterCallback() {
            @Override
            public void onShutter() {

            }
        };
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(ImageFormat.JPEG);//图片格式 必设置  否则无法获取图片信息
        parameters.set("jpeg-quality", 90);//设置图片的质量
        mCamera.setParameters(parameters);
        mCamera.takePicture(shutter, null, new PictureCallBack(photoPath, photoName, mTakePictureSuccess));
//        mCamera.takePicture(null, null, new PictureCallBack(photoPath, photoName));
    }

    /*** 拍照功能*/
    private class PictureCallBack implements Camera.PictureCallback {
        /*** 照片保存的路径和名称*/
        private String path;
        private String name;
        private TakePictureSuccess mTakePictureSuccess;

        public PictureCallBack(String path, String name, TakePictureSuccess mTakePictureSuccess) {
            this.path = path;
            this.name = name;
            this.mTakePictureSuccess = mTakePictureSuccess;
        }

        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            File file = new File(path, name);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                try {
                    fos.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Matrix matrix = new Matrix();
            if (cameraPosition == 0) {
                matrix.postRotate(90);
            } else {
                matrix.postRotate(270);
                matrix.postTranslate(-1, 0);
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            mTakePictureSuccess.takePictureSuccess(bitmap);
            //拍照结束 继续预览
            camera.startPreview();
        }
    }

    public interface TakePictureSuccess {
        void takePictureSuccess(Bitmap bitmap);
    }

    /**
     * 切换时，重新开启
     *
     * @param holder
     */
    private void doChange(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置相机参数
     *
     * @param camera
     * @param cameraPosition
     * @return
     */
    public Camera.Parameters setParameters(Camera camera, int cameraPosition) {
        Camera.Parameters parameters = null;
        if (camera != null) {
            parameters = camera.getParameters();
            if (cameraPosition == 0) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                parameters.setPreviewFrameRate(25);
            }
            parameters.setPictureFormat(ImageFormat.NV21);
            parameters.setPictureSize(CAMERA_WIDHT, CAMERA_HEIGHT);
            parameters.setPreviewSize(CAMERA_WIDHT, CAMERA_HEIGHT);
        }
        return parameters;
    }

    /***
     * 获取SupportedVideoSizes 控制输出视频width在300到600之间(尽可能小)
     * 获取PictureSize的大小(控制在w：1000-2000之间)
     */
    public void getVideoSize() {
        if (mCamera == null)
            return;
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> videoSize = parameters.getSupportedVideoSizes();
        for (int i = 0; i < videoSize.size(); i++) {
            int width1 = videoSize.get(i).width;
            int height1 = videoSize.get(i).height;
            if (width1 >= 300 && width1 <= 600) {
                if (height1 >= 200 && height1 <= 600) {
                    width = width1;
                    height = height1;
                }
            }
//            Log.d(TAG, "getVideoSize:----w:-- " + videoSize.get(i).width + "---h:--" + videoSize.get(i).height);
            Log.d(TAG, "width " + width + "---height" + height);
        }
        List<Camera.Size> photoSize = parameters.getSupportedPictureSizes();
        for (int i = 0; i < photoSize.size(); i++) {
            int width1 = photoSize.get(i).width;
            int height1 = photoSize.get(i).height;
            if (width1 >= 1000 && width1 <= 2000) {
                if (height1 >= 600 && height1 <= 2000) {
                    widthPhoto = width1;
                    heightPhoto = height1;
                }
            }
        }
        Log.i(TAG, "getVideoSize: " + widthPhoto + "----" + heightPhoto);
    }

    public interface CurrentCameraPositionInterface {
        void cameraPosition(int cameraPosition);
    }

    public void setCurrentDetectFaceInterface(CurrentDetectFaceInterface currentDetectFaceInterface) {
        mCurrentDetectFaceInterface = currentDetectFaceInterface;
    }

    private CurrentDetectFaceInterface mCurrentDetectFaceInterface;

    public interface CurrentDetectFaceInterface {
        void detectFace(Bitmap bitmap);
    }

}
