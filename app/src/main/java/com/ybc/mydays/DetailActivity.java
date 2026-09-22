package com.ybc.mydays;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import jp.wasabeef.glide.transformations.BlurTransformation;

public class DetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        View root = findViewById(R.id.detail_root);
        ImageView bgImage = findViewById(R.id.detail_bg_image);
        TextView tvName = findViewById(R.id.detail_name);
        TextView tvDays = findViewById(R.id.detail_days);

        // 1. 获取主页传过来的索引
        int index = getIntent().getIntExtra("DETAIL_INDEX", -1);
        if (index == -1) { finish(); return; }

        // 2. 从手机本地读取数据
        String jsonStr = getSharedPreferences("MyDaysPrefs", MODE_PRIVATE).getString("days_list", "[]");
        ArrayList<DaysData> list = new Gson().fromJson(jsonStr, new TypeToken<ArrayList<DaysData>>(){}.getType());
        DaysData data = list.get(index);

        // 3. 计算天数并展示文字
        tvName.setText(data.getMatterName());
        long finalDays = DaysUntil.calculateDays(data.getMatterYear(), data.getMatterMonth(), data.getMatterDay(), data.getRepeatIndex(), data.isCountUp());
        tvDays.setText(String.valueOf(Math.abs(finalDays))); // 只显示数字，最震撼

        // 4. 渲染颜色
        int txtColor = Color.parseColor(data.getTextColor() != null ? data.getTextColor() : "#000000");
        tvName.setTextColor(txtColor);
        tvDays.setTextColor(txtColor);

        // 5. 渲染背景 (优先显示图片，没有图片则显示纯色)
        if (data.getBgImageUri() != null && !data.getBgImageUri().isEmpty()) {
            bgImage.setVisibility(View.VISIBLE);
            bgImage.setImageURI(Uri.parse(data.getBgImageUri()));
        } else {
            root.setBackgroundColor(Color.parseColor(data.getBgColor() != null ? data.getBgColor() : "#FFFFFF"));
        }

        // 6. 渲染背景 (使用 Glide 完美加载图片并处理特效)
        if (data.getBgImageUri() != null && !data.getBgImageUri().isEmpty()) {
            bgImage.setVisibility(View.VISIBLE);

            if (data.isBlurBg()) {
                // 如果开启了虚化：使用 BlurTransformation
                // 参数 25 是模糊半径 (0-25)，参数 3 是缩放采样率 (值越大越模糊且性能越好)
                Glide.with(this)
                        .load(Uri.parse(data.getBgImageUri()))
                        .apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3)))
                        .into(bgImage);
            } else {
                // 正常加载原图
                Glide.with(this)
                        .load(Uri.parse(data.getBgImageUri()))
                        .into(bgImage);
            }
        } else {
            // 没有图片时使用纯色背景
            root.setBackgroundColor(Color.parseColor(data.getBgColor() != null ? data.getBgColor() : "#FFFFFF"));
        }
    }
}