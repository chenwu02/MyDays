package com.ybc.mydays;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 主页面布局、管理应用全局配置与后台通知任务分发
 */
public class MainActivity extends AppCompatActivity {

    // 将硬编码提取为常量，防拼写错误
    private static final String PREF_DATA = "MyDaysPrefs";
    private static final String PREF_THEME = "MyDaysTheme";
    private static final int REQ_CODE_NOTIFY = 100;

    private RecyclerView recyclerView;
    private DaysAdapter adapter;
    private List<DaysData> dataList = new ArrayList<>();

    private boolean isSortingMode = false;
    private Button btnSort;

    /**
     * 主页背景图片裁剪器回调
     * 裁剪成功后保存 URI 并清空纯色配置，随后刷新主题
     */
    private final androidx.activity.result.ActivityResultLauncher<CropImageContractOptions> mainCropLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null) {
                    saveThemeSettings(result.getUriContent().toString(), null);
                    applyMainTheme();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //主页面逻辑
        initViews();
        setupRecyclerView();
        setupListeners();
        initSystemServices();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyMainTheme();
        loadLocalData();
    }

    /**
     * 绑定所有的视图组件
     */
    private void initViews() {
        recyclerView = findViewById(R.id.list_home);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        btnSort = findViewById(R.id.sort);
    }

    /**
     * 配置列表以及拖拽排序逻辑
     */
    private void setupRecyclerView() {
        // Adapter 仅在此处实例化一次，提升性能
        adapter = new DaysAdapter(dataList);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                int dragFlags = isSortingMode ? (ItemTouchHelper.UP | ItemTouchHelper.DOWN) : 0;
                int swipeFlags = isSortingMode ? 0 : (ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                int from = vh.getAdapterPosition();
                int to = target.getAdapterPosition();
                Collections.swap(dataList, from, to);
                adapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int position = vh.getAdapterPosition();
                if (direction == ItemTouchHelper.LEFT) {
                    dataList.remove(position);
                    adapter.notifyItemRemoved(position);
                    saveDataToLocal();
                } else if (direction == ItemTouchHelper.RIGHT) {
                    adapter.notifyItemChanged(position); // 让滑出去的卡片弹回原位
                    Intent intent = new Intent(MainActivity.this, AddActivity.class);
                    intent.putExtra("EDIT_INDEX", position);
                    startActivity(intent);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                if (isSortingMode) {
                    saveDataToLocal();
                }
            }
        });
        touchHelper.attachToRecyclerView(recyclerView);
    }

    /**
     * 配置各种点击事件
     */
    private void setupListeners() {
        findViewById(R.id.add).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddActivity.class)));

        findViewById(R.id.btn_theme).setOnClickListener(v ->
                showThemeSettingsDialog());

        btnSort.setOnClickListener(v -> {
            isSortingMode = !isSortingMode;
            btnSort.setText(isSortingMode ? "完成排序" : "排序");
            if (!isSortingMode) {
                saveDataToLocal();
            }
        });
    }

    /**
     * 初始化系统服务：通知权限、通道以及精准闹钟引擎
     */
    private void initSystemServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_CODE_NOTIFY);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "MY_DAYS_CHANNEL",
                    "倒数日提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("用于显示事件即将到来的提醒");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        AlarmHelper.scheduleNextMidnightAlarm(this);
    }

    /**
     * 从本地读取事件列表并刷新 UI
     */
    private void loadLocalData() {
        SharedPreferences prefs = getSharedPreferences(PREF_DATA, MODE_PRIVATE);
        String jsonStr = prefs.getString("days_list", "[]");
        List<DaysData> savedList = new Gson().fromJson(jsonStr, new TypeToken<ArrayList<DaysData>>() {}.getType());

        dataList.clear(); // 清空旧数据
        if (savedList != null) {
            dataList.addAll(savedList);
        }
        adapter.notifyDataSetChanged(); // 通知 Adapter 数据已改变，让其自行渲染
    }

    /**
     * 将当前列表数据持久化到本地
     */
    private void saveDataToLocal() {
        if (dataList == null) return;
        getSharedPreferences(PREF_DATA, MODE_PRIVATE).edit()
                .putString("days_list", new Gson().toJson(dataList))
                .apply();

        // 通知桌面小组件数据发生删除或排序
        MyDaysWidgetProvider.notifyGlobalUpdate(this);
    }

    /**
     * 显示主页背景设置弹窗
     */
    private void showThemeSettingsDialog() {
        String[] options = {"从相册选图片", "预设渐变/纯色", "调色盘自定义"};
        new AlertDialog.Builder(this)
                .setTitle("设置主页背景")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        launchImageCropper();
                    } else if (which == 1) {
                        showPresetThemeDialog();
                    } else if (which == 2) {
                        CustomColorPickerHelper.showBackgroundPicker(this, colorResult -> {
                            saveThemeSettings(null, colorResult);
                            applyMainTheme();
                        });
                    }
                }).show();
    }

    /**
     * 调起图片裁剪器，根据设备显示比例进行裁剪
     */
    private void launchImageCropper() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        CropImageOptions cropOptions = new CropImageOptions();
        cropOptions.imageSourceIncludeGallery = true;
        cropOptions.imageSourceIncludeCamera = false;
        cropOptions.fixAspectRatio = true;
        cropOptions.aspectRatioX = metrics.widthPixels;
        cropOptions.aspectRatioY = metrics.heightPixels;

        mainCropLauncher.launch(new CropImageContractOptions(null, cropOptions));
    }

    /**
     * 显示预设背景选择弹窗
     */
    private void showPresetThemeDialog() {
        String[] names = {"暗夜黑", "落日橘 (默认)", "深海蓝", "蜜桃粉", "青翠自然"};
        String[] codes = {"#222222", "#ED8F03,#FFB75E", "#051937,#004D7A,#008793", "#FF9A9E,#FECFEF", "#11998E,#38EF7D"};

        new AlertDialog.Builder(this)
                .setTitle("预设背景")
                .setItems(names, (d, w) -> {
                    saveThemeSettings(null, codes[w]);
                    applyMainTheme();
                }).show();
    }

    /**
     * 保存主页主题配置
     * @param imgUri 图片地址，若无则传 null
     * @param colorHex 颜色十六进制代码，若无则传 null
     */
    private void saveThemeSettings(String imgUri, String colorHex) {
        getSharedPreferences(PREF_THEME, MODE_PRIVATE).edit()
                .putString("main_bg_img", imgUri)
                .putString("main_bg_color", colorHex)
                .apply();
    }

    /**
     * 解析本地配置并渲染主页背景
     */
    private void applyMainTheme() {
        SharedPreferences prefs = getSharedPreferences(PREF_THEME, MODE_PRIVATE);
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
            BackgroundUtil.applyBackground(viewGradient, bgColor, "#FF9A9E,#FECFEF");
        }
    }
}