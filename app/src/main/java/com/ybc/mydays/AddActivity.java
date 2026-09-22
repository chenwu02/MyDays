package com.ybc.mydays;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class AddActivity extends AppCompatActivity {

    private int selectedYear = 0, selectedMonth = 0, selectedDay = 0;
    private int selectedRepeatIndex = 0;
    private final String[] repeatOptions = {"不重复", "每年一次", "每月一次"};
    private int editIndex = -1;

    private String currentBgImageUri = null;
    private String currentBgColor = null;
    private String currentTextColor = null; // 默认白字

    private final androidx.activity.result.ActivityResultLauncher<com.canhub.cropper.CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new com.canhub.cropper.CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    currentBgImageUri = result.getUriContent().toString();
                    currentBgColor = null;
                    Toast.makeText(this, "图片已保存", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        // 绑定各种基础控件
        ImageButton btnBack = findViewById(R.id.backOfAdd);
        EditText etName = findViewById(R.id.renameOfAdd);
        Button btnSave = findViewById(R.id.saveOfAdd);
        RelativeLayout rowTargetDate = findViewById(R.id.row_target_date);
        TextView tvSelectedDate = findViewById(R.id.tv_selected_date);
        RelativeLayout rowRepeat = findViewById(R.id.row_repeat);
        TextView tvSelectedRepeat = findViewById(R.id.tv_selected_repeat);

        Switch switchCountUp = findViewById(R.id.switch_count_up);
        Switch switchReminder = findViewById(R.id.switch_reminder);
        LinearLayout containerReminder = findViewById(R.id.container_reminder_details);
        CheckBox cbRemind0 = findViewById(R.id.cb_remind_0);
        CheckBox cbRemind1 = findViewById(R.id.cb_remind_1);
        CheckBox cbRemind3 = findViewById(R.id.cb_remind_3);
        CheckBox cbRemind7 = findViewById(R.id.cb_remind_7);

        // 绑定外观控件
        RadioGroup rgBgType = findViewById(R.id.rg_bg_type);
        RadioButton rbBgImage = findViewById(R.id.rb_bg_image);
        RadioButton rbBgColor = findViewById(R.id.rb_bg_color);
        LinearLayout layoutBgImg = findViewById(R.id.layout_bg_image);
        LinearLayout layoutBgColor = findViewById(R.id.layout_bg_color);

        Button btnPickImage = findViewById(R.id.btn_pick_image);
        Button btnPresetBgColor = findViewById(R.id.btn_preset_bg_color);
        Button btnCustomBgColor = findViewById(R.id.btn_custom_bg_color);
        Button btnPresetTxtColor = findViewById(R.id.btn_preset_txt_color);
        Button btnCustomTxtColor = findViewById(R.id.btn_custom_txt_color);
        Switch switchBlurBg = findViewById(R.id.switch_blur_bg);

        // 回填数据 (编辑模式)
        editIndex = getIntent().getIntExtra("EDIT_INDEX", -1);
        if (editIndex != -1) {
            String jsonStr = getSharedPreferences("MyDaysPrefs", MODE_PRIVATE).getString("days_list", "[]");
            java.util.ArrayList<DaysData> currentList = new com.google.gson.Gson().fromJson(jsonStr, new com.google.gson.reflect.TypeToken<java.util.ArrayList<DaysData>>() {}.getType());
            if (currentList != null && editIndex < currentList.size()) {
                DaysData editData = currentList.get(editIndex);
                etName.setText(editData.getMatterName());
                selectedYear = editData.getMatterYear(); selectedMonth = editData.getMatterMonth(); selectedDay = editData.getMatterDay();
                tvSelectedDate.setText(selectedYear + "年" + selectedMonth + "月" + selectedDay + "日 >");
                tvSelectedDate.setTextColor(0xFFFFFFFF); // 强制白色
                selectedRepeatIndex = editData.getRepeatIndex();
                tvSelectedRepeat.setText(repeatOptions[selectedRepeatIndex] + " >");
                tvSelectedRepeat.setTextColor(0xFFFFFFFF);
                switchCountUp.setChecked(editData.isCountUp());
                switchReminder.setChecked(editData.isReminderOn());
                if (editData.isReminderOn()) {
                    containerReminder.setVisibility(View.VISIBLE);
                    cbRemind0.setChecked(editData.isRemindDayOf()); cbRemind1.setChecked(editData.isRemind1Day());
                    cbRemind3.setChecked(editData.isRemind3Days()); cbRemind7.setChecked(editData.isRemind7Days());
                }
                currentBgImageUri = editData.getBgImageUri();
                currentBgColor = editData.getBgColor();
                currentTextColor = editData.getTextColor();
                switchBlurBg.setChecked(editData.isBlurBg());

                if (currentBgImageUri != null && !currentBgImageUri.isEmpty()) {
                    rbBgImage.setChecked(true);
                    layoutBgImg.setVisibility(View.VISIBLE); // 强制显示图片控制区
                    layoutBgColor.setVisibility(View.GONE);
                } else {
                    rbBgColor.setChecked(true);
                    layoutBgImg.setVisibility(View.GONE);
                    layoutBgColor.setVisibility(View.VISIBLE); // 强制显示颜色控制区
                }
            }
        } else {
            rbBgColor.setChecked(true); // 新建默认选中颜色
            layoutBgImg.setVisibility(View.GONE);
            layoutBgColor.setVisibility(View.VISIBLE); // 新建时强制显示颜色控制区
        }

        // 外观 RadioGroup 切换逻辑
        rgBgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_bg_image) {
                layoutBgImg.setVisibility(View.VISIBLE); layoutBgColor.setVisibility(View.GONE);
                currentBgColor = null;
            } else {
                layoutBgImg.setVisibility(View.GONE); layoutBgColor.setVisibility(View.VISIBLE);
                currentBgImageUri = null;
            }
        });

        btnBack.setOnClickListener(v -> finish());
        rowTargetDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedYear = year; selectedMonth = month + 1; selectedDay = dayOfMonth;
                tvSelectedDate.setText(selectedYear + "年" + selectedMonth + "月" + selectedDay + "日 >");
                tvSelectedDate.setTextColor(0xFFFFFFFF);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        rowRepeat.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("选择重复周期")
                .setSingleChoiceItems(repeatOptions, selectedRepeatIndex, (dialog, which) -> {
                    selectedRepeatIndex = which;
                    tvSelectedRepeat.setText(repeatOptions[which] + " >");
                    tvSelectedRepeat.setTextColor(0xFFFFFFFF);
                    dialog.dismiss();
                }).show());

        switchReminder.setOnCheckedChangeListener((btn, isChecked) -> {
            containerReminder.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && !cbRemind0.isChecked() && !cbRemind1.isChecked() && !cbRemind3.isChecked() && !cbRemind7.isChecked()) {
                cbRemind0.setChecked(true);
            }
        });

        // ================= 外观按钮事件 =================
        btnPickImage.setOnClickListener(v -> {
            // 1. 实时获取当前设备的屏幕宽高像素
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;

            // 2. 将屏幕真实宽高作为裁切比例
            com.canhub.cropper.CropImageOptions options = new com.canhub.cropper.CropImageOptions();
            options.imageSourceIncludeGallery = true;
            options.imageSourceIncludeCamera = false;
            options.fixAspectRatio = true;
            options.aspectRatioX = screenWidth;   // 动态宽度比例
            options.aspectRatioY = screenHeight;  // 动态高度比例

            cropImageLauncher.launch(new com.canhub.cropper.CropImageContractOptions(null, options));
        });

        // 预设背景色
        String[] presetBgNames = {"暗夜黑", "纯净白", "清爽蓝", "落日橘 (渐变)", "深海蓝 (渐变)", "蜜桃粉 (渐变)", "青翠自然 (渐变)"};
        String[] presetBgCodes = {"#222222", "#FFFFFF", "#E3F2FD", "#ED8F03,#FFB75E", "#051937,#004D7A,#008793", "#FF9A9E,#FECFEF", "#11998E,#38EF7D"};
        btnPresetBgColor.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("选择预设背景").setItems(presetBgNames, (dialog, which) -> {
            currentBgColor = presetBgCodes[which];
            Toast.makeText(this, "已应用", Toast.LENGTH_SHORT).show();
        }).show());

        // 第三方库自定义背景色 (支持多色渐变)
        btnCustomBgColor.setOnClickListener(v -> CustomColorPickerHelper.showBackgroundPicker(this, colorResult -> {
            currentBgColor = colorResult;
            Toast.makeText(this, "自定义背景已应用", Toast.LENGTH_SHORT).show();
        }));

        // 预设文字颜色
        String[] presetTxtNames = {"经典黑", "纯净白", "高级灰", "警告红", "深海蓝", "樱花粉", "尊贵金"};
        String[] presetTxtCodes = {"#000000", "#FFFFFF", "#666666", "#FF3B30", "#007AFF", "#FF2D55", "#FFD700"};
        btnPresetTxtColor.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("选择文字颜色").setItems(presetTxtNames, (dialog, which) -> {
            currentTextColor = presetTxtCodes[which];
            Toast.makeText(this, "文字颜色已应用", Toast.LENGTH_SHORT).show();
        }).show());

        // 第三方库自定义文字色
        btnCustomTxtColor.setOnClickListener(v -> CustomColorPickerHelper.showTextPicker(this, colorResult -> {
            currentTextColor = colorResult;
            Toast.makeText(this, "自定义文字色已应用", Toast.LENGTH_SHORT).show();
        }));

        // 保存逻辑 (保持原样)
        btnSave.setOnClickListener(v -> {
            String eventName = etName.getText().toString().trim();
            if (eventName.isEmpty()) { Toast.makeText(this, "请输入事件名称！", Toast.LENGTH_SHORT).show(); return; }
            if (selectedYear == 0) { Toast.makeText(this, "请选择目标日期！", Toast.LENGTH_SHORT).show(); return; }

            DaysData newData = new DaysData(eventName, selectedYear, selectedMonth, selectedDay, selectedRepeatIndex,
                    switchCountUp.isChecked(), switchReminder.isChecked(), cbRemind0.isChecked(), cbRemind1.isChecked(),
                    cbRemind3.isChecked(), cbRemind7.isChecked(), currentBgImageUri, currentBgColor, currentTextColor, switchBlurBg.isChecked());

            android.content.SharedPreferences prefs = getSharedPreferences("MyDaysPrefs", MODE_PRIVATE);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.util.ArrayList<DaysData> list = gson.fromJson(prefs.getString("days_list", "[]"), new com.google.gson.reflect.TypeToken<java.util.ArrayList<DaysData>>() {}.getType());
            if (list == null) list = new java.util.ArrayList<>();

            if (editIndex != -1) list.set(editIndex, newData);
            else list.add(0, newData);

            prefs.edit().putString("days_list", gson.toJson(list)).apply();
            finish();
        });
    }
}