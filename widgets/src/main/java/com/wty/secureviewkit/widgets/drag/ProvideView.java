package com.wty.secureviewkit.widgets.drag;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author lifawei
 * @createTime 2023/1/18 14:49
 * @describe
 */
public class ProvideView extends RecyclerView {
    public ProvideView(Context context) {
        this(context, null);
    }

    public ProvideView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProvideView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
