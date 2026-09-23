package com.ybc.mydays;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 通知定时器校准提醒
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // 当手机开机、用户手动修改时间、或者跨越时区时，立刻重新校准闹钟
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_TIME_CHANGED.equals(action) ||
                Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {

            AlarmHelper.scheduleNextMidnightAlarm(context);
        }
    }
}