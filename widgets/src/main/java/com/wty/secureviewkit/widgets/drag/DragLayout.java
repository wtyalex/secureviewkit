package com.wty.secureviewkit.widgets.drag;

import com.wty.secureviewkit.widgets.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author lifawei
 * @createTime 2023/1/18 13:52
 * @describe
 */
public class DragLayout extends LinearLayout {
    private static final String TAG = "DragLayout";
    private final Paint mPaint;
    private final Paint mBorderPaint;
    private final GestureDetector mGestureDetector;
    private ProvideView mProvideView;
    private ReceiveView mReceiveView;
    private RectF mFromViewRectF;
    private RectF mReceiveViewRectF;
    private RectF mDrawRectF;
    private float mRadius = 0f;
    private boolean isLongPress;
    private boolean isDispatchTouchEventToReceiveView;
    private boolean isDragAble = true;
    private DragObject mTargetObj;
    private float mLastX;
    private float mLastY;
    private RecyclerView.ViewHolder mSelectedViewHolder;
    private Drawable mSelectedViewHolderBackground;
    private int mDragColor;
    private int mSelectedColor;
    private int mBorderColor;

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DragLayout);
        mRadius = ta.getDimensionPixelSize(R.styleable.DragLayout_dragRadius, 0);
        mDragColor = ta.getColor(R.styleable.DragLayout_dragColor, Color.BLUE);
        mSelectedColor = ta.getColor(R.styleable.DragLayout_selectedColor, Color.TRANSPARENT);
        isDragAble = ta.getBoolean(R.styleable.DragLayout_isDragAble, true);
        mBorderColor = ta.getColor(R.styleable.DragLayout_borderColor, Color.RED);
        ta.recycle();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mDragColor);

        // 子区域的边框画笔
        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStrokeWidth(1);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(mBorderColor);
        mGestureDetector = new GestureDetector(context, new Gesture());
        setWillNotDraw(false);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mDrawRectF == null) {
            return;
        }
        canvas.drawRoundRect(mDrawRectF, mRadius, mRadius, mPaint);
        canvas.drawRoundRect(mDrawRectF, mRadius, mRadius, mBorderPaint);
        Log.d(TAG, "draw");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!isDragAble) {
            return super.dispatchTouchEvent(ev);
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mLastX = ev.getX();
            mLastY = ev.getY();
            Log.d(TAG, "dispatchTouchEvent:isDispatchTouchEventToReceiveView:" + mLastX + " " + mLastY + " "
                + mReceiveViewRectF);
            isDispatchTouchEventToReceiveView = inRect(mLastX, mLastY, mReceiveViewRectF);
        }
        Log.d(TAG, "dispatchTouchEvent:isDispatchTouchEventToReceiveView:" + isDispatchTouchEventToReceiveView);
        if (isDispatchTouchEventToReceiveView) {
            if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                isDispatchTouchEventToReceiveView = false;
            }
            return super.dispatchTouchEvent(ev);
        }
        boolean result = mGestureDetector.onTouchEvent(ev);
        if (isLongPress) {
            if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                if (mReceiveView != null) {
                    mReceiveView.forecastConflict(null);
                }
                onLongPressUp();
            } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                Log.d(TAG, "onMove");
                updateUI(ev.getX() - mLastX, ev.getY() - mLastY);
                mLastX = ev.getX();
                mLastY = ev.getY();
            }
            return result;
        }
        Log.d(TAG, "super.dispatchTouchEvent()");
        return super.dispatchTouchEvent(ev) || result;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mProvideView == null || mProvideView == null) {
            mProvideView = findViewById(R.id.drag_from_container);
            if (mProvideView == null) {
                throw new RuntimeException("没有id是R.id.drag_from_container的子View");
            }
            mReceiveView = findViewById(R.id.drag_receive_container);
            if (mReceiveView == null) {
                throw new RuntimeException("没有id是R.id.drag_receive_container的子View");
            }
        }
        int[] provideViewOnScreen = new int[2];
        mProvideView.getLocationOnScreen(provideViewOnScreen);
        int[] selfOnScreen = new int[2];
        getLocationOnScreen(selfOnScreen);
        float provideViewLeft = provideViewOnScreen[0] - selfOnScreen[0];
        float provideViewTop = provideViewOnScreen[1] - selfOnScreen[1];
        mFromViewRectF = new RectF(provideViewLeft, provideViewTop, provideViewLeft + mProvideView.getWidth(),
            provideViewTop + mProvideView.getHeight());
        int[] receiveViewOnScreen = new int[2];
        mReceiveView.getLocationOnScreen(receiveViewOnScreen);
        float receiveViewLeft = receiveViewOnScreen[0] - selfOnScreen[0];
        float receiveViewTop = receiveViewOnScreen[1] - selfOnScreen[1];
        mReceiveViewRectF = new RectF(receiveViewLeft, receiveViewTop, receiveViewLeft + mReceiveView.getWidth(),
            receiveViewTop + mReceiveView.getHeight());
    }

    private void onLongPressUp() {
        if (!isLongPress) {
            return;
        }
        isLongPress = false;
        if (mSelectedViewHolder != null) {
            mSelectedViewHolder.itemView.setBackground(mSelectedViewHolderBackground);
            mSelectedViewHolder = null;
        }
        if (mReceiveView != null && mTargetObj != null && intersects(mReceiveViewRectF, mDrawRectF)) {
            RectF newRectF = new RectF(mDrawRectF);
            newRectF.offsetTo(mDrawRectF.left - mReceiveViewRectF.left, mDrawRectF.top - mReceiveViewRectF.top);
            // mReceiveView.addObject(mTargetObj, newRectF, false);
            mReceiveView.addObject(mTargetObj, newRectF);
            Log.d(TAG, "onLongPressUp:" + mDrawRectF.top + " " + mReceiveViewRectF.top);
        }
        mDrawRectF = null;
        mTargetObj = null;
        Log.d(TAG, "onLongPressUp:");
    }

    public void setDragAble(boolean dragAble) {
        isDragAble = dragAble;
    }

    public void setRadius(float radius) {
        this.mRadius = radius;
    }

    public void setDragColor(int dragColor) {
        this.mDragColor = dragColor;
        mPaint.setColor(mDragColor);
    }

    private void updateUI(float distanceX, float distanceY) {
        if (mDrawRectF == null) {
            return;
        }
        Log.d(TAG, "updateUI:");
        mDrawRectF.offset(distanceX, distanceY);
        invalidate();
    }

    class Gesture extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown");
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "onLongPress");
            isLongPress = true;
            e.setAction(MotionEvent.ACTION_CANCEL);
            DragLayout.super.dispatchTouchEvent(e);
            float x = e.getX();
            float y = e.getY();
            if (inRect(x, y, mFromViewRectF)) {
                RectF fromToParent = findChildViewUnderProvideView(x - mFromViewRectF.left, y - mFromViewRectF.top);
                if (fromToParent == null) {
                    return;
                }
                mReceiveView.forecastConflict(new DragData(mTargetObj));
                mDrawRectF = fromToParent;
            }
            updateUI(0, 0);
        }
    }

    private boolean inRect(float x, float y, RectF target) {
        if (target == null) {
            return false;
        }
        return target.contains(x, y);
    }

    private static boolean intersects(RectF a, RectF b) {
        if (a == null || b == null) {
            return false;
        }
        return RectF.intersects(a, b);
    }

    private RectF findChildViewUnderProvideView(float x, float y) {
        View child = mProvideView.findChildViewUnder(x, y);
        if (child != null) {
            Log.d(TAG, "findChildViewUnderProvideView :" + child.getClass().getName());
            mSelectedViewHolder = mProvideView.findContainingViewHolder(child);
            mSelectedViewHolderBackground = mSelectedViewHolder.itemView.getBackground();
            mSelectedViewHolder.itemView.setBackgroundColor(mSelectedColor);
            View childView = mSelectedViewHolder.itemView.findViewById(R.id.drag_from_child_view);
            if (childView != null) {
                int[] locachild = new int[2];
                childView.getLocationOnScreen(locachild);
                int[] locaSelf = new int[2];
                getLocationOnScreen(locaSelf);
                int left = locachild[0] - locaSelf[0];
                int top = locachild[1] - locaSelf[1];
                RectF result = new RectF(left, top, left + childView.getWidth(), top + childView.getHeight());

                Object obj = child.getTag(R.id.drag_target_data);
                if (obj instanceof DragObject) {
                    mTargetObj = (DragObject)obj;
                }
                return result;
            }
        }
        Log.d(TAG, "findChildViewUnderProvideView is null");
        return null;
    }
}
