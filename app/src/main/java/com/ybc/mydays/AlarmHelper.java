package com.ybc.mydays;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

/**
 * 精准闹钟调度工具类
 * 负责向 Android 系统底层注册高优先级的定时唤醒任务，确保倒数日提醒能在每天准点触发。
 */
public class AlarmHelper {

    // 常量定义
    private static final String TAG = "AlarmHelper";
    private static final int ALARM_REQUEST_CODE = 1001; // 广播请求码，用于区分不同的闹钟

    // 触发时间设定：每天 00:01
    // (设定在零点过一分，可完美避开跨天临界点的时间戳误差)
    private static final int TARGET_HOUR = 0;
    private static final int TARGET_MINUTE = 1;

    /**
     * 工具类禁止实例化
     */
    private AlarmHelper() {
        throw new UnsupportedOperationException("Helper class cannot be instantiated");
    }

    /**
     * 安排下一个零点（00:01）的精确唤醒闹钟。
     * 该方法会自动判断当前时间，如果今天的 00:01 已过，则自动设定为明天的 00:01。
     *
     * @param context 上下文
     */
    public static void scheduleNextMidnightAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "无法获取 AlarmManager 服务");
            return;
        }

        // 1. 设置触发目标时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, TARGET_HOUR);
        calendar.set(Calendar.MINUTE, TARGET_MINUTE);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 如果计算出的触发时间已经早于或等于现在，将天数推迟一天 (设定为明天)
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 2. 包装要触发的广播 Intent
        Intent intent = new Intent(context, ReminderReceiver.class);

        // 兼容性处理：Android 12 (S) 及以上要求 PendingIntent 必须显式声明可变性
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE, intent, flags
        );

        // 3. 强行设定精确闹钟
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0 引入了 Doze 深度休眠，必须使用该方法强行穿透休眠唤醒 CPU
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
            Log.d(TAG, "已成功注册下一次精准闹钟: " + calendar.getTime());

        } catch (SecurityException e) {
            // Android 14+ 严格限制了精准闹钟权限，虽然配置了 USE_EXACT_ALARM，
            // 仍需捕获异常，防止在某些深度定制的国产系统中引发崩溃。
            Log.e(TAG, "注册精确闹钟失败：系统拒绝了权限申请", e);
        }
    }
}