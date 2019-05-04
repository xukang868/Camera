package com.xiaohan.cameratest.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.xiaohan.cameratest.R;

public class PictureChosePopupWindow extends PopupWindow implements View.OnClickListener {
    private String TAG = getClass().getSimpleName();
    private PopupWindow mPopupWindow;
    private Context mContext;
    private View mView;
    private PopItemClickListener mPopItemClickListener;
    private static final int IMAGE_SWITCH_PICTURE = 222;
    private static final int IMAGE_SWITCH_LOCAL_CAMERA = 666;
    private static final int IMAGE_SWITCH_SYSTEM_CAMERA = 888;
    private static  int REQUESTCODE = 0;
    public PictureChosePopupWindow(Context context) {
        mContext = context;
    }

    public void initPopWindow(){
        mView = LinearLayout.inflate(mContext, R.layout.popup_picturechose, null);
        mPopupWindow = new PopupWindow(mView,LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT,true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable(mContext.getResources(), (Bitmap )null));

        mPopupWindow.getContentView().setFocusableInTouchMode(true);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setAnimationStyle(R.style.anim_menu_bottombar);

        mView.findViewById(R.id.systemCamera).setOnClickListener(this);
        mView.findViewById(R.id.localCamera).setOnClickListener(this);
        mView.findViewById(R.id.picture).setOnClickListener(this);
        mView.findViewById(R.id.cancle).setOnClickListener(this);
    }

    public void showAtBottom( View view){
        mPopupWindow.showAtLocation(mView, Gravity.BOTTOM, 0, 0);
        mPopupWindow.showAsDropDown(view);
    }

    public void dismiss(){
        mPopupWindow.dismiss();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.systemCamera:
                REQUESTCODE = IMAGE_SWITCH_SYSTEM_CAMERA;
                break;
            case R.id.localCamera:
                REQUESTCODE = IMAGE_SWITCH_LOCAL_CAMERA;
                break;
            case R.id.picture:
                REQUESTCODE = IMAGE_SWITCH_PICTURE;
                break;
            case R.id.cancle:
                break;
        }
        mPopItemClickListener.onClickItem(REQUESTCODE);
        dismiss();
        REQUESTCODE = 0;//重置参数
    }

    public interface PopItemClickListener{
        void onClickItem(int RequestCode);
    }
    public void setPopItemClickListener(PopItemClickListener popItemClickListener){
        mPopItemClickListener = popItemClickListener;
    }
}
