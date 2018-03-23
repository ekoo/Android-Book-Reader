package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.github.axet.bookreader.R;

public class NavigationDescriptionView extends AppCompatTextView {
    public NavigationDescriptionView(Context context) {
        super(context);
    }

    public NavigationDescriptionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigationDescriptionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NavigationDescriptionView(Context context, String text) {
        super(context);
        setText(text);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ViewGroup f = (ViewGroup) getParent(); // FrameLayout
        ViewGroup m = (ViewGroup) f.getParent(); // NavigationMenuItemView
        View t = m.findViewById(R.id.design_menu_item_text);
        if (t != null)
            t.setVisibility(GONE);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

}