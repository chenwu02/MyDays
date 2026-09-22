package com.ybc.mydays;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // 1. 读取本地保存的所有事件
        SharedPreferences prefs = context.getSharedPreferences("MyDaysPrefs", Context.MODE_PRIVATE);
        String jsonStr = prefs.getString("days_list", "[]");
        Type type = new TypeToken<ArrayList<DaysData>>() {}.getType();
        ArrayList<DaysData> dataList = new Gson().fromJson(jsonStr, type);

        if (dataList == null || dataList.isEmpty()) {
            return Result.success();
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 2. 遍历事件，检查是否需要触发提醒
        for (int i = 0; i < dataList.size(); i++) {
            DaysData data = dataList.get(i);

            // 如果开关没开，直接跳过
            if (!data.isReminderOn()) continue;

            long finalDays = DaysUntil.calculateDays(
                    data.getMatterYear(), data.getMatterMonth(), data.getMatterDay(),
                    data.getRepeatIndex(), data.isCountUp()
            );

            boolean shouldRemind = false;
            String remindContent = "";

            // 核心判断逻辑：匹配用户勾选的天数
            if (finalDays == 0 && data.isRemindDayOf()) {
                shouldRemind = true;
                remindContent = "就是今天！";
            } else if (!data.isCountUp()) {
                // 倒数日提前提醒
                if (finalDays == 1 && data.isRemind1Day()) {
                    shouldRemind = true;
                    remindContent = "明天就是啦！";
                } else if (finalDays == 3 && data.isRemind3Days()) {
                    shouldRemind = true;
                    remindContent = "还有 3 天，做好准备！";
                } else if (finalDays == 7 && data.isRemind7Days()) {
                    shouldRemind = true;
                    remindContent = "下周就要到了！";
                }
            }

            // 3. 发送系统通知
            if (shouldRemind) {
                // Android 13 以上需要检查权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "MY_DAYS_CHANNEL")
                        .setSmallIcon(R.mipmap.ic_launcher) // 使用 App 默认图标
                        .setContentTitle(data.getMatterName())
                        .setContentText(remindContent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

                manager.notify(i, builder.build()); // 使用列表索引 i 作为唯一的通知 ID
            }
        }
        return Result.success();
    }
}