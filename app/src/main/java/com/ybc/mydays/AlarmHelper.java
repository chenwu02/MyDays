package com.ybc.mydays;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class AlarmHelper {

    public static void scheduleNextMidnightAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // 1. 设置触发目标时间为当天的 00:01:00
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 如果当天的 00:01 已经过去了，则设定为明天的 00:01
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 2. 包装要触发的广播 Intent
        Intent intent = new Intent(context, ReminderReceiver.class);
        // 兼容高版本 Android 的安全性要求
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);

        // 3. 强行设定精确闹钟 (穿透深度休眠)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 允许在空闲(Doze)模式下唤醒设备执行
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } catch (SecurityException e) {
            // 虽然加了 USE_EXACT_ALARM，但为了防止某些极端定制系统报错，加上 try-catch
            e.printStackTrace();
        }
    }
}