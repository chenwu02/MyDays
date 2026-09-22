package com.ybc.mydays;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<DaysData> dataList;
    private DaysAdapter adapter;
    private boolean isSortingMode = false;

    // 主页图片选择器
    private final androidx.activity.result.ActivityResultLauncher<com.canhub.cropper.CropImageContractOptions> mainCropLauncher =
            registerForActivityResult(new com.canhub.cropper.CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    SharedPreferences prefs = getSharedPreferences("MyDaysTheme", MODE_PRIVATE);
                    prefs.edit().putString("main_bg_img", result.getUriContent().toString())
                            .putString("main_bg_color", null).apply();
                    applyMainTheme();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.list_home);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ImageButton btnAdd = findViewById(R.id.add);
        btnAdd.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddActivity.class)));

        ImageButton btnTheme = findViewById(R.id.btn_theme);
        btnTheme.setOnClickListener(v -> showThemeSettingsDialog());

        Button btnSort = findViewById(R.id.sort);
        btnSort.setOnClickListener(v -> {
            isSortingMode = !isSortingMode;
            if (isSortingMode) {
                btnSort.setText("完成排序");
            } else {
                btnSort.setText("排序");
                saveDataToLocal();
            }
        });

        // 拖拽逻辑 (保持原样)
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = isSortingMode ? (ItemTouchHelper.UP | ItemTouchHelper.DOWN) : 0;
                int swipeFlags = isSortingMode ? 0 : (ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
                return makeMovementFlags(dragFlags, swipeFlags);
            }
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                adapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (direction == ItemTouchHelper.LEFT) {
                    dataList.remove(position);
                    adapter.notifyItemRemoved(position);
                    saveDataToLocal();
                } else if (direction == ItemTouchHelper.RIGHT) {
                    adapter.notifyItemChanged(position);
                    Intent intent = new Intent(MainActivity.this, AddActivity.class);
                    intent.putExtra("EDIT_INDEX", position);
                    startActivity(intent);
                }
            }
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (isSortingMode) saveDataToLocal();
            }
        });
        touchHelper.attachToRecyclerView(recyclerView);

        // 1. 申请通知权限 (针对 Android 13 及以上)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        // 2. 创建通知渠道 (针对 Android 8.0 及以上)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "MY_DAYS_CHANNEL",
                    "倒数日提醒",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("用于显示事件即将到来的提醒");
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // 3. 启动精准闹钟引擎 (每天 00:01 触发)
        AlarmHelper.scheduleNextMidnightAlarm(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyMainTheme(); // 每次回主页重新渲染背景

        SharedPreferences prefs = getSharedPreferences("MyDaysPrefs", MODE_PRIVATE);
        String jsonStr = prefs.getString("days_list", "[]");
        Type type = new TypeToken<ArrayList<DaysData>>() {}.getType();
        dataList = new Gson().fromJson(jsonStr, type);
        adapter = new DaysAdapter(dataList);
        recyclerView.setAdapter(adapter);
    }

    private void saveDataToLocal() {
        if (dataList == null) return;
        getSharedPreferences("MyDaysPrefs", MODE_PRIVATE).edit()
                .putString("days_list", new Gson().toJson(dataList)).apply();
    }

    private void showThemeSettingsDialog() {
        String[] options = {"从相册选图片", "预设渐变/纯色", "调色盘自定义"};
        new AlertDialog.Builder(this).setTitle("设置主页背景").setItems(options, (dialog, which) -> {
            SharedPreferences prefs = getSharedPreferences("MyDaysTheme", MODE_PRIVATE);
            if (which == 0) {
                // 1. 实时获取当前设备的屏幕宽高像素
                android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                int screenWidth = metrics.widthPixels;
                int screenHeight = metrics.heightPixels;

                // 2. 将屏幕真实宽高作为裁切比例
                com.canhub.cropper.CropImageOptions cropOptions = new com.canhub.cropper.CropImageOptions();
                cropOptions.imageSourceIncludeGallery = true;
                cropOptions.imageSourceIncludeCamera = false;
                cropOptions.fixAspectRatio = true;
                cropOptions.aspectRatioX = screenWidth;
                cropOptions.aspectRatioY = screenHeight;

                mainCropLauncher.launch(new com.canhub.cropper.CropImageContractOptions(null, cropOptions));
            } else if (which == 1) {
                String[] names = {"暗夜黑", "落日橘 (默认)", "深海蓝", "蜜桃粉", "青翠自然"};
                String[] codes = {"#222222", "#ED8F03,#FFB75E", "#051937,#004D7A,#008793", "#FF9A9E,#FECFEF", "#11998E,#38EF7D"};
                new AlertDialog.Builder(this).setTitle("预设背景").setItems(names, (d, w) -> {
                    prefs.edit().putString("main_bg_color", codes[w]).putString("main_bg_img", null).apply();
                    applyMainTheme();
                }).show();
            } else if (which == 2) {
                CustomColorPickerHelper.showBackgroundPicker(this, colorResult -> {
                    prefs.edit().putString("main_bg_color", colorResult).putString("main_bg_img", null).apply();
                    applyMainTheme();
                });
            }
        }).show();
    }

    private void applyMainTheme() {
        SharedPreferences prefs = getSharedPreferences("MyDaysTheme", MODE_PRIVATE);
        String bgImg = prefs.getString("main_bg_img", null);
        String bgColor = prefs.getString("main_bg_color", null);

        ImageView ivBg = findViewById(R.id.main_bg_image);
        View viewGradient = findViewById(R.id.main_gradient_bg);

        if (bgImg != null) {
            ivBg.setVisibility(View.VISIBLE);
            viewGradient.setVisibility(View.GONE);
            Glide.with(this).load(Uri.parse(bgImg)).into(ivBg);
        } else {
            ivBg.setVisibility(View.GONE);
            viewGradient.setVisibility(View.VISIBLE);
            BackgroundUtil.applyBackground(viewGradient, bgColor, "#ED8F03,#FFB75E");
        }
    }
}