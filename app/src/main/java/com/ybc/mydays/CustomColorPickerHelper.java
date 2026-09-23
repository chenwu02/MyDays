package com.ybc.mydays;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;

import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

/**
 * 自定义颜色选择器工具类
 * 封装第三方库 ColorPickerView，支持单色以及多色渐变的连续选取。
 */
public class CustomColorPickerHelper {

    // 常量定义
    private static final String[] GRADIENT_MODES = {
            "纯色背景 (1种颜色)",
            "双色渐变 (2种颜色)",
            "三色渐变 (3种颜色)"
    };

    /**
     * 工具类禁止实例化
     */
    private CustomColorPickerHelper() {
        throw new UnsupportedOperationException("Helper class cannot be instantiated");
    }

    /**
     * 颜色选取完成的回调契约
     */
    public interface OnColorsPickedListener {
        /**
         * 当用户完成所有规定数量的颜色选取后触发
         *
         * @param colorString 最终的十六进制颜色字符串。
         *                    包含透明度 (ARGB)，格式始终带 "#" 号。
         *                    单色示例: "#FFFF0000"
         *                    多色示例: "#FFFF0000,#FF0000FF" (多色之间以逗号严格分隔)
         */
        void onPicked(String colorString);
    }

    /**
     * 唤起背景颜色选择器
     * 允许用户先选择色彩模式（单色/双色/三色），然后再连续拾取对应数量的颜色。
     *
     * @param context  上下文，用于构建 Dialog
     * @param listener 选色完成后的回调
     */
    public static void showBackgroundPicker(Context context, OnColorsPickedListener listener) {
        new AlertDialog.Builder(context)
                .setTitle("选择背景色彩模式")
                .setItems(GRADIENT_MODES, (dialog, which) -> {
                    // which 为数组索引 0,1,2，对应 1,2,3 种颜色
                    int totalColors = which + 1;
                    // 启动异步递归拾取器
                    pickColors(context, totalColors, 0, new StringBuilder(), listener);
                }).show();
    }

    /**
     * 唤起文字颜色选择器
     * 强制限制为只能选取 1 种纯色。
     *
     * @param context  上下文
     * @param listener 选色完成后的回调
     */
    public static void showTextPicker(Context context, OnColorsPickedListener listener) {
        pickColors(context, 1, 0, new StringBuilder(), listener);
    }

    /**
     * 核心算法：通过递归链式调用，解决 Android Dialog 异步非阻塞导致的层叠问题。
     *
     * @param context  上下文
     * @param total    需要拾取的总颜色数量
     * @param current  当前已经拾取的数量
     * @param sb       用于拼接最终颜色字符串的容器 (引用传递)
     * @param listener 最终完成时的回调
     */
    private static void pickColors(Context context, int total, int current, StringBuilder sb, OnColorsPickedListener listener) {
        // 递归终止条件：当前已选数量达到目标总数
        if (current == total) {
            listener.onPicked(sb.toString());
            return;
        }

        // 动态构建弹窗标题
        String title = (total == 1) ? "选择颜色" : "请选择第 " + (current + 1) + " 个颜色";

        new ColorPickerDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton("确定", (ColorEnvelopeListener) (envelope, fromUser) -> {
                    // 多色拼接逻辑：非第一个颜色前加上逗号分隔符
                    if (current > 0) {
                        sb.append(",");
                    }

                    // 第三方库 getHexCode() 返回的是 AARRGGBB，不带 # 号
                    sb.append("#").append(envelope.getHexCode());

                    // 当前弹窗用户点击确定后，触发下一次递归，弹出下一个弹窗
                    pickColors(context, total, current + 1, sb, listener);
                })
                .setNegativeButton("取消", (dialogInterface, i) -> dialogInterface.dismiss())
                .attachAlphaSlideBar(true)       // 允许调节透明度通道 (Alpha)
                .attachBrightnessSlideBar(true)  // 允许调节明暗度
                .show();
    }
}