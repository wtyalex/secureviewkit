package com.wty.secureviewkit.widgets.activity;

import com.wty.foundation.common.utils.ScreenUtils;
import com.wty.foundation.core.base.dialog.LoadDialog;
import com.wty.foundation.core.safe.OnSafeClickListener;
import com.wty.foundation.core.utils.ViewBindingUtils;
import com.wty.foundation.databinding.ActivityBaseBinding;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

/**
 * @author lifawei
 * @createTime 2023/2/16 10:35
 * @describe
 */
public abstract class ActionBarBaseActivity<VB extends ViewBinding, AC extends ViewBinding> extends AppCompatActivity
    implements View.OnClickListener {
    private static final String LOADING_DIALOG = "loading_dialog";
    private LoadDialog mLoadDialog;
    private VB mViewBinding;
    private AC mActionBarBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarStyle();
        Fragment loadDialog = getSupportFragmentManager().findFragmentByTag(LOADING_DIALOG);
        if (loadDialog instanceof DialogFragment) {
            mLoadDialog = (LoadDialog)loadDialog;
        }
        beforeView();
        ActivityBaseBinding mBaseBinding = ActivityBaseBinding.inflate(getLayoutInflater());
        mViewBinding = ViewBindingUtils.create(this.getClass(), getLayoutInflater());
        mActionBarBinding = createActionBar();
        mBaseBinding.getRoot().addView(mActionBarBinding.getRoot(),
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mBaseBinding.getRoot().addView(mViewBinding.getRoot(),
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(mBaseBinding.getRoot());
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeLoadDialog();
    }

    @Override
    public void onClick(View view) {}

    /**
     * 显示加载等待框
     */
    protected void showLoadDialog(String msg) {
        if (mLoadDialog == null) {
            mLoadDialog = new LoadDialog();
        }
        mLoadDialog.setMsg(msg);
        if (!mLoadDialog.isShowing()) {
            mLoadDialog.show(getSupportFragmentManager(), LOADING_DIALOG);
        }
    }

    /**
     * 关闭加载等待框
     */
    protected void closeLoadDialog() {
        if (mLoadDialog != null && mLoadDialog.isShowing()) {
            mLoadDialog.dismiss();
        }
    }

    protected final VB getViewBinding() {
        return mViewBinding;
    }

    protected final AC getActionBarBinding() {
        return mActionBarBinding;
    }

    protected void setStatusBarStyle() {
        ScreenUtils.setStatusBar(this, true, false, 0, true);
    }

    protected View.OnClickListener getOnClickListener() {
        return new OnSafeClickListener() {
            @Override
            protected void onSafeClick(View v) {
                ActionBarBaseActivity.this.onClick(v);
            }
        };
    }

    protected void beforeView() {}

    protected abstract AC createActionBar();

    protected abstract void initView();

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 获取当前获得焦点的View
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            // 调用方法判断是否需要隐藏键盘
            hideKeyboard(ev, view);
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 根据传入控件的坐标和用户的焦点坐标，判断是否隐藏键盘，如果点击的位置在控件内，则不隐藏键盘
     *
     * @param view 控件view
     * @param event 焦点位置
     * @return 是否隐藏
     */
    private void hideKeyboard(MotionEvent event, View view) {
        try {
            if (view instanceof EditText) {
                int[] location = {0, 0};
                view.getLocationInWindow(location);
                int left = location[0], top = location[1], right = left + view.getWidth(),
                    bootom = top + view.getHeight();
                // （判断是不是EditText获得焦点）判断焦点位置坐标是否在控件所在区域内，如果位置在控件区域外，则隐藏键盘
                if (event.getRawX() < left || event.getRawX() > right || event.getY() < top
                    || event.getRawY() > bootom) {
                    // 隐藏键盘
                    IBinder token = view.getWindowToken();
                    InputMethodManager inputMethodManager =
                        (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}