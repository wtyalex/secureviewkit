package com.wty.secureviewkit.widgets.wave;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author lifawei
 * @createTime 2023/6/9 11:02
 * @describe
 */
public class WaveProgressBar extends View {
    private int mRingColor = Color.parseColor("#21E782");
    private int mProgressBackgroundColor = Color.parseColor("#D8ECF8");
    private int mProgressColor = Color.parseColor("#30E88B");
    private int mRingWidth = 6;
    private int mInsideWidth = 10;
    private final Paint mRingPaint;
    private final Paint mProgressBackgroundPaint;
    private final Paint mProgressPaint;

    private int mRingRadius;
    private int mRingCx;
    private int mRingCy;
    private int mProgressRadius;
    private int mProgress = 0;
    private float mProgressCopy = 0;
    private int mMax = 100;
    /**
     * 波浪A的偏移
     */
    private int mOffsetA;
    /**
     * 波浪B的偏移
     */
    private int mOffsetB;
    /**
     * 波浪A的振幅
     */
    private int mWaveHeightA;
    /**
     * 波浪B的振幅
     */
    private int mWaveHeightB;
    /**
     * 波浪A的周期
     */
    private float mWaveACycle;
    /**
     * 波浪B的周期
     */
    private float mWaveBCycle;
    private Bitmap mBitmap;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            mOffsetA -= 2;
            mOffsetB -= 4;
            if (mProgressCopy < mProgress) {
                mProgressCopy += 0.5;
            }
            mProgressCopy = Math.min(mProgressCopy, mProgress);
            invalidate();
            if (!hasMessages(2)) {
                sendEmptyMessageDelayed(2, 30);
            }
        }
    };

    public WaveProgressBar(Context context) {
        this(context, null);
    }

    public WaveProgressBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WaveProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mRingPaint = new Paint();
        mRingPaint.setColor(mRingColor);
        mRingPaint.setStyle(Paint.Style.STROKE);// 模式
        mRingPaint.setAntiAlias(true);// 抗锯齿
        mRingPaint.setStrokeWidth(mRingWidth);// 笔宽

        mProgressBackgroundPaint = new Paint();
        mProgressBackgroundPaint.setColor(mProgressBackgroundColor);
        mProgressBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);// 模式
        mProgressBackgroundPaint.setAntiAlias(true);// 抗锯齿

        mProgressPaint = new Paint();
        mProgressPaint.setColor(mProgressColor);
        mProgressPaint.setFilterBitmap(true);
        mProgressPaint.setStyle(Paint.Style.FILL_AND_STROKE);// 模式
        mProgressPaint.setAntiAlias(true);// 抗锯齿
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnim();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeMessages(2);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawCircle(mRingCx, mRingCy, mRingRadius, mRingPaint);
        canvas.drawColor(Color.TRANSPARENT);
        int sc = canvas.saveLayer(mRingCx - mProgressRadius, mRingCy - mProgressRadius, mRingCx + mProgressRadius,
            mRingCy + mProgressRadius, null);
        for (int i = (int)(mRingCx - mProgressRadius); i <= (int)(mRingCx + mProgressRadius); i++) {
            mProgressPaint.setAlpha(100);
            canvas.drawLine(i, (int)getWaveY(i, mOffsetA, mWaveHeightA, mWaveACycle), i, mRingCy + mProgressRadius,
                mProgressPaint);
            mProgressPaint.setAlpha(255);
            canvas.drawLine(i, (int)getWaveY(i, mOffsetB, mWaveHeightB, mWaveBCycle), i, mRingCy + mProgressRadius,
                mProgressPaint);
        }
        // 设置遮罩效果，绘制遮罩
        mProgressPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        canvas.drawBitmap(mBitmap, mRingCx - mProgressRadius, mRingCx - mProgressRadius, mProgressPaint);
        mProgressPaint.setXfermode(null);
        canvas.restoreToCount(sc);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mRingCx = (int)((getWidth() - getPaddingStart() - getPaddingEnd()) * 1f / 2 + getPaddingStart());
        mRingCy = (int)((getHeight() - getPaddingTop() - getPaddingBottom()) * 1f / 2 + getPaddingTop());
        mRingRadius = (int)(Math.min(getWidth() - getPaddingLeft() - getPaddingRight(),
            getHeight() - getPaddingTop() - getPaddingBottom()) * 1f / 2 - mRingWidth);
        mProgressRadius = mRingRadius - mInsideWidth;
        mBitmap = Bitmap.createBitmap((int)mProgressRadius * 2, (int)mProgressRadius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);
        canvas.drawCircle(mProgressRadius, mProgressRadius, mProgressRadius, mProgressBackgroundPaint);
        mWaveHeightA = 20;
        mWaveHeightB = 20;
        if (h / 10 < mWaveHeightA) {
            mWaveHeightA = h / 10;
            mWaveHeightB = h / 20;
        }
        mWaveACycle = (float)(Math.PI / mProgressRadius);
        mWaveBCycle = (float)(Math.PI / mProgressRadius);
        mOffsetA = 10000000;
        mOffsetB = 10000000;
    }

    public void setMax(int max) {
        mMax = Math.max(0, Math.min(0, max));
        startAnim();
    }

    public void setProgress(int progress, boolean anim) {
        mProgress = Math.max(Math.min(progress, mMax), 0);
        mProgressCopy = anim ? 0 : mProgress;
        startAnim();
    }

    private void startAnim() {
        if (mProgress > 0 && mProgress < mMax) {
            if (!mHandler.hasMessages(2)) {
                mHandler.sendEmptyMessage(2);
            }
        } else {
            invalidate();
        }
    }

    public int getProgress() {
        return mProgress;
    }

    public int getMax() {
        return mMax;
    }

    /**
     * 波浪的函数，用于求y值 函数为a*sin(b*(x + c))+d
     * 
     * @param x x轴
     * @param offset 偏移
     * @param waveHeight 振幅
     * @param waveCycle 周期
     * @return
     */
    private double getWaveY(int x, int offset, int waveHeight, float waveCycle) {
        return mMax == 0 ? 0 : waveHeight * Math.sin(waveCycle * (x + offset))
            + (1 - mProgressCopy / mMax) * mProgressRadius * 2 + getPaddingTop() + mRingWidth + mInsideWidth;
    }
}
