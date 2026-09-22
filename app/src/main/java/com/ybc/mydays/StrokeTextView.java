package com.ybc.mydays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class StrokeTextView extends AppCompatTextView {

    public StrokeTextView(Context context) {
        super(context);
    }

    public StrokeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StrokeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 保存画笔原本的状态和颜色
        Paint paint = getPaint();
        int currentTextColor = getCurrentTextColor();
        Paint.Style currentStyle = paint.getStyle();

        // 动态计算边框粗细：字体越大，边框越粗 (大约是字体的 1/15)
        float strokeWidth = getTextSize() / 15f;

        // 1. 绘制白色的外边框
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        setTextColor(Color.WHITE); // 边框颜色设为纯白
        super.onDraw(canvas);

        // 2. 绘制真正的内层文字
        paint.setStyle(currentStyle);
        paint.setStrokeWidth(0);
        setTextColor(currentTextColor);
        super.onDraw(canvas);
    }
}