package com.ybc.mydays;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jp.wasabeef.glide.transformations.BlurTransformation;

/**
 * 事件详情展示页
 * 负责渲染倒数日的核心视觉（液态玻璃背景、沉浸式字体色、特殊状态排版），并在进入时自动清理相关系统通知。
 */
public class DetailActivity extends AppCompatActivity {

    // 常量定义
    private static final String PREF_NAME = "MyDaysPrefs";
    private static final String KEY_DAYS_LIST = "days_list";
    private static final int TEXT_SIZE_TODAY = 42;
    private static final int TEXT_SIZE_NORMAL = 80;

    // UI 控件
    private View gradientBg;
    private ImageView bgImage;
    private TextView tvLabel;
    private TextView tvTargetDate;
    private TextView tvUnit;
    private StrokeTextView tvName;
    private StrokeTextView tvDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        initViews();
        processIntentData();
    }

    /**
     * 绑定所有的视图组件
     */
    private void initViews() {
        gradientBg = findViewById(R.id.detail_gradient_bg);
        bgImage = findViewById(R.id.detail_bg_image);
        tvLabel = findViewById(R.id.detail_label);
        tvName = findViewById(R.id.detail_name);
        tvDays = findViewById(R.id.detail_days);
        tvTargetDate = findViewById(R.id.detail_target_date);
        tvUnit = findViewById(R.id.detail_days_unit);
    }

    /**
     * 解析 Intent 携带的索引，清理通知，并加载对应的数据进行渲染
     */
    private void processIntentData() {
        int index = getIntent().getIntExtra("DETAIL_INDEX", -1);
        if (index == -1) {
            finish();
            return;
        }

        clearAssociatedNotification(index);
        loadAndRenderData(index);
    }

    /**
     * 清除驻留在系统通知栏中的对应提醒（消除应用角标/小红点）
     * @param notificationId 发送通知时使用的索引 ID
     */
    private void clearAssociatedNotification(int notificationId) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(notificationId);
        }
    }

    /**
     * 从本地读取数据并执行安全校验，随后分发给各个渲染模块
     */
    private void loadAndRenderData(int index) {
        String jsonStr = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(KEY_DAYS_LIST, "[]");
        Type listType = new TypeToken<ArrayList<DaysData>>() {}.getType();
        List<DaysData> dataList = new Gson().fromJson(jsonStr, listType);

        // 安全边界校验：防止因脏数据或并发修改导致的数组越界崩溃
        if (dataList == null || index < 0 || index >= dataList.size()) {
            Toast.makeText(this, "数据加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DaysData data = dataList.get(index);
        tvName.setText(data.getMatterName());

        // 计算真实天数与精确目标日
        long finalDays = DaysUntil.calculateDays(
                data.getMatterYear(), data.getMatterMonth(), data.getMatterDay(),
                data.getRepeatIndex(), data.isCountUp()
        );
        Calendar targetDate = DaysUntil.getNextTargetDate(
                data.getMatterYear(), data.getMatterMonth(), data.getMatterDay(),
                data.getRepeatIndex(), data.isCountUp()
        );

        // 分发渲染任务
        renderDaysText(data, finalDays);
        renderTargetDateText(targetDate);
        renderColors(data);
        renderBackground(data);
    }

    /**
     * 渲染天数与前缀标签文本
     * 业务规则：当天数为 0 时，触发“当日专属视图”，隐藏多余单位标签，展示大字号提示语。
     */
    private void renderDaysText(DaysData data, long finalDays) {
        if (finalDays == 0) {
            tvLabel.setVisibility(View.GONE);
            tvUnit.setVisibility(View.GONE);
            tvDays.setText("就是今天");
            tvDays.setTextSize(TEXT_SIZE_TODAY);
        } else {
            tvLabel.setVisibility(View.VISIBLE);
            tvUnit.setVisibility(View.VISIBLE);
            tvDays.setText(String.valueOf(Math.abs(finalDays)));
            tvDays.setTextSize(TEXT_SIZE_NORMAL);

            // 动态决定前缀修饰词
            String labelText = data.isCountUp() ? "已经" : (finalDays < 0 && data.getRepeatIndex() == DaysUntil.REPEAT_NONE ? "已超" : "还有");
            tvLabel.setText(labelText);
        }
    }

    /**
     * 格式化并渲染底部精确目标日期
     */
    private void renderTargetDateText(Calendar targetDate) {
        tvTargetDate.setText(String.format("目标日: %d-%02d-%02d",
                targetDate.get(Calendar.YEAR),
                targetDate.get(Calendar.MONTH) + 1,
                targetDate.get(Calendar.DAY_OF_MONTH)));
    }

    /**
     * 统一应用用户自定义的文字颜色
     */
    private void renderColors(DaysData data) {
        int txtColor = Color.parseColor(data.getTextColor() != null ? data.getTextColor() : "#000000");
        tvName.setTextColor(txtColor);
        tvDays.setTextColor(txtColor);
        tvLabel.setTextColor(txtColor);
        tvUnit.setTextColor(txtColor);
        tvTargetDate.setTextColor(txtColor);
    }

    /**
     * 解析用户配置，应用纯色渐变或通过 Glide 渲染自定义图片背景（包含毛玻璃虚化处理）
     */
    private void renderBackground(DaysData data) {
        String bgUri = data.getBgImageUri();

        if (bgUri != null && !bgUri.isEmpty()) {
            // 图片模式
            bgImage.setVisibility(View.VISIBLE);
            gradientBg.setVisibility(View.GONE);

            if (data.isBlurBg()) {
                Glide.with(this)
                        .load(Uri.parse(bgUri))
                        .apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3)))
                        .into(bgImage);
            } else {
                Glide.with(this)
                        .load(Uri.parse(bgUri))
                        .into(bgImage);
            }
        } else {
            // 纯色/渐变模式
            bgImage.setVisibility(View.GONE);
            gradientBg.setVisibility(View.VISIBLE);
            BackgroundUtil.applyBackground(gradientBg, data.getBgColor(), "#FF9A9E,#FECFEF");
        }
    }
}