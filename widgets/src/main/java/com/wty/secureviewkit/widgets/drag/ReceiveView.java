package com.wty.secureviewkit.widgets.drag;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.wty.foundation.common.utils.ArrayUtils;
import com.wty.foundation.common.utils.MathUtils;
import com.wty.foundation.common.utils.StringUtils;
import com.wty.secureviewkit.widgets.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * @author lifawei
 * @createTime 2023/1/18 14:05
 * @describe
 */
public class ReceiveView extends View {
    private static final String TAG = "ReceiveView";
    private static final String SYMBOL1 = "00:00";
    private static final String SYMBOL2 = "00:";
    // 内容区域
    private RectF mContentArea;
    private final RectF mCursorRectF = new RectF();
    private final Paint mPaint;
    private final Paint mBorderPaint;
    private final Paint mMovingAreaPaint;
    private final Paint mAxisTextPaint;
    private final Paint mAxisPaint;
    private final Paint mXAxisTickMarkPaint;
    private final Paint mMaxYPaint;
    private final Paint mConflictPaint;
    private final Paint mCursorLinePaint;

    private int mAreaColor;
    private int mSelectedAreaColor;
    private int mBorderColor;

    private final int mAxisWidth;
    private final int mXAxisTickMarkHeight;
    private int mAreaRadius;
    // 需求量占的容器高度
    private float mMaxYHeight;
    // 需求量
    private float mMaxYValue;
    // 目标量
    private float mFittedYValue;
    // x轴结束值
    private int mMaxXValue;
    // x轴开始值
    private int mMinXValue;
    // x轴的最小分割单位量
    private final int mXAxisMinSpacing = 15;
    // x轴的下标
    private float mXAxisTickMarkDrawSpace = mXAxisMinSpacing;
    // x轴每分钟所占的像素
    private float mXAxisPxUnit = -1;
    private int mXAxisTickMarkDrawCount;
    // x轴每个下标的间距像素
    private float mXAxisTickMarkDistance;

    // y轴每单位占用的像素量
    private float mYAxisPxUnit = -1;
    // 每个时间段中的数据
    private ArrayList<DragData>[] mTimeBlock;
    // 所有的数据
    private final ArrayList<DragData> mDataBlock = new ArrayList<>();
    // 有冲突的数据
    private final ArrayList<DragData> mConflictData = new ArrayList<>();
    // 容器中每个区域所对应的数据
    private final HashMap<RectF, DragData> mDataByRectF = new HashMap<>();

    private final ValueFormatter mValueFormatter = new ValueFormatter();
    private ReceiveViewListener mReceiveViewListener;
    private final GestureDetector mGestureDetector;
    // 触摸拖拽
    private boolean isCanMove;
    private boolean isDragAble;
    private boolean isTouchCursor;
    private RectF mMoveRectF;
    private final RectF mMoveRectFCopy = new RectF();
    private RectF mSelectedRectF;
    private DragData mMoveDragData;
    private float mLastX;
    private float mLastY;
    private final Path mCursorPath = new Path();

    public ReceiveView(Context context) {
        this(context, null);
    }

