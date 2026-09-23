package com.ybc.mydays;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 添加与编辑事件页面
 * 负责处理新倒数日的创建以及已有倒数日的修改，包括日期计算与外观自定义配置。
 */
public class AddActivity extends AppCompatActivity {

    // 常量定义
    private static final String PREF_NAME = "MyDaysPrefs";
    private static final String KEY_DAYS_LIST = "days_list";
    private static final String[] REPEAT_OPTIONS = {"不重复", "每年一次", "每月一次"};

    // 状态变量
    private int selectedYear = 0;
    private int selectedMonth = 0;
    private int selectedDay = 0;
    private int selectedRepeatIndex = 0;
    private int editIndex = -1;

    private String currentBgImageUri = null;
    private String currentBgColor = null;
    private String currentTextColor = "#000000";

    // UI 控件
    private EditText etName;
    private TextView tvSelectedDate;
    private TextView tvSelectedRepeat;
    private Switch switchCountUp, switchReminder, switchBlurBg;
    private LinearLayout containerReminder, layoutBgImg, layoutBgColor;
    private CheckBox cbRemind0, cbRemind1, cbRemind3, cbRemind7;
    private RadioGroup rgBgType;
    private RadioButton rbBgImage, rbBgColor;

    /**
     * 图片裁剪器回调
     */
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null) {
                    currentBgImageUri = result.getUriContent().toString();
                    currentBgColor = null;
                    Toast.makeText(this, "图片已保存", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        // 标准的生命周期职责划分
        initViews();
        setupListeners();
        handleIntentData();
    }

    /**
     * 初始化视图控件，建立 XML 与 Java 对象的绑定
     */
    private void initViews() {
        etName = findViewById(R.id.renameOfAdd);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvSelectedRepeat = findViewById(R.id.tv_selected_repeat);

        switchCountUp = findViewById(R.id.switch_count_up);
        switchReminder = findViewById(R.id.switch_reminder);
        switchBlurBg = findViewById(R.id.switch_blur_bg);

        containerReminder = findViewById(R.id.container_reminder_details);
        cbRemind0 = findViewById(R.id.cb_remind_0);
        cbRemind1 = findViewById(R.id.cb_remind_1);
        cbRemind3 = findViewById(R.id.cb_remind_3);
        cbRemind7 = findViewById(R.id.cb_remind_7);

        rgBgType = findViewById(R.id.rg_bg_type);
        rbBgImage = findViewById(R.id.rb_bg_image);
        rbBgColor = findViewById(R.id.rb_bg_color);
        layoutBgImg = findViewById(R.id.layout_bg_image);
        layoutBgColor = findViewById(R.id.layout_bg_color);
    }

    /**
     * 解析传递意图 (Intent)
     * 判断是“新建”操作还是“编辑”操作，并作相应的初始化
     */
    private void handleIntentData() {
        editIndex = getIntent().getIntExtra("EDIT_INDEX", -1);

        if (editIndex != -1) {
            loadEditData();
        } else {
            // 新建模式：默认选中纯色背景，隐藏图片配置区
            rbBgColor.setChecked(true);
            layoutBgImg.setVisibility(View.GONE);
            layoutBgColor.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 读取本地数据并回填到对应的输入框和开关中
     */
    private void loadEditData() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_DAYS_LIST, "[]");
        Type listType = new TypeToken<ArrayList<DaysData>>() {}.getType();
        List<DaysData> currentList = new Gson().fromJson(jsonStr, listType);

        if (currentList == null || editIndex >= currentList.size()) return;

        DaysData data = currentList.get(editIndex);

        etName.setText(data.getMatterName());

        selectedYear = data.getMatterYear();
        selectedMonth = data.getMatterMonth();
        selectedDay = data.getMatterDay();
        tvSelectedDate.setText(String.format("%d年%d月%d日 >", selectedYear, selectedMonth, selectedDay));
        tvSelectedDate.setTextColor(0xFFFFFFFF);

        selectedRepeatIndex = data.getRepeatIndex();
        tvSelectedRepeat.setText(String.format("%s >", REPEAT_OPTIONS[selectedRepeatIndex]));
        tvSelectedRepeat.setTextColor(0xFFFFFFFF);

        switchCountUp.setChecked(data.isCountUp());
        switchBlurBg.setChecked(data.isBlurBg());

        // 回填提醒设置
        boolean isRemindOn = data.isReminderOn();
        switchReminder.setChecked(isRemindOn);
        if (isRemindOn) {
            containerReminder.setVisibility(View.VISIBLE);
            cbRemind0.setChecked(data.isRemindDayOf());
            cbRemind1.setChecked(data.isRemind1Day());
            cbRemind3.setChecked(data.isRemind3Days());
            cbRemind7.setChecked(data.isRemind7Days());
        }

        // 回填外观配置
        currentBgImageUri = data.getBgImageUri();
        currentBgColor = data.getBgColor();
        currentTextColor = data.getTextColor() != null ? data.getTextColor() : "#FFFFFF";

        if (currentBgImageUri != null && !currentBgImageUri.isEmpty()) {
            rbBgImage.setChecked(true);
            layoutBgImg.setVisibility(View.VISIBLE);
            layoutBgColor.setVisibility(View.GONE);
        } else {
            rbBgColor.setChecked(true);
            layoutBgImg.setVisibility(View.GONE);
            layoutBgColor.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 绑定所有的用户交互事件（点击、开关切换）
     */
    private void setupListeners() {
        findViewById(R.id.backOfAdd).setOnClickListener(v -> finish());
        findViewById(R.id.saveOfAdd).setOnClickListener(v -> saveEventData());

        findViewById(R.id.row_target_date).setOnClickListener(v -> showDatePicker());
        findViewById(R.id.row_repeat).setOnClickListener(v -> showRepeatDialog());

        // 背景模式切换逻辑
        rgBgType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isImageMode = (checkedId == R.id.rb_bg_image);
            layoutBgImg.setVisibility(isImageMode ? View.VISIBLE : View.GONE);
            layoutBgColor.setVisibility(isImageMode ? View.GONE : View.VISIBLE);

            if (isImageMode) currentBgColor = null;
            else currentBgImageUri = null;
        });

        // 提醒开关联动逻辑：开启时默认勾选“当天”
        switchReminder.setOnCheckedChangeListener((btn, isChecked) -> {
            containerReminder.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && !cbRemind0.isChecked() && !cbRemind1.isChecked()
                    && !cbRemind3.isChecked() && !cbRemind7.isChecked()) {
                cbRemind0.setChecked(true);
            }
        });

        setupAppearanceListeners();
    }

    /**
     * 配置外观区域（图片裁剪、颜色选择）的监听器
     */
    private void setupAppearanceListeners() {
        findViewById(R.id.btn_pick_image).setOnClickListener(v -> launchImageCropper());

        findViewById(R.id.btn_preset_bg_color).setOnClickListener(v -> showPresetColorDialog(true));
        findViewById(R.id.btn_custom_bg_color).setOnClickListener(v ->
                CustomColorPickerHelper.showBackgroundPicker(this, colorResult -> {
                    currentBgColor = colorResult;
                    Toast.makeText(this, "自定义背景已应用", Toast.LENGTH_SHORT).show();
                }));

        findViewById(R.id.btn_preset_txt_color).setOnClickListener(v -> showPresetColorDialog(false));
        findViewById(R.id.btn_custom_txt_color).setOnClickListener(v ->
                CustomColorPickerHelper.showTextPicker(this, colorResult -> {
                    currentTextColor = colorResult;
                    Toast.makeText(this, "自定义文字色已应用", Toast.LENGTH_SHORT).show();
                }));
    }

    /**
     * 对话框与辅助方法
     */
    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedYear = year;
            selectedMonth = month + 1;
            selectedDay = dayOfMonth;
            tvSelectedDate.setText(String.format("%d年%d月%d日 >", selectedYear, selectedMonth, selectedDay));
            tvSelectedDate.setTextColor(0xFFFFFFFF);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showRepeatDialog() {
        new AlertDialog.Builder(this)
                .setTitle("选择重复周期")
                .setSingleChoiceItems(REPEAT_OPTIONS, selectedRepeatIndex, (dialog, which) -> {
                    selectedRepeatIndex = which;
                    tvSelectedRepeat.setText(String.format("%s >", REPEAT_OPTIONS[which]));
                    tvSelectedRepeat.setTextColor(0xFFFFFFFF);
                    dialog.dismiss();
                }).show();
    }

    private void launchImageCropper() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        CropImageOptions options = new CropImageOptions();
        options.imageSourceIncludeGallery = true;
        options.imageSourceIncludeCamera = false;
        options.fixAspectRatio = true;
        options.aspectRatioX = metrics.widthPixels;
        options.aspectRatioY = metrics.heightPixels;
        cropImageLauncher.launch(new CropImageContractOptions(null, options));
    }

    private void showPresetColorDialog(boolean isBackground) {
        String[] bgNames = {"暗夜黑", "纯净白", "清爽蓝", "落日橘 (渐变)", "深海蓝 (渐变)", "蜜桃粉 (渐变)", "青翠自然 (渐变)"};
        String[] bgCodes = {"#222222", "#FFFFFF", "#E3F2FD", "#ED8F03,#FFB75E", "#051937,#004D7A,#008793", "#FF9A9E,#FECFEF", "#11998E,#38EF7D"};

        String[] txtNames = {"经典黑", "纯净白", "高级灰", "警告红", "深海蓝", "樱花粉", "尊贵金"};
        String[] txtCodes = {"#000000", "#FFFFFF", "#666666", "#FF3B30", "#007AFF", "#FF2D55", "#FFD700"};

        new AlertDialog.Builder(this)
                .setTitle(isBackground ? "选择预设背景" : "选择文字颜色")
                .setItems(isBackground ? bgNames : txtNames, (dialog, which) -> {
                    if (isBackground) currentBgColor = bgCodes[which];
                    else currentTextColor = txtCodes[which];
                    Toast.makeText(this, "已应用", Toast.LENGTH_SHORT).show();
                }).show();
    }

    /**
     * 校验输入合法性，构建 DaysData 对象，并保存到本地 SharedPreferences
     */
    private void saveEventData() {
        String eventName = etName.getText().toString().trim();

        // 1. 数据合法性校验
        if (eventName.isEmpty()) {
            Toast.makeText(this, "请输入事件名称！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedYear == 0) {
            Toast.makeText(this, "请选择目标日期！", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 封装数据对象
        DaysData newData = new DaysData(
                eventName, selectedYear, selectedMonth, selectedDay, selectedRepeatIndex,
                switchCountUp.isChecked(), switchReminder.isChecked(),
                cbRemind0.isChecked(), cbRemind1.isChecked(), cbRemind3.isChecked(), cbRemind7.isChecked(),
                currentBgImageUri, currentBgColor, currentTextColor, switchBlurBg.isChecked()
        );

        // 3. 读取本地列表
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<DaysData>>() {}.getType();
        List<DaysData> list = gson.fromJson(prefs.getString(KEY_DAYS_LIST, "[]"), listType);

        if (list == null) list = new ArrayList<>();

        // 4. 更新或新增数据
        if (editIndex != -1) {
            list.set(editIndex, newData);
        } else {
            list.add(0, newData); // 新建事件置顶
        }

        // 5. 序列化并保存
        prefs.edit().putString(KEY_DAYS_LIST, gson.toJson(list)).apply();
        finish();
    }
}