package com.xiaohan.cameratest;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Time: 2019/1/28 10:34
 * Author: xiaohan
 * Des: New Class
 * Company: HIKVISION
 */
public class BarChatView extends View {
    private static final String TAG = "BarChatView";
    private Context mContext;
    private Paint mPaintBar;//绘制柱状图的笔
    private Paint mPaintLine;//绘制直线的笔
    private Paint mPaintValueText;//绘制数值文字的笔
    private Paint mPaintText;//绘制文字的笔

    private int keduTextSpace = 10;//刻度与文字之间的间距
    /**
     * 坐标轴上横向标识线宽度
     **/
    private int keduWidth = 20;
    /**
     * 每个刻度之间的间距 px
     */
    private int keduSpace = 60;
    /**
     * 柱状条之间的间距
     */
    private int itemSpace = 40;
    /**
     * 柱状条的宽度
     */
    private int itemWidth = 20;

    /**
     * 绘制柱形图的X坐标起点
     */
    private int startX = 107;
    /**
     * 绘制柱形图的Y坐标起点
     */
    private int startY = 520;
    /**
     * 数值的最大值
     */
    private int maxValue = 0;
    private int mTextSize = 25;
    private int mMaxTextWidth;
    private int mMaxTextHeight;
    private Rect mXMaxTextRect;
    private Rect mYMaxTextRect;

    /**
     * 绘制刻度递增的值
     */
    private int valueSpace = 100;
    /**
     * 实际刻度递增的值
     */
    private int valueNum = 100;

    /**
     * Y轴的绘制长度
     */
    private int YvalueLength = 500;

    //是否要展示柱状条对应的值
    private boolean isShowValueText = true;
    //数据值
    private List<Integer> mData = new ArrayList<>();
    private List<Integer> yAxisList = new ArrayList<>();
    private List<String> xAxisList = new ArrayList<>();

    public BarChatView(Context context) {
        this(context, null);
    }

