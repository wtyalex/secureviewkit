package com.wty.secureviewkit.widgets.drag;

import java.util.Locale;

import com.wty.foundation.common.utils.StringUtils;
import com.wty.secureviewkit.widgets.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author lifawei
 * @createTime 2023/4/6 11:38
 * @describe
 */
public class SeekBarView extends View {
    private final Paint mAxisTextPaint;
    private final Paint mAxisPaint;
    private final Paint mBorderPaint;
    private final Paint mProgressPaint;
    private Paint mCursorPaint;
    private static final String SYMBOL1 = "0";
    private static final String SYMBOL2 = "000分钟";
    private static final String SYMBOL3 = "钟";
    private int mAreaRadius = 12;
    private int mAmAxisWidth = 2;
    private int mXMaxValue;
    private int mCurrentValue;
    private int mOldCurrentValue;
    private int mXAxisMinSpacing = 15;
    private float mXAxisTickMarkHeight = 6;
    private float mPadding = 10;
    private RectF mContentArea;
    private RectF mBorderArea;

    // x轴的下标
    private float mXAxisTickMarkDrawSpace = mXAxisMinSpacing;
    // x轴每分钟所占的像素
    private float mXAxisPxUnit = -1;
    private int mXAxisTickMarkDrawCount;
    // x轴每个下标的间距像素
    private float mXAxisTickMarkDistance;
    private float mLastX;
    private float mLastY;
    private boolean isCanDrag;
    private final GestureDetector mGestureDetector;
    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    private final Path mLeftCursorPath = new Path();
    private final Path mRightCursorPath = new Path();

    public SeekBarView(Context context) {
        this(context, null);
    }

