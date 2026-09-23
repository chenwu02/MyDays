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
import java.util.List;

/**
 * 核心通知调度广播接收器
 * 由系统的 AlarmManager 准点唤醒，负责遍历事件列表并分发今日的通知提醒，
 * 任务结束后自动完成下一个零点闹钟的循环注册。
 */
public class ReminderReceiver extends BroadcastReceiver {

    // 常量定义
    private static final String PREF_NAME = "MyDaysPrefs";
    private static final String KEY_DAYS_LIST = "days_list";
    private static final String CHANNEL_ID = "MY_DAYS_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. 执行今日通知下发任务
        checkAndSendNotifications(context);

        // 2. 引擎自启：任务执行完，立刻安排明天的准点闹钟
        AlarmHelper.scheduleNextMidnightAlarm(context);
    }

    /**
     * 遍历本地存储的事件列表，计算天数并决定是否需要派发系统通知
     */
    private void checkAndSendNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_DAYS_LIST, "[]");
        Type type = new TypeToken<ArrayList<DaysData>>() {}.getType();
        List<DaysData> dataList = new Gson().fromJson(jsonStr, type);

        if (dataList == null || dataList.isEmpty()) return;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        // 主干流程：极为清爽的循环体，将复杂的逻辑全部分发给子方法
        for (int i = 0; i < dataList.size(); i++) {
            DaysData data = dataList.get(i);

            // 如果用户关闭了该事件的提醒，直接跳过
            if (!data.isReminderOn()) continue;

            long finalDays = DaysUntil.calculateDays(
                    data.getMatterYear(), data.getMatterMonth(), data.getMatterDay(),
                    data.getRepeatIndex(), data.isCountUp()
            );

            // 获取应该发送的通知文案（若返回 null 则代表今天不需要提醒）
            String remindContent = evaluateReminderContent(data, finalDays);

            if (remindContent != null) {
                dispatchNotification(context, manager, data, remindContent, i);
            }
        }
    }

    /**
     * 核心业务判断：根据距离天数与用户设定的开关，评估今日的通知文案
     *
     * @param data      事件数据模型
     * @param finalDays 距离目标日的真实天数
     * @return 需要发送的文案文本。若不满足提醒条件，则返回 null。
     */
    private String evaluateReminderContent(DaysData data, long finalDays) {
        // 当天提醒逻辑 (倒数日与累积日通用)
        if (finalDays == 0 && data.isRemindDayOf()) {
            return "就是今天！";
        }

        // 业务规则：累积日（过去发生的事件）不进行提前预警，仅限倒数日进入以下判断
        if (!data.isCountUp()) {
            if (finalDays == 1 && data.isRemind1Day()) {
                return "明天就是啦！";
            } else if (finalDays == 3 && data.isRemind3Days()) {
                return "还有 3 天，做好准备！";
            } else if (finalDays == 7 && data.isRemind7Days()) {
                return "下周就要到了！";
            }
        }

        return null;
    }

    /**
     * 构建并向系统投递最终的通知卡片
     *
     * @param context        上下文
     * @param manager        系统通知管理器
     * @param data           事件数据模型（用于提取标题等信息）
     * @param content        通知正文
     * @param notificationId 通知的唯一识别码（使用列表索引绑定，以便在详情页清除）
     */
    private void dispatchNotification(Context context, NotificationManager manager, DaysData data, String content, int notificationId) {
        // 针对 Android 13 (TIRAMISU) 及以上的权限拦截校验
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        // 构建点击通知后的跳转意图
        Intent detailIntent = new Intent(context, DetailActivity.class);
        detailIntent.putExtra("DETAIL_INDEX", notificationId);

        // 兼容 Android 12 的高安全性 PendingIntent 标志位
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notificationId, detailIntent, pendingFlags
        );

        // 组装并发送高优先级通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(data.getMatterName())
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // 绑定点击跳转事件
                .setAutoCancel(true);            // 点击后通知自动消失

        manager.notify(notificationId, builder.build());
    }
}