    public BarChatView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarChatView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
//        init(context,true);
    }

    private void init(Context context, boolean isUpdate) {
        if (isUpdate) {
//            initData();
        }
        //绘制柱形图的笔
        BlurMaskFilter blurMaskFilter = new BlurMaskFilter(1, BlurMaskFilter.Blur.INNER);
        mPaintBar = new Paint();
        mPaintBar.setStyle(Paint.Style.FILL);
        mPaintBar.setColor(Color.YELLOW);
        mPaintBar.setStrokeWidth(1);
        mPaintBar.setMaskFilter(blurMaskFilter);

        //绘制背景直线的笔
        mPaintLine = new Paint();
        mPaintLine.setStyle(Paint.Style.FILL);
//        mPaintLine.setColor(mContext.getResources().getColor(R.color.trafficCountTextview));
        mPaintLine.setStrokeWidth(1);
        mPaintLine.setAntiAlias(true);

        //绘制文字的笔
        mPaintText = new Paint();
//        mPaintText.setColor(mContext.getResources().getColor(R.color.trafficCountTextview));
        mPaintText.setStrokeWidth(1);
        mPaintText.setAntiAlias(true);

        //绘制刻度文字的笔
        mPaintValueText = new Paint();
        mPaintValueText.setColor(Color.WHITE);
        mPaintValueText.setStrokeWidth(1);
        mPaintValueText.setAntiAlias(true);

        mYMaxTextRect = new Rect();
        mXMaxTextRect = new Rect();
        mPaintText.getTextBounds(Integer.toString(yAxisList.get(yAxisList.size() - 1)), 0, Integer.toString(yAxisList.get(yAxisList.size() - 1)).length(), mYMaxTextRect);
        mPaintText.getTextBounds(xAxisList.get(xAxisList.size() - 1), 0, xAxisList.get(xAxisList.size() - 1).length(), mXMaxTextRect);
        //绘制的刻度文字的最大值所占的宽高
        mMaxTextWidth = mYMaxTextRect.width() > mXMaxTextRect.width() ? mYMaxTextRect.width() : mXMaxTextRect.width();
        mMaxTextHeight = mYMaxTextRect.height() > mXMaxTextRect.height() ? mYMaxTextRect.height() : mXMaxTextRect.height();
    }

    /**
     * 初始化示例数据
     */
   /* private void initData() {
        int[] data = {200, 160, 300, 400, 500,120,500};
        for (int i = 0; i <data.length; i++) {
            mData.add(data[i]);
            yAxisList.add(0 + i * valueSpace);
        }
        Arrays.sort(data);
        maxValue = data[data.length - 1];

        String[] xAxis = {"一", "二", "三", "四", "五", "六", "七"};
        for (int i = 0; i < mData.size(); i++) {
            xAxisList.add(xAxis[i]);
        }
    }*/
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode == MeasureSpec.AT_MOST) {
            if (keduWidth > mMaxTextHeight + keduTextSpace) {
                heightSize = (yAxisList.size() - 1) * keduSpace + keduWidth + mMaxTextHeight;
            } else {
                heightSize = (yAxisList.size() - 1) * keduSpace + (mMaxTextHeight + keduTextSpace) + mMaxTextHeight;
            }
            heightSize = heightSize + keduTextSpace + (isShowValueText ? keduTextSpace : 0);//x轴刻度对应的文字距离底部的padding:keduTextSpace
        }
        if (widthMode == MeasureSpec.AT_MOST) {
            widthSize = startX + mData.size() * itemWidth + (mData.size() + 1) * itemSpace;
        }
        //保存测量结果
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mPaintLine == null)
            return;
        //绘制一条竖线 Y 轴
        canvas.drawLine(startX, startY, startX, startY - YvalueLength, mPaintLine);
        for (int i = 0; i < YvalueLength / valueSpace ; i++) {
            Log.i(TAG, "onDraw: "+i);
            //绘制Y轴的文字
            Rect textRect = new Rect();
            //Y轴上方横向的值
            canvas.drawText(Integer.toString(i * valueNum), (startX - keduWidth) - textRect.width() - keduTextSpace, startY - (i + 1) * valueSpace + valueSpace, mPaintText);
            //画每条Y轴刻度线
            canvas.drawLine(startX, startY - valueSpace * i, startX + mData.size() * itemWidth + itemSpace * (mData.size() + 1), startY - valueSpace * i, mPaintLine);
        }

        for (int j = 0; j < xAxisList.size(); j++) {
            //绘制X轴的文字
            Rect rect = new Rect();
            mPaintText.getTextBounds(xAxisList.get(j), 0, xAxisList.get(j).length(), rect);
            canvas.drawText(xAxisList.get(j), startX + itemSpace * (j + 1) + itemWidth * j + itemWidth / 2 - rect.width() / 2, startY + rect.height() + keduTextSpace, mPaintText);

            int initx = startX + itemSpace * (j + 1) + j * itemWidth;
            float initY = (float) (startY - (mData.get(j) * (valueSpace * 1.0 / valueNum)));

            if (isShowValueText) {
                //绘制柱状条上的值
                Rect rectText = new Rect();
                mPaintValueText.getTextBounds(mData.get(j) + "", 0, (mData.get(j) + "").length(), rectText);
                canvas.drawText(mData.get(j) + "", initx + itemWidth / 2 - rectText.width() / 2, initY-rectText.height()/2, mPaintValueText);
            }
            //绘制柱状条
            canvas.drawRect(initx,initY , initx + itemWidth, startY, mPaintBar);
        }
    }

    public void updateValueData(List<Integer> datas, List<String> xList, int itemSpace, int maxValue) {
        this.mData = datas;
        this.xAxisList = xList;
        for (int i = 0; i < xAxisList.size(); i++) {
            yAxisList.add(0 + i * valueSpace);
        }
        this.itemSpace = itemSpace;
        this.maxValue = maxValue;
        valueSpace = 45;
        if ( maxValue < 100) {
            valueNum = 10;
        } else if ( maxValue < 500) {
            valueNum = 50;
        } else if(maxValue<1000) {
            valueNum = 100;
        }else if(maxValue<1500) {
            valueNum = 150;
        }else if(maxValue<2000) {
            valueNum = 200;
        }else if(maxValue<2500) {
            valueNum = 250;
        }else if(maxValue<3000) {
            valueNum = 300;
        }else if(maxValue<3500) {
            valueNum = 350;
        }
        init(mContext, false);
        invalidate();
    }
}
