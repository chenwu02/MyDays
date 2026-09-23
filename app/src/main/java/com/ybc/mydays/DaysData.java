package com.ybc.mydays;

/**
 * 数据模型
 */
public class DaysData {
    private String matterName;
    private int matterYear;
    private int matterMonth;
    private int matterDay;
    private int repeatIndex; // 0=不重复, 1=每年, 2=每月
    private boolean isCountUp;
    private boolean isReminderOn;
    private boolean remindDayOf;
    private boolean remind1Day;
    private boolean remind3Days;
    private boolean remind7Days;
    private String bgImageUri; // 裁剪后图片的本地路径 (如果没有图片则为 null)
    private String bgColor; // 纯色背景的色值 (默认为 "#FFFFFF")
    private String textColor; // 字体颜色的色值 (默认为 "#000000")
    private boolean isBlurBg;

    public DaysData() {};

    public DaysData(String matterName, int matterYear, int matterMonth, int matterDay, int repeatIndex, boolean isCountUp, boolean isReminderOn, boolean remindDayOf, boolean remind1Day, boolean remind3Days, boolean remind7Days, String bgImageUri, String bgColor, String textColor, boolean isBlurBg) {
        this.matterName = matterName;
        this.matterYear = matterYear;
        this.matterMonth = matterMonth;
        this.matterDay = matterDay;
        this.repeatIndex = repeatIndex;
        this.isCountUp = isCountUp;
        this.isReminderOn = isReminderOn;
        this.remindDayOf = remindDayOf;
        this.remind1Day = remind1Day;
        this.remind3Days = remind3Days;
        this.remind7Days = remind7Days;
        this.bgImageUri = bgImageUri;
        this.bgColor = bgColor;
        this.textColor = textColor;
        this.isBlurBg = isBlurBg;
    }

    public String getMatterName() {
        return matterName;
    }

    public void setMatterName(String matterName) {
        this.matterName = matterName;
    }

    public int getMatterYear() {
        return matterYear;
    }

    public void setMatterYear(int matterYear) {
        this.matterYear = matterYear;
    }

    public int getMatterMonth() {
        return matterMonth;
    }

    public void setMatterMonth(int matterMonth) {
        this.matterMonth = matterMonth;
    }

    public int getMatterDay() {
        return matterDay;
    }

    public void setMatterDay(int matterDay) {
        this.matterDay = matterDay;
    }

    public int getRepeatIndex() {
        return repeatIndex;
    }

    public void setRepeatIndex(int repeatIndex) {
        this.repeatIndex = repeatIndex;
    }

    public boolean isCountUp() {
        return isCountUp;
    }

    public void setCountUp(boolean isCountUp) {
        this.isCountUp = isCountUp;
    }

    public boolean isReminderOn() {
        return isReminderOn;
    }

    public void setReminderOn(boolean isReminderOn) {
        this.isReminderOn = isReminderOn;
    }

    public boolean isRemindDayOf() {
        return remindDayOf;
    }

    public void setRemindDayOf(boolean remindDayOf) {
        this.remindDayOf = remindDayOf;
    }

    public boolean isRemind1Day() {
        return remind1Day;
    }

    public void setRemind1Day(boolean remind1Day) {
        this.remind1Day = remind1Day;
    }

    public boolean isRemind3Days() {
        return remind3Days;
    }

    public void setRemind3Days(boolean remind3Days) {
        this.remind3Days = remind3Days;
    }

    public boolean isRemind7Days() {
        return remind7Days;
    }

    public void setRemind7Days(boolean remind7Days) {
        this.remind7Days = remind7Days;
    }

    public String getBgImageUri() {
        return bgImageUri;
    }

    public void setBgImageUri(String bgImageUri) {
        this.bgImageUri = bgImageUri;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public boolean isBlurBg() {
        return isBlurBg;
    }

    public void setBlurBg(boolean isBlurBg) {
        this.isBlurBg = isBlurBg;
    }
}
