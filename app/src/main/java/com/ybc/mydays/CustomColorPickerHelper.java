package com.ybc.mydays;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

public class CustomColorPickerHelper {

    public interface OnColorsPickedListener {
        void onPicked(String colorString); // 返回格式如 "#FF0000" 或 "#FF0000,#0000FF"
    }

    // 显示背景颜色选择器（允许用户选择 1、2 或 3 种颜色进行渐变）
    public static void showBackgroundPicker(Context context, OnColorsPickedListener listener) {
        String[] modes = {"纯色背景 (1种颜色)", "双色渐变 (2种颜色)", "三色渐变 (3种颜色)"};
        new AlertDialog.Builder(context)
                .setTitle("选择背景色彩模式")
                .setItems(modes, (dialog, which) -> {
                    int totalColors = which + 1;
                    pickColors(context, totalColors, 0, new StringBuilder(), listener);
                }).show();
    }

    // 显示文字颜色选择器（强制只能选 1 种纯色）
    public static void showTextPicker(Context context, OnColorsPickedListener listener) {
        pickColors(context, 1, 0, new StringBuilder(), listener);
    }

    // 递归调用第三方库，实现连续多次选色
    private static void pickColors(Context context, int total, int current, StringBuilder sb, OnColorsPickedListener listener) {
        if (current == total) {
            listener.onPicked(sb.toString());
            return;
        }

        String title = (total == 1) ? "选择颜色" : "请选择第 " + (current + 1) + " 个颜色";

        new ColorPickerDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton("确定", (ColorEnvelopeListener) (envelope, fromUser) -> {
                    if (current > 0) sb.append(",");
                    // 获取选中的 ARGB 颜色并拼接
                    sb.append("#").append(envelope.getHexCode());
                    pickColors(context, total, current + 1, sb, listener);
                })
                .setNegativeButton("取消", (dialogInterface, i) -> dialogInterface.dismiss())
                .attachAlphaSlideBar(true) // 允许调透明度
                .attachBrightnessSlideBar(true) // 允许调明暗
                .show();
    }
}