    public SeekBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        mGestureDetector = new GestureDetector(context, new Gesture());
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SeekBarView);
        mAreaRadius = ta.getDimensionPixelSize(R.styleable.SeekBarView_radius, 12);
        int axisTextSize = ta.getDimensionPixelSize(R.styleable.SeekBarView_textSize, 20);
        int axisTextColor = ta.getColor(R.styleable.SeekBarView_textColor, Color.parseColor("#778290"));
        int borderColor = ta.getColor(R.styleable.SeekBarView_borderColor, Color.parseColor("#00CBFF"));
        int progressColor = ta.getColor(R.styleable.SeekBarView_progressColor, borderColor);
        mPadding = ta.getDimensionPixelSize(R.styleable.SeekBarView_borderPadding, 10);
        int axisColor = Color.parseColor("#778290");
        int cursorColor = Color.parseColor("#C83A3A");
        ta.recycle();
        // 轴文字画笔
        mAxisTextPaint = new TextPaint();
        mAxisTextPaint.setTextSize(axisTextSize);
        mAxisTextPaint.setColor(axisTextColor);
        mAxisTextPaint.setSubpixelText(true);
        mAxisTextPaint.setTypeface(Typeface.SANS_SERIF);
        mAxisTextPaint.setTextLocale(Locale.SIMPLIFIED_CHINESE);
        mAxisTextPaint.setHinting(Paint.HINTING_ON);
        // x、y轴画笔
        mAxisPaint = new Paint();
        mAxisPaint.setStrokeWidth(mAmAxisWidth);
        mAxisPaint.setColor(axisColor);

        // 边框
        mBorderPaint = new Paint();
        mBorderPaint.setStrokeWidth(4);
        mBorderPaint.setColor(borderColor);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        // 进度条
        mProgressPaint = new Paint();
        mProgressPaint.setColor(progressColor);
        mProgressPaint.setStyle(Paint.Style.FILL);
        // 指示器
        mCursorPaint = new Paint();
        mCursorPaint.setColor(cursorColor);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            measureArea();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawAxis(canvas);
        drawBorder(canvas);
        drawProgress(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (isCanDrag) {
                float x = event.getX();
                int d = (int)((mLastX - x) * mXMaxValue / mBorderArea.width());
                mCurrentValue = Math.min(Math.max(15, mOldCurrentValue - d), mXMaxValue);
                updateCursor();
                invalidate();
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onProgressChanged(this, mCurrentValue);
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (isCanDrag) {
                isCanDrag = false;
                int surplus = mCurrentValue % 15;
                if (surplus >= mXAxisMinSpacing * 1f / 2) {
                    mCurrentValue = mCurrentValue + (mXAxisMinSpacing - surplus);
                } else {
                    mCurrentValue = mCurrentValue - surplus;
                }
                updateCursor();
                invalidate();
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onProgressChanged(this, mCurrentValue);
                }
            }
            mLastX = 0;
            mLastY = 0;
        }

        return true;
    }

    public void setMaxValue(int max, int progress) {
        mXMaxValue = max;
        mCurrentValue = Math.min(progress, max);
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener onSeekBarChangeListener) {
        this.mOnSeekBarChangeListener = onSeekBarChangeListener;
    }

    private void drawProgress(Canvas canvas) {
        RectF progress = new RectF(mContentArea);
        progress.right = progress.left + mCurrentValue * (mContentArea.width()) * 1.0f / mXMaxValue;
        canvas.drawRoundRect(progress, mAreaRadius, mAreaRadius, mProgressPaint);
        if (mCurrentValue == mXAxisMinSpacing) {
            canvas.drawPath(mRightCursorPath, mCursorPaint);
        } else if (mCurrentValue == mXMaxValue) {
            canvas.drawPath(mLeftCursorPath, mCursorPaint);
        } else {
            canvas.drawPath(mRightCursorPath, mCursorPaint);
            canvas.drawPath(mLeftCursorPath, mCursorPaint);
        }

    }

    private void drawBorder(Canvas canvas) {
        canvas.drawRoundRect(mBorderArea, mAreaRadius, mAreaRadius, mBorderPaint);
    }

    private void drawAxis(Canvas canvas) {
        if (mContentArea == null) {
            return;
        }
        // 底x轴
        canvas.drawLine(mContentArea.left, mBorderArea.bottom + 6, mContentArea.right, mBorderArea.bottom + 6,
            mAxisPaint);
        for (int i = 0; i < mXAxisTickMarkDrawCount + 1; i++) {
            canvas.drawLine(mXAxisTickMarkDistance * i + mContentArea.left, mBorderArea.bottom + 5,
                mXAxisTickMarkDistance * i + mContentArea.left, mBorderArea.bottom + 6 + mXAxisTickMarkHeight,
                mAxisPaint);
        }
        drawXAxisText(canvas);
    }

    private void drawXAxisText(Canvas canvas) {
        Paint.FontMetrics fm = getAxisTextFontMetrics();
        for (int i = 0; i < mXAxisTickMarkDrawCount + 1; i++) {
            String text = (int)(i * mXAxisTickMarkDrawSpace) + "分钟";
            Rect rect = getAxisTextRect(text);
            if (i == 0) {
                canvas.drawText(text, 0, getHeight() - fm.bottom, mAxisTextPaint);
            } else if (i == mXAxisTickMarkDrawCount) {
                canvas.drawText(text, getWidth() - rect.width(), getHeight() - fm.bottom, mAxisTextPaint);
            } else {
                canvas.drawText(text, mContentArea.left + i * mXAxisTickMarkDistance - rect.width() / 2,
                    getHeight() - fm.bottom, mAxisTextPaint);
            }
        }
    }

    private void measureArea() {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        Paint.FontMetrics fontMetrics = getAxisTextFontMetrics();
        Rect textRect = getAxisTextRect(SYMBOL1);
        Rect textRect3 = getAxisTextRect(SYMBOL3);
        float yAxisLeft = getPaddingLeft() + textRect.width() + 4;
        float yAxisTop = getPaddingTop() + 4;
        float yAxisRight = getWidth() - getPaddingRight() - textRect3.width() - 6;
        float yAxisBottom = getHeight() - getPaddingBottom() - (int)(fontMetrics.bottom - fontMetrics.top)
            - mXAxisTickMarkHeight - mAmAxisWidth - 2;
        mBorderArea = new RectF(yAxisLeft, yAxisTop, yAxisRight, yAxisBottom);
        mContentArea = new RectF(mBorderArea.left + mPadding, mBorderArea.top + mPadding, mBorderArea.right - mPadding,
            mBorderArea.bottom - mPadding);
        updateCursor();
        allocationXAxis();

    }

    private void updateCursor() {
        if (mContentArea == null) {
            return;
        }
        mLeftCursorPath.reset();
        mLeftCursorPath.setFillType(Path.FillType.EVEN_ODD);
        float relyX = mContentArea.left + mCurrentValue * mContentArea.width() / mXMaxValue;
        float relyY = (mContentArea.top + mContentArea.bottom) / 2;
        mLeftCursorPath.moveTo(relyX - 17, relyY);
        mLeftCursorPath.lineTo(relyX - 2, relyY - 15);
        mLeftCursorPath.lineTo(relyX - 2, relyY + 15);
        mLeftCursorPath.close();

        mRightCursorPath.reset();
        mRightCursorPath.setFillType(Path.FillType.EVEN_ODD);
        mRightCursorPath.moveTo(relyX + 2, relyY - 15);
        mRightCursorPath.lineTo(relyX + 2, relyY + 15);
        mRightCursorPath.lineTo(relyX + 17, relyY);
        mRightCursorPath.close();
    }

    /**
     * 分配x轴，计算x轴画几个坐标
     */
    private void allocationXAxis() {
        if (mContentArea == null || mXMaxValue <= 0) {
            return;
        }

        Rect rect = getAxisTextRect(SYMBOL2);
        int maxCount = (getWidth() - getPaddingLeft() - getPaddingRight()) / rect.width() - 4;
        maxCount = Math.max(maxCount, 2);
        int space = mXAxisMinSpacing;
        int count = mXMaxValue / space + (mXMaxValue % space == 0 ? 0 : 1);
        if (count > maxCount) {
            mXAxisTickMarkDrawSpace = mXMaxValue * 1f / maxCount;
            mXAxisTickMarkDrawCount = maxCount;
        } else {
            mXAxisTickMarkDrawSpace = space;
            mXAxisTickMarkDrawCount = count;
        }
        mXAxisTickMarkDistance = (mContentArea.width()) * 1f / mXAxisTickMarkDrawCount;
        mXAxisPxUnit = (mContentArea.width()) * 1f / mXMaxValue;
    }

    private Paint.FontMetrics getAxisTextFontMetrics() {
        return mAxisTextPaint.getFontMetrics();
    }

    private Rect getAxisTextRect(String str) {
        Rect rect = new Rect();
        if (!StringUtils.isNull(str)) {
            mAxisTextPaint.getTextBounds(str, 0, str.length(), rect);
        }
        return rect;
    }

    public interface OnSeekBarChangeListener {
        void onProgressChanged(SeekBarView seekBar, int progress);
    }

    class Gesture extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            mLastX = e.getX();
            mLastY = e.getY();
            mOldCurrentValue = mCurrentValue;
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            isCanDrag = mBorderArea.contains(mLastX, mLastY);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            isCanDrag = mBorderArea.contains(mLastX, mLastY);
            return true;
        }
    }
}
