package com.ybc.mydays;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 桌面小组件动态渲染引擎
 */
public class MyDaysWidgetProvider extends AppWidgetProvider {

    /**
     * 发送全局广播：通知手机桌面上所有的 MyDays 小组件立刻刷新！
     * (提供给 MainActivity 和 AddActivity 在修改/删除数据后调用)
     */
    public static void notifyGlobalUpdate(Context context) {
        Intent updateIntent = new Intent(context, MyDaysWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        android.content.ComponentName componentName = new android.content.ComponentName(context, MyDaysWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(componentName);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(updateIntent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // 1. 读取当初绑定时的原始快照
        DaysData originalSnapshot = loadSnapshot(context, appWidgetId);
        if (originalSnapshot == null) return;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_square);

        // 2. 拿着快照的特征去全局主列表里找“最新”的它
        LiveEventResult liveResult = findLiveEventData(context, originalSnapshot);

        if (liveResult.data == null) {
            // 找不到：已被删除
            views.setViewVisibility(R.id.widget_layout_normal, View.GONE);
            views.setViewVisibility(R.id.widget_layout_today, View.GONE);
            views.setViewVisibility(R.id.widget_layout_deleted, View.VISIBLE);

            // 恢复默认的灰色半透明底板
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_glass_card);

            // 点击已被删除的组件，直接退回主页
            attachIntent(context, views, appWidgetId, new Intent(context, MainActivity.class));

        } else {
            // 找到了：同步最新修改
            DaysData liveData = liveResult.data;
            views.setViewVisibility(R.id.widget_layout_deleted, View.GONE);

            long finalDays = DaysUntil.calculateDays(
                    liveData.getMatterYear(), liveData.getMatterMonth(), liveData.getMatterDay(),
                    liveData.getRepeatIndex(), liveData.isCountUp()
            );

            // 渲染最新的背景、文字和颜色
            renderBackground(views, liveData);
            renderTextAndColors(views, liveData, finalDays);

            // 绑定详情页跳转，精准使用最新的列表索引
            Intent detailIntent = new Intent(context, DetailActivity.class);
            detailIntent.putExtra("DETAIL_INDEX", liveResult.index);
            attachIntent(context, views, appWidgetId, detailIntent);

            // 【关键】用最新的数据覆盖掉老快照，防止用户改了名字后下次找不到
            saveSnapshot(context, appWidgetId, liveData);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences.Editor editor = context.getSharedPreferences("MyDaysPrefs", Context.MODE_PRIVATE).edit();
        for (int appWidgetId : appWidgetIds) {
            editor.remove(WidgetConfigActivity.PREF_WIDGET_PREFIX + appWidgetId);
        }
        editor.apply();
    }

    // 辅助模块

    private static DaysData loadSnapshot(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences("MyDaysPrefs", Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(WidgetConfigActivity.PREF_WIDGET_PREFIX + appWidgetId, null);
        return jsonStr != null ? new Gson().fromJson(jsonStr, DaysData.class) : null;
    }

    private static void saveSnapshot(Context context, int appWidgetId, DaysData newData) {
        SharedPreferences prefs = context.getSharedPreferences("MyDaysPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString(WidgetConfigActivity.PREF_WIDGET_PREFIX + appWidgetId, new Gson().toJson(newData)).apply();
    }

    /**
     * 包装类：同时返回找到的数据和它在最新列表中的索引
     */
    private static class LiveEventResult {
        DaysData data;
        int index;
        LiveEventResult(DaysData data, int index) { this.data = data; this.index = index; }
    }

    /**
     * 身份追踪：通过唯一的 eventId 确认事件是否还在主列表里
     */
    private static LiveEventResult findLiveEventData(Context context, DaysData snapshotData) {
        SharedPreferences prefs = context.getSharedPreferences("MyDaysPrefs", Context.MODE_PRIVATE);
        Type type = new TypeToken<ArrayList<DaysData>>() {}.getType();
        List<DaysData> mainList = new Gson().fromJson(prefs.getString("days_list", "[]"), type);

        if (mainList != null) {
            for (int i = 0; i < mainList.size(); i++) {
                DaysData current = mainList.get(i);

                // 对比ID查找
                if (current.getEventId().equals(snapshotData.getEventId())) {
                    return new LiveEventResult(current, i);
                }
            }
        }
        return new LiveEventResult(null, -1);
    }

    private static void renderBackground(RemoteViews views, DaysData data) {
        if (data.getBgImageUri() != null && !data.getBgImageUri().isEmpty()) {
            // 用户在 App 内选择了图片背景
            // 小组件不支持直接渲染带模糊效果的本地图片，因此恢复使用默认的半透明毛玻璃底板
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_glass_card);
        } else {
            // 用户在 App 内选择了纯色/渐变色
            String bgColor = data.getBgColor();
            if (bgColor != null) {
                String mainColor = bgColor.contains(",") ? bgColor.split(",")[0] : bgColor;
                views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor(mainColor));
            } else {
                views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor("#FF9A9E"));
            }
        }
    }

    private static void renderTextAndColors(RemoteViews views, DaysData data, long finalDays) {
        int txtColor = Color.parseColor(data.getTextColor() != null ? data.getTextColor() : "#000000");

        views.setTextColor(R.id.widget_tv_name, txtColor);
        views.setTextColor(R.id.widget_tv_label, txtColor);
        views.setTextColor(R.id.widget_tv_days, txtColor);
        views.setTextColor(R.id.widget_tv_unit, txtColor);
        views.setTextColor(R.id.widget_tv_today_name, txtColor);

        if (finalDays == 0) {
            views.setViewVisibility(R.id.widget_layout_normal, View.GONE);
            views.setViewVisibility(R.id.widget_layout_today, View.VISIBLE);
            views.setTextViewText(R.id.widget_tv_today_name, data.getMatterName());
        } else {
            views.setViewVisibility(R.id.widget_layout_normal, View.VISIBLE);
            views.setViewVisibility(R.id.widget_layout_today, View.GONE);

            views.setTextViewText(R.id.widget_tv_name, data.getMatterName());
            views.setTextViewText(R.id.widget_tv_days, String.valueOf(Math.abs(finalDays)));

            String labelText = data.isCountUp() ? "已经" : (finalDays < 0 && data.getRepeatIndex() == DaysUntil.REPEAT_NONE ? "已超" : "还有");
            views.setTextViewText(R.id.widget_tv_label, labelText);
        }
    }

    private static void attachIntent(Context context, RemoteViews views, int appWidgetId, Intent intent) {
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, pendingFlags);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
    }
}