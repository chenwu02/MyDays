package com.ybc.mydays;

import android.content.Context;
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
import java.util.Calendar;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import jp.wasabeef.glide.transformations.BlurTransformation;

public class DetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        View gradientBg = findViewById(R.id.detail_gradient_bg);
        ImageView bgImage = findViewById(R.id.detail_bg_image);
        TextView tvLabel = findViewById(R.id.detail_label);
        StrokeTextView tvName = findViewById(R.id.detail_name);
        StrokeTextView tvDays = findViewById(R.id.detail_days);
        TextView tvTargetDate = findViewById(R.id.detail_target_date);
        TextView tvUnit = findViewById(R.id.detail_days_unit);

        int index = getIntent().getIntExtra("DETAIL_INDEX", -1);
        if (index == -1) { finish(); return; }

        android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(index); // 这里的 index 就是发通知时用的 ID
        }

        String jsonStr = getSharedPreferences("MyDaysPrefs", MODE_PRIVATE).getString("days_list", "[]");
        DaysData data = ((ArrayList<DaysData>)new Gson().fromJson(jsonStr, new TypeToken<ArrayList<DaysData>>(){}.getType())).get(index);

        tvName.setText(data.getMatterName());
        long finalDays = DaysUntil.calculateDays(data.getMatterYear(), data.getMatterMonth(), data.getMatterDay(), data.getRepeatIndex(), data.isCountUp());

        // ======= 0天逻辑处理 =======
        if (finalDays == 0) {
            tvLabel.setVisibility(View.GONE);
            tvUnit.setVisibility(View.GONE);
            tvDays.setText("就是今天");
            tvDays.setTextSize(42);
        } else {
            tvLabel.setVisibility(View.VISIBLE);
            tvUnit.setVisibility(View.VISIBLE);
            tvDays.setText(String.valueOf(Math.abs(finalDays)));
            tvDays.setTextSize(80);

            // “还有”/“已经”/“已超”判断 (现已显示在标题后方)
            String labelText = data.isCountUp() ? "已经" : (finalDays < 0 && data.getRepeatIndex() == 0 ? "已超" : "还有");
            tvLabel.setText(labelText);
        }

        // 底部精确目标日期
        Calendar tDate = DaysUntil.getNextTargetDate(data.getMatterYear(), data.getMatterMonth(), data.getMatterDay(), data.getRepeatIndex(), data.isCountUp());
        tvTargetDate.setText(String.format("目标日: %d-%02d-%02d", tDate.get(Calendar.YEAR), tDate.get(Calendar.MONTH) + 1, tDate.get(Calendar.DAY_OF_MONTH)));

        // 颜色渲染
        int txtColor = Color.parseColor(data.getTextColor() != null ? data.getTextColor() : "#000000");
        tvName.setTextColor(txtColor); tvDays.setTextColor(txtColor);
        tvLabel.setTextColor(txtColor); tvUnit.setTextColor(txtColor); tvTargetDate.setTextColor(txtColor);

        // 背景渲染
        if (data.getBgImageUri() != null && !data.getBgImageUri().isEmpty()) {
            bgImage.setVisibility(View.VISIBLE);
            gradientBg.setVisibility(View.GONE);
            if (data.isBlurBg()) {
                Glide.with(this).load(Uri.parse(data.getBgImageUri())).apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3))).into(bgImage);
            } else {
                Glide.with(this).load(Uri.parse(data.getBgImageUri())).into(bgImage);
            }
        } else {
            bgImage.setVisibility(View.GONE);
            gradientBg.setVisibility(View.VISIBLE);
            // 使用全新的 BackgroundUtil 解析渲染背景
            BackgroundUtil.applyBackground(gradientBg, data.getBgColor(), "#FF9A9E,#FECFEF");
        }
    }
}