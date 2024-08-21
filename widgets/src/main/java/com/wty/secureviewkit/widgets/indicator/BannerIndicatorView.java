package com.wty.secureviewkit.widgets.indicator;

import com.wty.secureviewkit.widgets.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

/**
 * @author lifawei
 * @createTime 2023/6/12 15:23
 * @describe
 */
public class BannerIndicatorView extends LinearLayout {
    private int selectColor;
    private int normalColor;
    private int indicatorCount;
    private int indicatorPosition;
    private int indicatorSelectWidth;
    private int indicatorSelectHeight;
    private int indicatorNormalWidth;
    private int indicatorNormalHeight;
    private int indicatorMargin;

    public BannerIndicatorView(Context context) {
        this(context, null);
    }

    public BannerIndicatorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BannerIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BannerIndicatorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initXmlAttrs(context, attrs);
    }

    private void initXmlAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerIndicatorView);
        indicatorSelectWidth = (int)typedArray.getDimension(R.styleable.BannerIndicatorView_indicator_select_width, 0f);
        indicatorSelectHeight =
            (int)typedArray.getDimension(R.styleable.BannerIndicatorView_indicator_select_height, 0f);
        indicatorNormalWidth = (int)typedArray.getDimension(R.styleable.BannerIndicatorView_indicator_normal_width, 0f);
        indicatorNormalHeight =
            (int)typedArray.getDimension(R.styleable.BannerIndicatorView_indicator_normal_height, 0f);
        indicatorMargin = (int)typedArray.getDimension(R.styleable.BannerIndicatorView_indicator_margins, 0f);
        selectColor = typedArray.getResourceId(R.styleable.BannerIndicatorView_indicator_select_color,
            R.drawable.shape_indicator_selected);
        normalColor = typedArray.getResourceId(R.styleable.BannerIndicatorView_indicator_normal_color,
            R.drawable.shape_indicator_normal);
        typedArray.recycle();
    }

    public final void initIndicatorCount(int count) {
        this.indicatorCount = count;
        this.indicatorPosition = 0;
        this.removeAllViews();
        for (int i = 0; i < count; i++) {
            ImageView ivIndicator = new ImageView(getContext());
            int widthParam = indicatorNormalWidth;
            int heightParam = indicatorNormalHeight;
            ivIndicator.setImageResource(normalColor);
            if (i == indicatorPosition) {
                widthParam = indicatorSelectWidth;
                heightParam = indicatorSelectHeight;
                ivIndicator.setImageResource(selectColor);
            }
            LinearLayoutCompat.LayoutParams lp = new LinearLayoutCompat.LayoutParams(widthParam, heightParam);
            if (i != 0) {
                lp.leftMargin = indicatorMargin;
            }
            this.addView(ivIndicator, lp);
        }
    }

    public final void changeIndicator(int position) {
        if (position < 0 || getChildCount() == 0 || position == indicatorPosition) {
            return;
        }
        position = position % getChildCount();
        ImageView ivIndicator = (ImageView)getChildAt(indicatorPosition);
        ivIndicator.setImageResource(normalColor);
        LayoutParams lp = (LayoutParams)ivIndicator.getLayoutParams();
        lp.width = indicatorNormalWidth;
        lp.height = indicatorNormalHeight;
        ivIndicator.setLayoutParams(lp);
        indicatorPosition = position;
        ivIndicator = (ImageView)getChildAt(indicatorPosition);
        ivIndicator.setImageResource(selectColor);
        LayoutParams lp1 = (LayoutParams)ivIndicator.getLayoutParams();
        lp1.width = indicatorSelectWidth;
        lp1.height = indicatorSelectHeight;
        ivIndicator.setLayoutParams(lp1);
    }
}
