package im.socks.yysk;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by cole on 2017/10/27.
 */

public class PageBar extends RelativeLayout {
    public PageBar(Context context) {
        this(context, null);
        init(context, null, R.attr.pageBarStyle, 0);
    }

    public PageBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.pageBarStyle);

    }

    public PageBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PageBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        LayoutInflater.from(context).inflate(R.layout.view_pagebar, this, true);

        TextView titleView = findViewById(R.id.titleView);
        TextView backView = findViewById(R.id.backView);
        TextView actionView = findViewById(R.id.actionView);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PageBar, defStyleAttr, defStyleRes);
        titleView.setText(a.getText(R.styleable.PageBar_pb_title));

        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, context.getResources().getDisplayMetrics());
        setPadding(padding, padding, padding, padding);

        setBackgroundColor(a.getColor(R.styleable.PageBar_pb_backgroundColor, 0x000000));

        int textSize = a.getDimensionPixelSize(R.styleable.PageBar_pb_textSize, 24);//24sp?
        int textColor = a.getColor(R.styleable.PageBar_pb_textColor, 0xffffff);//


        titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        backView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        actionView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        titleView.setTextColor(textColor);
        backView.setTextColor(textColor);
        actionView.setTextColor(textColor);

        a.recycle();
    }


}
