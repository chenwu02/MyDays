package com.ybc.mydays;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView; // 改用 RecyclerView
    private ArrayList<DaysData> dataList;
    private DaysAdapter adapter;

    // 控制是否允许排序的开关变量
    private boolean isSortingMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 初始化 RecyclerView
        recyclerView = findViewById(R.id.list_home);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // 必须设置 LayoutManager

        // 2. 绑定跳转 AddActivity 按钮
        ImageButton btnAdd = findViewById(R.id.add);
        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddActivity.class));
        });

        // 3. 找到你新加的排序按钮
        Button btnSort = findViewById(R.id.sort);
        btnSort.setOnClickListener(v -> {
            isSortingMode = !isSortingMode; // 切换排序状态
            if (isSortingMode) {
                btnSort.setText("完成排序");
            } else {
                btnSort.setText("排序");
                // 退出排序模式时，保存最新的顺序到手机
                saveDataToLocal();
            }
        });

        // 4. 配置拖拽神器 ItemTouchHelper
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // 排序模式下：只允许上下拖拽
                // 非排序模式下：允许左右滑动 (LEFT = 删除, RIGHT = 编辑)
                int dragFlags = isSortingMode ? (ItemTouchHelper.UP | ItemTouchHelper.DOWN) : 0;
                int swipeFlags = isSortingMode ? 0 : (ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                // 用户拖动时，通知 Adapter 交换数据
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                adapter.moveItem(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                if (direction == ItemTouchHelper.LEFT) {
                    // ====== 向左滑动：删除 ======
                    dataList.remove(position); // 从数据源移除
                    adapter.notifyItemRemoved(position); // 播放移除动画
                    saveDataToLocal(); // 保存到本地
                }
                else if (direction == ItemTouchHelper.RIGHT) {
                    // ====== 向右滑动：编辑 ======
                    // 1. 让滑动出去的卡片先弹回原位
                    adapter.notifyItemChanged(position);

                    // 2. 携带当前项的索引（position）跳转到新建/编辑页面
                    Intent intent = new Intent(MainActivity.this, AddActivity.class);
                    // 传入一个标记，告诉 AddActivity 我们现在是“编辑模式”，并且编辑的是第几个数据
                    intent.putExtra("EDIT_INDEX", position);
                    startActivity(intent);
                }
            }

            // 绘制滑动时的底色背景
            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    android.graphics.Paint paint = new android.graphics.Paint();
                    if (dX < 0) {
                        // 向左滑：画红色背景
                        paint.setColor(android.graphics.Color.parseColor("#FF3B30"));
                        c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                                (float) itemView.getRight(), (float) itemView.getBottom(), paint);
                    } else if (dX > 0) {
                        // 向右滑：画蓝色背景
                        paint.setColor(android.graphics.Color.parseColor("#007AFF"));
                        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(),
                                (float) itemView.getLeft() + dX, (float) itemView.getBottom(), paint);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // 拖拽松手后触发，这里也可以选择实时保存
                if (isSortingMode) {
                    saveDataToLocal();
                }
            }
        });
        touchHelper.attachToRecyclerView(recyclerView); // 将拖拽工具绑定到列表上

        // 1. 申请通知权限 (针对 Android 13 及以上)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        // 2. 创建通知渠道
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "MY_DAYS_CHANNEL",
                    "倒数日提醒",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("用于显示事件即将到来的提醒");
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // 3. 启动每日巡检后台任务
        // 设置任务每天执行一次 (24小时)
        androidx.work.PeriodicWorkRequest reminderRequest =
                new androidx.work.PeriodicWorkRequest.Builder(ReminderWorker.class, 24, java.util.concurrent.TimeUnit.HOURS)
                        .build();
        // 保证唯一性，不会重复启动多个相同的任务
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyReminderWork",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 读取数据逻辑不变
        SharedPreferences prefs = getSharedPreferences("MyDaysPrefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String jsonStr = prefs.getString("days_list", "[]");
        Type type = new TypeToken<ArrayList<DaysData>>() {}.getType();
        dataList = gson.fromJson(jsonStr, type);

        // 设置 Adapter
        adapter = new DaysAdapter(dataList);
        recyclerView.setAdapter(adapter);
    }

    // 新增：提取出来的保存数据方法
    private void saveDataToLocal() {
        if (dataList == null) return;
        SharedPreferences prefs = getSharedPreferences("MyDaysPrefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String newJsonStr = gson.toJson(dataList);
        prefs.edit().putString("days_list", newJsonStr).apply();
    }
}