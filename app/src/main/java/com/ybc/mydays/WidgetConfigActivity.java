package com.ybc.mydays;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 小组件配置页面
 * 职责：拦截系统的小组件创建请求，展示可选事件，保存用户选择的快照并通知系统生成组件。
 */
public class WidgetConfigActivity extends AppCompatActivity {

    private static final String PREF_NAME = "MyDaysPrefs";
    public static final String PREF_WIDGET_PREFIX = "widget_data_";

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private List<DaysData> dataList;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 默认返回取消状态，防止用户中途退出导致生成空组件
        setResult(RESULT_CANCELED);

        if (!extractWidgetId()) {
            finish();
            return;
        }

        initViews();
        loadLocalEvents();
        bindListViewAdapter();
    }

    /**
     * 解析 Intent 获取即将生成的小组件 ID
     * @return 获取成功返回 true，否则返回 false
     */
    private boolean extractWidgetId() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
            );
        }
        return mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID;
    }

    /**
     * 极简 UI 初始化
     */
    private void initViews() {
        listView = new ListView(this);

        // 动态增加头部提示语
        TextView header = new TextView(this);
        header.setText("请选择一个事件");
        header.setTextSize(18);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(48, 48, 48, 48);
        listView.addHeaderView(header, null, false);

        setContentView(listView);
    }

    /**
     * 读取本地保存的所有倒数日事件
     */
    private void loadLocalEvents() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String jsonStr = prefs.getString("days_list", "[]");
        Type type = new TypeToken<ArrayList<DaysData>>() {}.getType();
        dataList = new Gson().fromJson(jsonStr, type);
    }

    /**
     * 渲染列表并配置点击绑定事件
     */
    private void bindListViewAdapter() {
        if (dataList == null || dataList.isEmpty()) {
            Toast.makeText(this, "请先在 App 内创建一个倒数日", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        List<String> nameList = new ArrayList<>();
        for (DaysData data : dataList) {
            nameList.add(data.getMatterName());
        }

        // 适配器数据由于有 Header，实际索引位置会向下偏移
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nameList));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            // 减去 1 是因为我们增加了一个不可点击的 Header
            int actualPosition = position - listView.getHeaderViewsCount();
            if (actualPosition >= 0) {
                confirmAndBindWidget(dataList.get(actualPosition));
            }
        });
    }

    /**
     * 确认用户选择，保存数据快照并激活小组件
     */
    private void confirmAndBindWidget(DaysData selectedData) {
        // 1. 保存隔离快照
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putString(PREF_WIDGET_PREFIX + mAppWidgetId, new Gson().toJson(selectedData)).apply();

        // 2. 触发首次渲染
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        MyDaysWidgetProvider.updateAppWidget(this, appWidgetManager, mAppWidgetId);

        // 3. 通知系统完成配置
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}