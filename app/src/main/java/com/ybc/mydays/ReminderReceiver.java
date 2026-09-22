package com.ybc.mydays;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. 准点执行通知检查逻辑
        checkAndSendNotifications(context);

        // 2. 任务执行完，立刻安排明天的准点闹钟（无限循环）
        AlarmHelper.scheduleNextMidnightAlarm(context);
    }

    private void checkAndSendNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MyDaysPrefs", Context.MODE_PRIVATE);
        String jsonStr = prefs.getString("days_list", "[]");
        Type type = new TypeToken<ArrayList<DaysData>>() {}.getType();
        ArrayList<DaysData> dataList = new Gson().fromJson(jsonStr, type);

        if (dataList == null || dataList.isEmpty()) return;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        for (int i = 0; i < dataList.size(); i++) {
            DaysData data = dataList.get(i);
            if (!data.isReminderOn()) continue;

            long finalDays = DaysUntil.calculateDays(
                    data.getMatterYear(), data.getMatterMonth(), data.getMatterDay(),
                    data.getRepeatIndex(), data.isCountUp()
            );

            boolean shouldRemind = false;
            String remindContent = "";

            if (finalDays == 0 && data.isRemindDayOf()) {
                shouldRemind = true;
                remindContent = "就是今天！";
            } else if (!data.isCountUp()) {
                if (finalDays == 1 && data.isRemind1Day()) {
                    shouldRemind = true; remindContent = "明天就是啦！";
                } else if (finalDays == 3 && data.isRemind3Days()) {
                    shouldRemind = true; remindContent = "还有 3 天，做好准备！";
                } else if (finalDays == 7 && data.isRemind7Days()) {
                    shouldRemind = true; remindContent = "下周就要到了！";
                }
            }

            if (shouldRemind) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                Intent detailIntent = new Intent(context, DetailActivity.class);
                detailIntent.putExtra("DETAIL_INDEX", i);

                // 兼容高版本 Android 的安全性要求
                int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
                }

                // 打包成 PendingIntent
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, i, detailIntent, pendingFlags
                );
                // =======================================================

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "MY_DAYS_CHANNEL")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(data.getMatterName())
                        .setContentText(remindContent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent) // <--- 关键：绑定点击跳转事件
                        .setAutoCancel(true);            // <--- 关键：点击后自动消失

                manager.notify(i, builder.build()); // 我们用索引 i 作为通知的 ID
            }
        }
    }
}