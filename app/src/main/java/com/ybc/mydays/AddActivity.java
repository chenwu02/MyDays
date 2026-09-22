package com.ybc.mydays;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AddActivity extends AppCompatActivity {

    // 1. 声明稍后保存时需要用到的核心数据变量
    private int selectedYear = 0;
    private int selectedMonth = 0; // 这里存的是 1-12 的真实月份
    private int selectedDay = 0;
    private int selectedRepeatIndex = 0; // 0=不重复, 1=每年一次, 2=每月一次
    private final String[] repeatOptions = {"不重复", "每年一次", "每月一次"};
    private int editIndex = -1; // -1 代表新建，大于等于 0 代表编辑
    // 暂存用户选择的外观属性
    private String currentBgImageUri = null;
    private String currentBgColor = "#FFFFFF";
    private String currentTextColor = "#000000";

    // 引入图片裁剪器 (自动请求图库、弹出等比裁剪框、支持双指缩放，并返回最终图片的 URI)
    private final androidx.activity.result.ActivityResultLauncher<com.canhub.cropper.CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new com.canhub.cropper.CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    currentBgImageUri = result.getUriContent().toString(); // 获取裁切好的高清图片路径
                    currentBgColor = null; // 有了图片就不要纯色了
                    android.widget.Toast.makeText(this, "图片裁剪并保存成功！", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(this, "取消选择图片", android.widget.Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add);

        //绑定UI控件
        ImageButton btnBack = findViewById(R.id.backOfAdd);
        EditText etName = findViewById(R.id.renameOfAdd);
        Button btnSave = findViewById(R.id.saveOfAdd);

        RelativeLayout rowTargetDate = findViewById(R.id.row_target_date);
        TextView tvSelectedDate = findViewById(R.id.tv_selected_date);

        RelativeLayout rowRepeat = findViewById(R.id.row_repeat);
        TextView tvSelectedRepeat = findViewById(R.id.tv_selected_repeat);

        Switch switchReminder = findViewById(R.id.switch_reminder);
        LinearLayout containerReminderDetails = findViewById(R.id.container_reminder_details);
        CheckBox cbRemind0 = findViewById(R.id.cb_remind_0);
        CheckBox cbRemind1 = findViewById(R.id.cb_remind_1);
        CheckBox cbRemind3 = findViewById(R.id.cb_remind_3);
        CheckBox cbRemind7 = findViewById(R.id.cb_remind_7);

        Switch switchCountUp = findViewById(R.id.switch_count_up);

        Button btnPickImage = findViewById(R.id.btn_pick_image);
        Button btnPickBgColor = findViewById(R.id.btn_pick_bg_color);
        Button btnPickTxtColor = findViewById(R.id.btn_pick_txt_color);
        Switch switchBlurBg = findViewById(R.id.switch_blur_bg);

        // 判断是否为编辑模式并回填数据
        editIndex = getIntent().getIntExtra("EDIT_INDEX", -1);

        if (editIndex != -1) {
            // 是编辑模式，从本地读取数据列表
            android.content.SharedPreferences prefs = getSharedPreferences("MyDaysPrefs", MODE_PRIVATE);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String jsonStr = prefs.getString("days_list", "[]");
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.ArrayList<DaysData>>() {}.getType();
            java.util.ArrayList<DaysData> currentList = gson.fromJson(jsonStr, type);

            // 确保数据存在
            if (currentList != null && editIndex < currentList.size()) {
                DaysData editData = currentList.get(editIndex);

                // 1. 回填名字
                etName.setText(editData.getMatterName());

                // 2. 回填日期
                selectedYear = editData.getMatterYear();
                selectedMonth = editData.getMatterMonth();
                selectedDay = editData.getMatterDay();
                tvSelectedDate.setText(selectedYear + "年" + selectedMonth + "月" + selectedDay + "日 >");
                tvSelectedDate.setTextColor(0xFF000000); // 改为黑色

                // 3. 回填重复周期
                selectedRepeatIndex = editData.getRepeatIndex();
                tvSelectedRepeat.setText(repeatOptions[selectedRepeatIndex] + " >");
                tvSelectedRepeat.setTextColor(0xFF000000);

                // 4. 回填正数(累计日)开关
                // 注意：请确保你之前在 AddActivity.java 顶部声明并 findViewById 绑定了 switchCountUp
                switchCountUp.setChecked(editData.isCountUp());

                // 5. 回填提醒设置
                switchReminder.setChecked(editData.isReminderOn());
                if (editData.isReminderOn()) {
                    containerReminderDetails.setVisibility(View.VISIBLE);
                    cbRemind0.setChecked(editData.isRemindDayOf());
                    cbRemind1.setChecked(editData.isRemind1Day());
                    cbRemind3.setChecked(editData.isRemind3Days());
                    cbRemind7.setChecked(editData.isRemind7Days());
                }

                // 6. 回填外观设置
                currentBgImageUri = editData.getBgImageUri();
                currentBgColor = editData.getBgColor();
                currentTextColor = editData.getTextColor();
                switchBlurBg.setChecked(editData.isBlurBg());
            }
        }

        //返回按钮
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 关闭当前页面，返回上一页
            }
        });

        //日期选择器
        rowTargetDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取当天的日期作为日历默认选中的日期
                Calendar calendar = Calendar.getInstance();
                int currentYear = calendar.get(Calendar.YEAR);
                int currentMonth = calendar.get(Calendar.MONTH);
                int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

                // 创建并显示 DatePickerDialog
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        AddActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                // 记录用户选中的日期（注意：系统返回的 month 依然是 0-11，所以要 +1）
                                selectedYear = year;
                                selectedMonth = month + 1;
                                selectedDay = dayOfMonth;

                                // 更新界面文字显示
                                String dateText = selectedYear + "年" + selectedMonth + "月" + selectedDay + "日 >";
                                tvSelectedDate.setText(dateText);
                                tvSelectedDate.setTextColor(0xFF000000); // 选好后把灰色字变成黑色
                            }
                        },
                        currentYear, currentMonth, currentDay
                );
                datePickerDialog.show();
            }
        });

        //重复周期选择器
        rowRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AddActivity.this);
                builder.setTitle("选择重复周期");
                builder.setSingleChoiceItems(repeatOptions, selectedRepeatIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedRepeatIndex = which; // 记录选中了哪一个
                        tvSelectedRepeat.setText(repeatOptions[which] + " >");
                        tvSelectedRepeat.setTextColor(0xFF000000);
                        dialog.dismiss(); // 选完自动关闭
                    }
                });
                builder.create().show();
            }
        });

        //提醒选择器
        switchReminder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 开关打开，显示下方设置区
                    containerReminderDetails.setVisibility(View.VISIBLE);
                    // 如果全部都没勾选，给个默认勾选“提前1天”
                    if (!cbRemind0.isChecked() && !cbRemind1.isChecked() && !cbRemind3.isChecked() && !cbRemind7.isChecked()) {
                        cbRemind0.setChecked(true);
                    }
                } else {
                    // 开关关闭，隐藏设置区，并清空勾选状态
                    containerReminderDetails.setVisibility(View.GONE);
                    cbRemind0.setChecked(false);
                    cbRemind1.setChecked(false);
                    cbRemind3.setChecked(false);
                    cbRemind7.setChecked(false);
                }
            }
        });

        // 点击选择图片
        btnPickImage.setOnClickListener(v -> {
            // 配置裁剪器：锁定 9:16 的手机全屏纵向比例
            com.canhub.cropper.CropImageOptions options = new com.canhub.cropper.CropImageOptions();
            options.imageSourceIncludeGallery = true;
            options.imageSourceIncludeCamera = false;
            options.fixAspectRatio = true; // 开启比例锁定
            options.aspectRatioX = 9;      // 宽 9
            options.aspectRatioY = 16;     // 高 16

            cropImageLauncher.launch(new com.canhub.cropper.CropImageContractOptions(null, options));
        });

        // 准备一个简单的弹窗颜色库
        String[] colorNames = {"暗夜黑", "纯净白", "清爽蓝", "少女粉"};
        String[] colorHexCodes = {"#222222", "#FFFFFF", "#E3F2FD", "#FCE4EC"};
        String[] textColorHexCodes = {"#FFFFFF", "#000000", "#007AFF", "#FF2D55"};

        // 点击选择背景色
        btnPickBgColor.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("选择纯色背景")
                    .setItems(colorNames, (dialog, which) -> {
                        currentBgColor = colorHexCodes[which];
                        currentBgImageUri = null; // 选了颜色就清空图片
                    }).show();
        });

        // 点击选择文字颜色
        btnPickTxtColor.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("选择文字颜色")
                    .setItems(colorNames, (dialog, which) -> {
                        currentTextColor = textColorHexCodes[which];
                    }).show();
        });

        //保存
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. 获取输入的名字
                String eventName = etName.getText().toString().trim();

                // 2. 基础数据校验（不填名字或不选日期不让保存）
                if (eventName.isEmpty()) {
                    Toast.makeText(AddActivity.this, "请输入事件名称！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedYear == 0) {
                    Toast.makeText(AddActivity.this, "请选择目标日期！", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 3. 收集提醒状态
                boolean isReminderOn = switchReminder.isChecked();
                boolean remindDayOf = cbRemind0.isChecked();
                boolean remind1Day = cbRemind1.isChecked();
                boolean remind3Days = cbRemind3.isChecked();
                boolean remind7Days = cbRemind7.isChecked();

                //是否正数
                boolean isCountUp = switchCountUp.isChecked();

                boolean isBlurBg = switchBlurBg.isChecked();

                // 4. 将所有数据打包成一个 DaysData 对象
                DaysData newData = new DaysData(
                        eventName, selectedYear, selectedMonth, selectedDay,
                        selectedRepeatIndex, isCountUp,isReminderOn, remindDayOf,
                        remind1Day, remind3Days, remind7Days,
                        currentBgImageUri, currentBgColor, currentTextColor, isBlurBg
                );

                // 5. 取出旧数据，把新数据插进去，再存回手机里 (使用 SharedPreferences + Gson)
                android.content.SharedPreferences prefs = getSharedPreferences("MyDaysPrefs", MODE_PRIVATE);
                com.google.gson.Gson gson = new com.google.gson.Gson();

                // 取出之前的列表（如果没有存过，默认是一个空列表的 JSON 字符串 "[]"）
                String jsonStr = prefs.getString("days_list", "[]");

                // 将 JSON 字符串还原成 Java 的 ArrayList
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.ArrayList<DaysData>>() {}.getType();
                java.util.ArrayList<DaysData> currentList = gson.fromJson(jsonStr, type);

                // 将新创建的事件添加到列表的最前面
                if (currentList == null) {
                    currentList = new java.util.ArrayList<>();
                }

                // 核心判断：是编辑覆盖旧数据，还是新增数据？
                if (editIndex != -1) {
                    // 编辑模式：覆盖原来位置的数据
                    currentList.set(editIndex, newData);
                    Toast.makeText(AddActivity.this, "修改成功！", Toast.LENGTH_SHORT).show();
                } else {
                    // 新建模式：插入到列表最前面
                    currentList.add(0, newData);
                    Toast.makeText(AddActivity.this, "保存成功！", Toast.LENGTH_SHORT).show();
                }

                // 重新转换成 JSON 存入手机
                String newJsonStr = gson.toJson(currentList);
                prefs.edit().putString("days_list", newJsonStr).apply();

                finish();
            }
        });
    }
}