package com.wty.secureviewkit.widgets.activity;

import com.wty.foundation.common.utils.ResUtils;
import com.wty.foundation.common.utils.ViewUtils;
import com.wty.foundation.core.safe.OnSafeClickListener;
import com.wty.foundation.databinding.UiActionBarItiBinding;
import com.wty.secureviewkit.widgets.R;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

public abstract class BaseActivity<VB extends ViewBinding> extends ActionBarBaseActivity<VB, UiActionBarItiBinding> {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActionBar();
    }

    @Override
    protected final UiActionBarItiBinding createActionBar() {
        return UiActionBarItiBinding.inflate(getLayoutInflater());
    }

    private void initActionBar() {
        getActionBarBinding().actionBarLeft.setOnClickListener(new OnSafeClickListener() {
            @Override
            protected void onSafeClick(View v) {
                onBackPressed();
            }
        });
        getActionBarBinding().actionBarLeft.setImageResource(R.drawable.black_back);
        getActionBarBinding().actionBarLeft.setScaleType(ImageView.ScaleType.FIT_START);
        ViewUtils.setVerticalPadding(getActionBarBinding().actionBarLeft,
            ResUtils.getDimensionPixelSize(R.dimen.back_icon_vertical_padding));
        ViewUtils.setHorizontalPadding(getActionBarBinding().actionBarLeft,
            ResUtils.getDimensionPixelSize(R.dimen.action_bar_horizontal_padding), 0);
        getActionBarBinding().actionBarRight.setScaleType(ImageView.ScaleType.FIT_END);
        ViewUtils.setVerticalPadding(getActionBarBinding().actionBarRight,
            ResUtils.getDimensionPixelSize(R.dimen.back_icon_vertical_padding));
        ViewUtils.setHorizontalPadding(getActionBarBinding().actionBarRight, 0,
            ResUtils.getDimensionPixelSize(R.dimen.action_bar_horizontal_padding));
        setActionBar(getActionBarBinding());
    }

    protected void setActionBar(UiActionBarItiBinding actionbar) {}

}