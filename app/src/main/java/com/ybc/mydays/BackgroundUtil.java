package com.ybc.mydays;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

/**
 * 处理背景渲染
 */
public class BackgroundUtil {
    public static void applyBackground(View view, String colorStr, String defaultColor) {
        String parseStr = (colorStr != null && !colorStr.isEmpty()) ? colorStr : defaultColor;

        if (parseStr.contains(",")) {
            // 解析多色渐变
            String[] hexes = parseStr.split(",");
            int[] colors = new int[hexes.length];
            for (int i = 0; i < hexes.length; i++) {
                colors[i] = Color.parseColor(hexes[i].trim());
            }
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
            view.setBackground(gd);
        } else {
            // 解析纯色
            view.setBackgroundColor(Color.parseColor(parseStr.trim()));
        }
    }
}