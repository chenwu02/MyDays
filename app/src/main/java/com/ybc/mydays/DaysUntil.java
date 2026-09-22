package com.ybc.mydays;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DaysUntil {

    // 核心计算引擎：接收目标年月日，重复模式(0=不重复, 1=每年, 2=每月)，以及是否为正数(isCountUp)
    public static long calculateDays(int targetYear, int targetMonth, int targetDay, int repeatIndex, boolean isCountUp) {

        // 1. 获取准确的“今天”00:00，消除时分秒误差
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(System.currentTimeMillis());
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // 2. 构造计算基准日
        Calendar target = Calendar.getInstance();
        target.clear();

        if (repeatIndex == 0) {
            // 【不重复】：直接使用用户选定的具体年份
            target.set(targetYear, targetMonth - 1, targetDay);

        } else if (repeatIndex == 1) {
            // 【每年重复】：先把基准日设为“今年的该月该日”
            target.set(today.get(Calendar.YEAR), targetMonth - 1, targetDay);

            if (isCountUp) {
                // 正数（过去）：如果今年的日子还没到，说明最近的一次是去年的这一天
                if (target.after(today)) {
                    target.add(Calendar.YEAR, -1);
                }
            } else {
                // 倒数（未来）：如果今年的日子已经过去了，说明下一个是明年的这一天
                if (target.before(today)) {
                    target.add(Calendar.YEAR, 1);
                }
            }

        } else if (repeatIndex == 2) {
            // 【每月重复】：先把基准日设为“本月的该日”
            target.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), targetDay);

            if (isCountUp) {
                // 正数（过去）：如果本月的日子还没到，说明最近的一次是上个月的这一天
                if (target.after(today)) {
                    target.add(Calendar.MONTH, -1);
                }
            } else {
                // 倒数（未来）：如果本月的日子已经过去了，说明下一个是下个月的这一天
                if (target.before(today)) {
                    target.add(Calendar.MONTH, 1);
                }
            }
        }

        // 3. 计算天数差并返回
        long diffInMillis;
        if (isCountUp) {
            // 正数（累计日）：今天 - 目标日
            diffInMillis = today.getTimeInMillis() - target.getTimeInMillis();
        } else {
            // 倒数日：目标日 - 今天
            diffInMillis = target.getTimeInMillis() - today.getTimeInMillis();
        }

        return diffInMillis / (1000 * 60 * 60 * 24);
    }
}