package com.xiaohan.cameratest;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaohan.cameratest.utils.CameraUtils;
import com.xiaohan.cameratest.utils.FaceDetectorUtils;
import com.xiaohan.cameratest.view.FaceRectView;
import com.xiaohan.cameratest.view.PictureChosePopupWindow;

import java.io.File;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private SurfaceView mShowPreview;
    private FaceRectView mFaceRectView;
    private Button mTurnCamera, mOpenFlash, mTakePicture, mVideo;
    private TextView mRadioTime;
    private boolean openFlash = false;
    private boolean videoing = false;
    private ImageView detectFace;
    private SeekBar changeZoom;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("mm:ss");
    private int startTime = 0;
    private final int UPDATE = 100;

    private PictureChosePopupWindow mPictureChosePopupWindow;
    private static final int IMAGE_SWITCH_PICTURE = 222;
    private static final int IMAGE_SWITCH_LOCAL_CAMERA = 666;
    private static final int IMAGE_SWITCH_SYSTEM_CAMERA = 888;
    String basePath;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE:
                    startTime++;
                    mRadioTime.setText(String.format(getString(R.string.rediotime), mSimpleDateFormat.format(startTime * 1000)));
                    break;
            }
            return false;
        }
    });
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(mRunnable, 1000);
            mHandler.sendEmptyMessage(UPDATE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        checkPermission();
        initData();
    }

    private void initView() {
        mShowPreview = findViewById(R.id.showPreview);
        mFaceRectView = findViewById(R.id.FaceRect);
        mTurnCamera = findViewById(R.id.turnCamera);
        mOpenFlash = findViewById(R.id.openFlash);
        detectFace = findViewById(R.id.detectFace);
        changeZoom = findViewById(R.id.changeZoom);
        mTakePicture = findViewById(R.id.takePicture);
        mVideo = findViewById(R.id.video);
        mRadioTime = findViewById(R.id.radioTime);

        mTurnCamera.setOnClickListener(this);
        mOpenFlash.setOnClickListener(this);
        mTakePicture.setOnClickListener(this);
        mVideo.setOnClickListener(this);
    }

    private void initData() {
        basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CameraTest/";
        File file = new File(basePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        changeZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                CameraUtils.getInstance().changZoom(100, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        CameraUtils.getInstance().initCamera(mShowPreview, mFaceRectView);
        CameraUtils.getInstance().startCamera();
        CameraUtils.getInstance().setCurrentDetectFaceInterface(new CameraUtils.CurrentDetectFaceInterface() {
            @Override
            public void detectFace(Bitmap bitmap) {
                detectFace.setImageBitmap(FaceDetectorUtils.getInstance().getCutBitmap(bitmap));
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.turnCamera:
                CameraUtils.getInstance().changeCamera(new CameraUtils.CurrentCameraPositionInterface() {
                    @Override
                    public void cameraPosition(int cameraPosition) {
                        if (cameraPosition == 0) {
                            mOpenFlash.setVisibility(View.VISIBLE);
                        } else {
                            mOpenFlash.setVisibility(View.INVISIBLE);
                        }
                    }
                });
                break;
            case R.id.openFlash:
                openFlash = !openFlash;
                if (openFlash) {
                    mOpenFlash.setBackground(getResources().getDrawable(R.mipmap.camera_flash_on));
                } else {
                    mOpenFlash.setBackground(getResources().getDrawable(R.mipmap.camera_flash_off));
                }
                CameraUtils.getInstance().turnFlash();
                break;
            case R.id.takePicture:
                mPictureChosePopupWindow = new PictureChosePopupWindow(MainActivity.this);
                mPictureChosePopupWindow.initPopWindow();
                mPictureChosePopupWindow.setPopItemClickListener(new PictureChosePopupWindow.PopItemClickListener() {
                    @Override
                    public void onClickItem(int RequestCode) {
                        doCapture(RequestCode);
                    }
                });
                mPictureChosePopupWindow.showAtBottom(v);
                break;
            case R.id.video:
                videoing = !videoing;
                if (videoing) {
                    mRadioTime.setVisibility(View.VISIBLE);
                    CameraUtils.getInstance().startRecord(basePath, "xhVideo.mp4");
                    mHandler.postDelayed(mRunnable, 10);
                } else {
                    mHandler.removeCallbacks(mRunnable);
                    startTime = 0;
                    mRadioTime.setVisibility(View.INVISIBLE);
                    CameraUtils.getInstance().stopRecord();
                    Toast.makeText(MainActivity.this, "录像成功,保存至/CameraTest/xhVideo.mp4!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    private void doCapture(int RequestCode) {
        Intent intent = null;
        if (RequestCode == IMAGE_SWITCH_LOCAL_CAMERA) {
            //获取大图的形式
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = new File(basePath + "local.jpg");
            Uri uri = null;
            if (file != null ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {//7.0以下则直接使用Uri的fromFile方法将File转化为Uri
                    uri = Uri.fromFile(file);
                } else {//7.0以上要通过FileProvider将File转化为Uri
                    uri = FileProvider.getUriForFile(this, getPackageName()+".fileprovider", file);
                }
            }
            if (uri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(intent, RequestCode);
            }
            //通过缩率图的形式
            /*Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//用来打开相机的Intent
            if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {//这句作用是如果没有相机则该应用不会闪退，要是不加这句则当系统没有相机应用的时候该应用会闪退
                startActivityForResult(takePhotoIntent, RequestCode);//启动相机
            }*/
        } else if (RequestCode == IMAGE_SWITCH_SYSTEM_CAMERA) {
            CameraUtils.getInstance().takePicture(basePath, "system.jpg", new CameraUtils.TakePictureSuccess() {
                @Override
                public void takePictureSuccess(Bitmap bitmap) {
                    detectFace.setImageBitmap(bitmap);
                }
            });

        } else if (RequestCode == IMAGE_SWITCH_PICTURE) {
            intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, RequestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap;
        String absolutePath;
        if (requestCode == IMAGE_SWITCH_LOCAL_CAMERA) {                      //通过相机获取
           //获取大图的形式
            if (resultCode == Activity.RESULT_OK) {
                File file = new File(basePath + "local.jpg");
                Log.i(TAG, "onActivityResult: " + file.exists());
                try {
                    bitmap = BitmapFactory.decodeFile(basePath + "local.jpg");
                    detectFace.setImageBitmap(bitmap);
                } catch (Exception e) {
                }
            }
            //缩率图的形式
           /* Bundle extras = data.getExtras();
            bitmap = (Bitmap) extras.get("data");
            detectFace.setImageBitmap(bitmap);*/
        } else if (requestCode == IMAGE_SWITCH_PICTURE) {                  //通过照片获取
            if (resultCode == RESULT_OK && null != data) {
                Uri uriData = data.getData();
                Log.i(TAG, "onActivityResult: " + uriData);
                String[] filePathColum = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(uriData, filePathColum, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColum[0]);
                absolutePath = cursor.getString(columnIndex);
                if (!TextUtils.isEmpty(absolutePath)) {
                    bitmap = BitmapFactory.decodeFile(absolutePath);
                    detectFace.setImageBitmap(bitmap);
                }
                cursor.close();
            }
        }
    }

    private void checkPermission() {
        try {
            if (Integer.valueOf(android.os.Build.VERSION.SDK) < 23) {
                return;
            }
        } catch (NumberFormatException e) {
        }

        // 检查权限是否获取（android6.0及以上系统可能默认关闭权限，且没提示）
        PackageManager pm = getPackageManager();
        boolean permission_writeStorage = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", "packageName"));
        boolean permission_caremera = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission("android.permission.CAMERA", "packageName"));

        boolean permission_record = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission("android.permission.RECORD_AUDIO", "packageName"));

        if (!(permission_writeStorage && permission_caremera && permission_record)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
            }, 0x01);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x01) {
            // If request is cancelled, the result arrays are empty.

            if (grantResults != null && (grantResults.length > 0)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                getAppDetailSettingIntent();
                Toast.makeText(getApplicationContext(), "读写/Camera/录屏权限未开启，请开启!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getAppDetailSettingIntent() {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        LogUtils.i("SDK_INT "+Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= 9) {
            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            localIntent.setData(Uri.fromParts("package", getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            localIntent.setAction(Intent.ACTION_VIEW);
            localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            localIntent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
        }
        startActivity(localIntent);
    }
}
