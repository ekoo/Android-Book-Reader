package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.util.AttributeSet;

public class SearchView extends android.support.v7.widget.SearchView {
    OnCloseListener listener;

    public SearchView(Context context) {
        super(context);
    }

    public SearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setOnCloseListener(OnCloseListener listener) {
        super.setOnCloseListener(listener);
        this.listener = listener;
    }

    @Override
    public void onActionViewCollapsed() {
        super.onActionViewCollapsed();
        listener.onClose();
    }
}
