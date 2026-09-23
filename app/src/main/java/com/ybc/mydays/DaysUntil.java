package com.ybc.mydays;

import java.util.Calendar;

/**
 * 日期计算核心工具类
 * 负责推算目标日期，以及计算当前日期与目标日期之间的天数差。
 */
public class DaysUntil {

    // 常量定义
    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_YEARLY = 1;
    public static final int REPEAT_MONTHLY = 2;

    // 一天的毫秒数 (定义为 double 类型以保证除法精度)
    private static final double MILLIS_IN_DAY = 1000d * 60 * 60 * 24;

    private DaysUntil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取下一个有效的目标日期
     *
     * @param targetYear  目标年份
     * @param targetMonth 目标月份 (1-12)
     * @param targetDay   目标日期
     * @param repeatIndex 重复模式 (0:不重复, 1:每年, 2:每月)
     * @param isCountUp   是否为正数累积日 (过去的时间)
     * @return 经过周期推算后的目标日历对象
     */
    public static Calendar getNextTargetDate(int targetYear, int targetMonth, int targetDay, int repeatIndex, boolean isCountUp) {
        Calendar today = getTodayMidnight();
        Calendar target = Calendar.getInstance();
        target.clear();

        if (repeatIndex == REPEAT_NONE) {
            // Calendar 的月份从 0 开始，所以需要减 1
            target.set(targetYear, targetMonth - 1, targetDay);

        } else if (repeatIndex == REPEAT_YEARLY) {
            target.set(today.get(Calendar.YEAR), targetMonth - 1, targetDay);
            if (isCountUp && target.after(today)) {
                target.add(Calendar.YEAR, -1);
            } else if (!isCountUp && target.before(today)) {
                target.add(Calendar.YEAR, 1);
            }

        } else if (repeatIndex == REPEAT_MONTHLY) {
            target.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), targetDay);
            if (isCountUp && target.after(today)) {
                target.add(Calendar.MONTH, -1);
            } else if (!isCountUp && target.before(today)) {
                target.add(Calendar.MONTH, 1);
            }
        }

        return target;
    }

    /**
     * 计算今天距离目标日期的真实天数
     *
     * @param targetYear  目标年份
     * @param targetMonth 目标月份 (1-12)
     * @param targetDay   目标日期
     * @param repeatIndex 重复模式
     * @param isCountUp   是否为正数累积日
     * @return 最终的天数差（绝对数值的计算交由 UI 层处理）
     */
    public static long calculateDays(int targetYear, int targetMonth, int targetDay, int repeatIndex, boolean isCountUp) {
        Calendar today = getTodayMidnight();
        Calendar target = getNextTargetDate(targetYear, targetMonth, targetDay, repeatIndex, isCountUp);

        long diffInMillis;
        if (isCountUp) {
            diffInMillis = today.getTimeInMillis() - target.getTimeInMillis();
        } else {
            diffInMillis = target.getTimeInMillis() - today.getTimeInMillis();
        }

        // 使用 Math.round 四舍五入，修复夏令时 (DST) 切换导致的 23小时/25小时 精度丢失漏洞
        return Math.round(diffInMillis / MILLIS_IN_DAY);
    }

    /**
     * 获取今天零点零分零秒的 Calendar 对象
     */
    private static Calendar getTodayMidnight() {
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(System.currentTimeMillis());
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today;
    }
}