    public ReceiveView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReceiveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ReceiveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setWillNotDraw(false);
        mGestureDetector = new GestureDetector(context, new Gesture());
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ReceiveView);
        mAreaColor = ta.getColor(R.styleable.ReceiveView_areaColor, Color.BLUE);
        mSelectedAreaColor = ta.getColor(R.styleable.ReceiveView_selectedAreaColor, Color.parseColor("#006AFF"));
        mBorderColor = ta.getColor(R.styleable.ReceiveView_borderColor, Color.parseColor("#135361"));

        int axisColor = ta.getColor(R.styleable.ReceiveView_axisColor, Color.parseColor("#778290"));
        int warningColor = ta.getColor(R.styleable.ReceiveView_warningColor, Color.parseColor("#C83A3A"));
        int conflictColor = ta.getColor(R.styleable.ReceiveView_conflictColor, Color.parseColor("#FF423A"));
        int xAxisTickMarkColor = ta.getColor(R.styleable.ReceiveView_xAxisTickMarkColor, Color.parseColor("#979797"));
        int axisTextColor = ta.getColor(R.styleable.ReceiveView_xAxisTextColor, Color.parseColor("#778290"));

        int axisTextSize = ta.getDimensionPixelSize(R.styleable.ReceiveView_xAxisTextSize, 24);
        int warningWidth = ta.getDimensionPixelSize(R.styleable.ReceiveView_warningWidth, 2);
        mAreaRadius = ta.getDimensionPixelSize(R.styleable.ReceiveView_areaRadius, 0);
        mAxisWidth = ta.getDimensionPixelSize(R.styleable.ReceiveView_axisWidth, 2);
        mXAxisTickMarkHeight = ta.getDimensionPixelSize(R.styleable.ReceiveView_xAxisTickMarkHeight, 12);
        isDragAble = ta.getBoolean(R.styleable.ReceiveView_isDragAble, true);
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
        mAxisPaint.setStrokeWidth(mAxisWidth);
        mAxisPaint.setColor(axisColor);
        // x轴的下标分割线画笔
        mXAxisTickMarkPaint = new Paint();
        mXAxisTickMarkPaint.setStrokeWidth(mAxisWidth);
        mXAxisTickMarkPaint.setColor(xAxisTickMarkColor);
        // 警戒线的画笔
        mMaxYPaint = new Paint();
        mMaxYPaint.setStrokeWidth(warningWidth);
        mMaxYPaint.setColor(warningColor);
        mMaxYPaint.setStyle(Paint.Style.STROKE);
        mMaxYPaint.setPathEffect(new DashPathEffect(new float[] {5, 5}, 0));

        // 子区域的画笔
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mAreaColor);
        // 子区域的边框画笔
        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStrokeWidth(1);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(mBorderColor);
        // 冲突子区域的画笔
        mConflictPaint = new Paint();
        mConflictPaint.setAntiAlias(true);
        mConflictPaint.setStyle(Paint.Style.FILL);
        mConflictPaint.setColor(conflictColor);
        // 选中
        mMovingAreaPaint = new Paint();
        mMovingAreaPaint.setAntiAlias(true);
        mMovingAreaPaint.setStrokeWidth(warningWidth);
        mMovingAreaPaint.setStyle(Paint.Style.FILL);
        mMovingAreaPaint.setColor(Color.parseColor("#135361"));
        // 警戒线的画笔
        mCursorLinePaint = new Paint();
        mCursorLinePaint.setStrokeWidth(warningWidth);
        mCursorLinePaint.setColor(warningColor);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            measureArea();
            updateCursor();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawArea(canvas);
        drawConflictArea(canvas);
        drawMovingArea(canvas);
        drawAxis(canvas);
        drawCursor(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "onTouchEvent:ACTION_DOWN");
            if (isDragAble) {
                mLastX = event.getX();
                mLastY = event.getY();
                if (mCursorRectF.contains(mLastX, mLastY)) {
                    isTouchCursor = true;
                } else {
                    for (Map.Entry<RectF, DragData> entry : mDataByRectF.entrySet()) {
                        if (entry.getKey().contains(mLastX, mLastY)) {
                            mMoveDragData = entry.getValue();
                            mSelectedRectF = entry.getKey();
                            break;
                        }
                    }
                }
            }
            if (mMoveDragData != null) {
                invalidate();
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            Log.d(TAG, "onTouchEvent:ACTION_MOVE: " + isCanMove);
            float x = event.getX();
            float y = event.getY();
            if (isCanMove) {
                if (mMoveRectF == null) {
                    if (Math.abs(x - mLastX) < 1 && Math.abs(y - mLastY) < 1) {
                        return true;
                    }
                }
                if (mMoveRectF == null && mMoveDragData != null && mSelectedRectF != null) {
                    mMoveRectF = new RectF(
                        mContentArea.left + (mMoveDragData.getXPosition() - mMinXValue) * mXAxisPxUnit,
                        mSelectedRectF.top,
                        mContentArea.left
                            + (mMoveDragData.getXPosition() + mMoveDragData.getXMaxValue() - mMinXValue) * mXAxisPxUnit,
                        mSelectedRectF.bottom);
                }
                onDragMove(x - mLastX, y - mLastY);
            } else if (isTouchCursor) {
                float targetValue = (mContentArea.bottom - y) / mYAxisPxUnit;
                if (targetValue < 0) {
                    targetValue = 0;
                } else if (targetValue > mMaxYValue) {
                    targetValue = mMaxYValue;
                }
                setYTargetValue(targetValue);
            }
            mLastX = x;
            mLastY = y;
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            Log.d(TAG, "onTouchEvent:ACTION_UP");
            if (isCanMove) {
                if (mMoveRectF != null) {
                    if (RectF.intersects(mMoveRectF, mContentArea)) {
                        DragData tempD = mMoveDragData.copy();
                        adjustPositionAndSize(tempD, mMoveRectF.left);
                        DragData conflictData = getConflictData(tempD);
                        if (conflictData == null) {
                            removedData(mMoveDragData);
                            addObject(tempD, mMoveRectF.left);
                            rearrange();
                        } else if (mReceiveViewListener != null) {
                            mReceiveViewListener.onConflict(mMoveDragData, conflictData);
                        }
                    } else {
                        removedData(mMoveDragData);
                        rearrange();
                        if (mReceiveViewListener != null) {
                            mReceiveViewListener.onRemove(mMoveDragData);
                        }
                    }
                }
                mMoveDragData = null;
            }
            isCanMove = false;
            mMoveRectF = null;
            isTouchCursor = false;
            mSelectedRectF = null;
            forecastConflict(null);
        }
        return true;
    }

    /**
     * 添加数据
     *
     * @param obj
     * @param position
     */
    public void addObject(DragObject obj, RectF position) {
        if (RectF.intersects(position, mContentArea)) {
            DragData data = new DragData(obj);
            DragData conflictData = addObject(data, position.left);
            if (mReceiveViewListener != null) {
                if (conflictData != null) {
                    mReceiveViewListener.addChildResult(false, data.getObject() == conflictData.getObject() ? 1 : 2);
                } else {
                    mReceiveViewListener.addChildResult(true, 0);
                }
            }
        } else if (mReceiveViewListener != null) {
            mReceiveViewListener.addChildResult(false, 3);
        }

    }

    public void forecastConflict(DragData obj) {
        mConflictData.clear();
        if (obj != null) {
            for (int i = 0; i < mDataBlock.size(); i++) {
                DragData data = mDataBlock.get(i);
                if (data.getObject() == obj.getObject()
                    || (mReceiveViewListener != null && mReceiveViewListener.isConflict(obj, data))) {
                    mConflictData.add(data);
                }
            }
        }
        invalidate();
    }

    public void setDragData(List<DragData> data) {
        if (!ArrayUtils.isEmpty(data)) {
            mDataBlock.addAll(data);
        }
        rearrange();
    }

    /**
     * 获取每个15分钟的数据集合
     *
     * @return
     */
    public ArrayList<DragData>[] getTimeBlock() {
        return mTimeBlock;
    }

    public void cancelSelectedTarget() {
        if (mMoveRectF == null && mMoveDragData != null) {
            mMoveDragData = null;
            invalidate();
        }
    }

    /**
     * 获取添加的数据
     *
     * @return
     */
    public ArrayList<DragData> getData() {
        return new ArrayList<>(mDataBlock);
    }

    private void drawMovingArea(Canvas canvas) {
        if (mMoveRectF != null && mMoveRectF.left < mContentArea.right && mMoveRectF.right > mContentArea.left) {
            mMoveRectFCopy.set(mMoveRectF);
            mMoveRectFCopy.right = Math.min(mContentArea.right, mMoveRectF.right);
            canvas.drawRoundRect(mMoveRectFCopy, mAreaRadius, mAreaRadius, mMovingAreaPaint);
        }
    }

    private void drawArea(Canvas canvas) {
        for (Map.Entry<RectF, DragData> entry : mDataByRectF.entrySet()) {
            if (entry.getValue() != mMoveDragData) {
                if (entry.getValue().getYValue() != 0) {
                    mPaint.setColor(mAreaColor);
                    canvas.drawRoundRect(entry.getKey(), mAreaRadius, mAreaRadius, mPaint);
                    canvas.drawRoundRect(entry.getKey(), mAreaRadius, mAreaRadius, mBorderPaint);
                }
            } else if (mMoveRectF == null) {
                mPaint.setColor(mSelectedAreaColor);
                canvas.drawRoundRect(entry.getKey(), mAreaRadius, mAreaRadius, mPaint);
                canvas.drawRoundRect(entry.getKey(), mAreaRadius, mAreaRadius, mBorderPaint);
            }
        }
    }

    private void drawCursor(Canvas canvas) {
        if (mContentArea != null) {
            if (isDragAble) {
                canvas.drawPath(mCursorPath, mCursorLinePaint);
            }
            float top = mContentArea.bottom - Math.min(mMaxYValue, mFittedYValue) * mYAxisPxUnit;
            canvas.drawLine(mContentArea.left, top, mContentArea.right, top, mCursorLinePaint);
        }
    }

    private void drawConflictArea(Canvas canvas) {
        for (Map.Entry<RectF, DragData> entry : mDataByRectF.entrySet()) {
            for (int i = 0; i < mConflictData.size(); i++) {
                if (entry.getValue() == mConflictData.get(i) && !Objects.equals(entry.getValue(), mMoveDragData)) {
                    canvas.drawRoundRect(entry.getKey(), mAreaRadius, mAreaRadius, mConflictPaint);
                }
            }
        }
    }

    private void drawAxis(Canvas canvas) {
        if (mContentArea == null) {
            return;
        }
        // 左y轴
        canvas.drawLine(mContentArea.left - mAxisWidth, mContentArea.top, mContentArea.left - mAxisWidth,
            mContentArea.bottom + mAxisWidth, mAxisPaint);
        // 右y轴
        canvas.drawLine(mContentArea.right, mContentArea.top, mContentArea.right, mContentArea.bottom + mAxisWidth,
            mAxisPaint);
        // 底x轴
        canvas.drawLine(mContentArea.left - mAxisWidth, mContentArea.bottom + mAxisWidth, mContentArea.right,
            mContentArea.bottom + mAxisWidth, mAxisPaint);
        // 最大y值线
        canvas.drawLine(mContentArea.left - mAxisWidth, mContentArea.bottom - mMaxYHeight, mContentArea.right,
            mContentArea.bottom - mMaxYHeight, mMaxYPaint);
        for (int i = 0; i < mXAxisTickMarkDrawCount + 1; i++) {
            canvas.drawLine(mXAxisTickMarkDistance * i + mContentArea.left, mContentArea.bottom,
                mXAxisTickMarkDistance * i + mContentArea.left, mContentArea.bottom + mXAxisTickMarkHeight,
                mXAxisTickMarkPaint);
        }
        drawXAxisText(canvas);
    }

    private void drawXAxisText(Canvas canvas) {
        Paint.FontMetrics fm = getAxisTextFontMetrics();
        for (int i = 0; i < mXAxisTickMarkDrawCount + 1; i++) {
            String text = mValueFormatter.getFormattedValue(mMinXValue + i * mXAxisTickMarkDrawSpace);
            Rect rect = getAxisTextRect(text);
            canvas.drawText(text, mContentArea.left + i * mXAxisTickMarkDistance - rect.width() / 2,
                getHeight() - fm.bottom, mAxisTextPaint);
        }
    }

    private void measureArea() {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        Paint.FontMetrics fontMetrics = getAxisTextFontMetrics();
        Rect textRect = getAxisTextRect(SYMBOL2);
        float yAxisLeft = getPaddingLeft() + textRect.width() + mAxisWidth;
        float yAxisTop = getPaddingTop();
        float yAxisRight = getWidth() - getPaddingRight() - textRect.width() - mAxisWidth;
        float yAxisBottom =
            getHeight() - getPaddingBottom() - (int)(fontMetrics.bottom - fontMetrics.top) - mXAxisTickMarkHeight;
        mContentArea = new RectF(yAxisLeft, yAxisTop, yAxisRight, yAxisBottom - mAxisWidth - 1);
        mMaxYHeight = mContentArea.height() * 5 / 6;
        allocationXAxis();
        allocationYAxis();
    }

    /**
     * 分配x轴，计算x轴画几个坐标
     */
    private void allocationXAxis() {
        int range = mMaxXValue - mMinXValue;
        if (mContentArea == null || range <= 0) {
            return;
        }
        Log.d(TAG, "allocationXAxis");

        Rect rect = getAxisTextRect(SYMBOL1);
        int maxCount = (getWidth() - getPaddingLeft() - getPaddingRight()) / rect.width() - 3;
        maxCount = Math.max(maxCount, 2);
        int space = mXAxisMinSpacing;
        int count = range / space + (range % space == 0 ? 0 : 1);
        if (count > maxCount) {
            mXAxisTickMarkDrawSpace = range * 1f / maxCount;
            mXAxisTickMarkDrawCount = maxCount;
        } else {
            mXAxisTickMarkDrawSpace = space;
            mXAxisTickMarkDrawCount = count;
        }
        mXAxisTickMarkDistance = (mContentArea.width()) * 1f / mXAxisTickMarkDrawCount;
        mXAxisPxUnit = (mContentArea.width()) * 1f / range;
        mTimeBlock = new ArrayList[range / mXAxisMinSpacing];
        Log.d(TAG, "mTimeBlock length: " + mTimeBlock.length);
        if (mYAxisPxUnit >= 0 && mXAxisPxUnit >= 0) {
            rearrange();
            if (mReceiveViewListener != null) {
                mReceiveViewListener.layoutFinish();
            }
        }
    }

    private void allocationYAxis() {
        if (mContentArea == null) {
            return;
        }
        mYAxisPxUnit = mMaxYHeight / mMaxYValue;
        if (mYAxisPxUnit >= 0 && mXAxisPxUnit >= 0) {
            rearrange();
            if (mReceiveViewListener != null) {
                mReceiveViewListener.layoutFinish();
            }
        }
    }

    /**
     * 添加数据
     *
     * @param data
     * @param xPositionPx
     * @return 有冲突返回冲突数据，没有则不返回
     */
    private DragData addObject(DragData data, float xPositionPx) {
        forecastConflict(data);
        adjustPositionAndSize(data, xPositionPx);
        DragData conflictData = null;
        if (isFill(data)) {
            if (mReceiveViewListener != null) {
                mReceiveViewListener.addChildResult(false, 4);
            }
        } else {
            conflictData = getConflictData(data);
            if (conflictData == null) {
                addData(data);
            }
        }
        forecastConflict(null);
        return conflictData;
    }

    private void addData(DragData data) {
        int maxX = data.getXValue() + data.getXPosition() - mMinXValue;
        int xPositionM = data.getXPosition() - mMinXValue;
        int start = xPositionM / mXAxisMinSpacing;
        int to = Math.min(maxX / mXAxisMinSpacing, mTimeBlock.length);
        for (int i = start; i < to; i++) {
            ArrayList<DragData> list = mTimeBlock[i];
            if (list == null) {
                list = new ArrayList<>();
                mTimeBlock[i] = list;
            }
            float left = i * mXAxisMinSpacing * mXAxisPxUnit + mContentArea.left;
            float right = left + mXAxisMinSpacing * mXAxisPxUnit - 0.0000001f;
            float topY = 0f;
            for (int j = 0; j < list.size(); j++) {
                topY += list.get(j).getYValue();
            }
            float bottom = mContentArea.bottom - topY * mYAxisPxUnit;
            float top = Math.max(bottom - data.getYValue() * mYAxisPxUnit, mContentArea.top);
            mDataByRectF.put(new RectF(left, top, right, bottom), data);
            list.add(data);
        }
        Log.d(TAG, "addData:start: " + start + "  to: " + to);
        mDataBlock.add(data);
    }

    private boolean isFill(DragData object) {
        float max = mMaxYValue * 1.2f;
        int maxX = object.getXValue() + object.getXPosition() - mMinXValue;
        int xPositionM = object.getXPosition() - mMinXValue;
        int start = xPositionM / mXAxisMinSpacing;
        int to = Math.min(maxX / mXAxisMinSpacing, mTimeBlock.length);
        for (int i = start; i < to; i++) {
            ArrayList<DragData> list = mTimeBlock[i];
            float value = 0f;
            for (int j = 0; j < ArrayUtils.size(list); j++) {
                value += list.get(j).getYValue();
            }
            if (value < max) {
                return false;
            }
        }
        return true;
    }

    private void removedData(DragData data) {
        for (int i = 0; i < mTimeBlock.length; i++) {
            ArrayList<DragData> list = mTimeBlock[i];
            if (list != null) {
                list.remove(data);
            }
        }
        mDataBlock.remove(data);
        final ArrayList<RectF> deleteRectF = new ArrayList<>();
        for (Map.Entry<RectF, DragData> entry : mDataByRectF.entrySet()) {
            if (Objects.equals(entry.getValue(), data)) {
                deleteRectF.add(entry.getKey());
            }
        }
        for (int i = 0; i < deleteRectF.size(); i++) {
            mDataByRectF.remove(deleteRectF.get(i));
        }
    }

    private void adjustPositionAndSize(DragData data, float xPositionPx) {
        float newXPosition = getXPosition(xPositionPx);
        float startMinuteF = (newXPosition - mContentArea.left) / mXAxisPxUnit;
        int startMinute = (int)MathUtils.floatScale(startMinuteF, 0, RoundingMode.HALF_UP);
        Log.d(TAG, "startMinuteF: " + startMinuteF + "startMinute: " + startMinute);
        data.setXPosition(startMinute + mMinXValue);
        data.setXValue(Math.min(mMaxXValue - data.getXPosition(), data.getXMaxValue()));
    }

    private DragData getConflictData(DragData data) {
        int xEnd = data.getXPosition() + data.getXValue();
        for (int i = 0; i < mConflictData.size(); i++) {
            DragData key = mConflictData.get(i);
            if (Objects.equals(data, key)) {
                continue;
            }
            if (key.getXPosition() <= data.getXPosition()
                && data.getXPosition() <= (key.getXPosition() + key.getXValue())) {
                Log.d(TAG, "开始时间冲突");
                return key;
            }
            if (key.getXPosition() <= xEnd && (key.getXValue() + key.getXPosition()) >= xEnd) {
                Log.d(TAG, "结束时冲突");
                return key;
            }
        }
        return null;
    }

    private float getXPosition(float xPositionPx) {
        if (MathUtils.isEqual(mXAxisPxUnit, 0)) {
            return mContentArea.left;
        }
        float tox;
        if (xPositionPx < mContentArea.left) {
            tox = mContentArea.left;
        } else {
            float toLeft = xPositionPx - mContentArea.left;
            float xSorption = mXAxisPxUnit * mXAxisMinSpacing;
            float surplus = toLeft / xSorption;
            surplus = (surplus - (int)surplus) * xSorption;
            if (surplus > xSorption / 2) {
                tox = xPositionPx + xSorption - surplus;
            } else {
                tox = xPositionPx - surplus;
            }
            if (xPositionPx < mContentArea.left) {
                tox = mContentArea.left;
            } else if (mContentArea.right - xPositionPx < xSorption) {
                tox = mContentArea.right - xSorption;
            }
        }
        return tox;
    }

    private void rearrange() {
        if (mYAxisPxUnit < 0 || mXAxisPxUnit < 0) {
            return;
        }
        // 所有的数据
        final ArrayList<DragData> dataBlock = new ArrayList<>(mDataBlock);
        for (int i = 0; i < mTimeBlock.length; i++) {
            ArrayList<DragData> list = mTimeBlock[i];
            if (list != null) {
                list.clear();
            }
        }
        mDataBlock.clear();
        mDataByRectF.clear();
        for (int i = 0; i < dataBlock.size(); i++) {
            DragData data = dataBlock.get(i);
            addObject(data, mContentArea.left + (data.getXPosition() - mMinXValue) * mXAxisPxUnit);
        }
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

    private void onDragMove(float dx, float dy) {
        if (mMoveRectF == null) {
            return;
        }
        mMoveRectF.offset(dx, dy);
        invalidate();
    }

    private void updateCursor() {
        float fittedYValue = mFittedYValue;
        if (mYAxisPxUnit <= 0 || fittedYValue < 0 || mContentArea == null) {
            return;
        }
        mCursorPath.reset();
        mCursorPath.setFillType(Path.FillType.EVEN_ODD);
        if (fittedYValue > mMaxYValue) {
            fittedYValue = mMaxYValue;
        }
        float yLeft = mContentArea.bottom - fittedYValue * mYAxisPxUnit;
        float xLeft = mContentArea.right + 4;
        mCursorPath.moveTo(xLeft, yLeft);
        float yTop = yLeft - 25;
        float xRight = xLeft + 40;
        mCursorPath.lineTo(xRight, yTop);
        float yBottom = yLeft + 25;
        mCursorPath.lineTo(xRight, yBottom);
        mCursorPath.close();
        mCursorRectF.set(xLeft, yTop - 25, getWidth(), yBottom + 25);
        Log.d(TAG, "updateCursor");
        invalidate();
    }

    class Gesture extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mReceiveViewListener != null) {
                float x = e.getX();
                float y = e.getY();
                for (Map.Entry<RectF, DragData> entry : mDataByRectF.entrySet()) {
                    if (entry.getKey().contains(x, y)) {
                        mMoveDragData = entry.getValue();
                        invalidate();
                        mReceiveViewListener.onClick(mMoveDragData);
                        break;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            openDrag();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            openDrag();
        }

        private void openDrag() {
            if (!isDragAble) {
                return;
            }
            if (mMoveDragData != null) {
                forecastConflict(mMoveDragData);
                isCanMove = true;
            }
        }
    }

    /**
     * ============================================================================================================================
     */

    public void clear() {
        for (int i = 0; i < ArrayUtils.size(mTimeBlock); i++) {
            ArrayList<DragData> list = mTimeBlock[i];
            if (list != null) {
                list.clear();
            }
        }
        mDataBlock.clear();
        mDataByRectF.clear();
        mConflictData.clear();
        invalidate();
    }

    public void setXAxis(int minValue, int maxValue) {
        mMinXValue = minValue;
        mMaxXValue = maxValue;
        allocationXAxis();
    }

    public void setYAxis(float maxValue) {
        mMaxYValue = maxValue;
        allocationYAxis();
        updateCursor();
    }

    public float getYTargetValue() {
        return Math.min(mFittedYValue, mMaxYValue);
    }

    public void setYTargetValue(float targetValue) {
        mFittedYValue = targetValue;
        if (mReceiveViewListener != null) {
            mReceiveViewListener.auxiliaryYAxisMax(targetValue);
        }
        updateCursor();
    }

    public void setReceiveViewListener(ReceiveViewListener listener) {
        mReceiveViewListener = listener;
    }

    public RectF getContainerRectF() {
        return mContentArea;
    }

    public int getXMinValue() {
        return mMinXValue;
    }

    public int getXMaxValue() {
        return mMaxXValue;
    }

    public interface ReceiveViewListener {
        /**
         * 点击
         *
         * @param obj
         */
        default void onClick(DragData obj) {}

        /**
         * 移除
         *
         * @param obj
         */
        default void onRemove(DragData obj) {}

        /**
         * 内部拖拽结束后如果有冲突，则回调
         *
         * @param move
         * @param to
         */
        default void onConflict(DragData move, DragData to) {}

        /**
         * 添加子组件
         *
         * @param isSuccess true添加成功
         * @param reason 1：已存在；2：有冲突；3：不在范围
         */
        default void addChildResult(boolean isSuccess, int reason) {}

        /**
         * 移动时，X轴有重叠的两个数据对象
         *
         * @param obj1 Object
         * @param obj2 Object
         * @return true表示有冲突，false没有冲突
         */
        default boolean isConflict(DragData obj1, DragData obj2) {
            return false;
        }

        default void auxiliaryYAxisMax(float value) {}

        /**
         * 表示各种测量完成
         */
        default void layoutFinish() {}
    }

    /**
     * 获取Y轴每单位占用的像素点
     *
     * @return float
     */
    public float getYAxisPxUnit() {
        return mYAxisPxUnit;
    }

    public static class ValueFormatter {
        public String getFormattedValue(float value) {
            return time2Str((int)value);
        }

        private static String time2Str(int time) {
            int h = time / 60;
            int minute = time % 60;
            StringBuilder sb = new StringBuilder();
            sb.append(h < 10 ? "0" + h : String.valueOf(h)).append(":");
            sb.append(minute < 10 ? "0" + minute : String.valueOf(minute));
            return sb.toString();
        }
    }
}
