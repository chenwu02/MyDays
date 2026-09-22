package com.ybc.mydays;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DaysUntil {

    // 核心计算引擎：接收目标年月日，重复模式(0=不重复, 1=每年, 2=每月)，以及是否为正数(isCountUp)
    public static Calendar getNextTargetDate(int targetYear, int targetMonth, int targetDay, int repeatIndex, boolean isCountUp) {
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(System.currentTimeMillis());
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar target = Calendar.getInstance();
        target.clear();

        if (repeatIndex == 0) {
            // 【不重复】：直接使用用户选定的具体年份
            target.set(targetYear, targetMonth - 1, targetDay);
        } else if (repeatIndex == 1) {
            // 【每年重复】
            target.set(today.get(Calendar.YEAR), targetMonth - 1, targetDay);
            if (isCountUp) {
                if (target.after(today)) target.add(Calendar.YEAR, -1);
            } else {
                if (target.before(today)) target.add(Calendar.YEAR, 1);
            }
        } else if (repeatIndex == 2) {
            // 【每月重复】
            target.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), targetDay);
            if (isCountUp) {
                if (target.after(today)) target.add(Calendar.MONTH, -1);
            } else {
                if (target.before(today)) target.add(Calendar.MONTH, 1);
            }
        }
        return target;
    }

    // 核心计算引擎：直接调用上面的方法算出日期再做减法
    public static long calculateDays(int targetYear, int targetMonth, int targetDay, int repeatIndex, boolean isCountUp) {
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(System.currentTimeMillis());
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar target = getNextTargetDate(targetYear, targetMonth, targetDay, repeatIndex, isCountUp);

        long diffInMillis;
        if (isCountUp) {
            diffInMillis = today.getTimeInMillis() - target.getTimeInMillis();
        } else {
            diffInMillis = target.getTimeInMillis() - today.getTimeInMillis();
        }

        return diffInMillis / (1000 * 60 * 60 * 24);
    }
}