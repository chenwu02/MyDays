package com.ybc.mydays;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;

// 注意这里继承的是 RecyclerView.Adapter
public class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.DaysViewHolder> {

    private List<DaysData> dataList;

    public DaysAdapter(List<DaysData> dataList) {
        this.dataList = dataList;
    }

    // 提供给外部的拖动排序方法
    public void moveItem(int fromPosition, int toPosition) {
        // 在数据源中交换位置
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(dataList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(dataList, i, i - 1);
            }
        }
        // 通知界面执行动画更新
        notifyItemMoved(fromPosition, toPosition);
    }

    @NonNull
    @Override
    public DaysViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_list, parent, false);
        return new DaysViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DaysViewHolder holder, int position) {
        DaysData daysData = dataList.get(position);
        holder.list_name.setText(daysData.getMatterName());

        // 调用新算法
        long finalDays = DaysUntil.calculateDays(
                daysData.getMatterYear(), daysData.getMatterMonth(),
                daysData.getMatterDay(), daysData.getRepeatIndex(), daysData.isCountUp()
        );

        // 设置颜色和文字
        if (daysData.isCountUp()) {
            holder.list_restDays.setText(finalDays + " 天");
            holder.list_restDays.setTextColor(Color.parseColor("#FF9500")); // 橙色
        } else {
            if (finalDays < 0 && daysData.getRepeatIndex() == 0) {
                holder.list_restDays.setText("已超 " + Math.abs(finalDays) + " 天");
                holder.list_restDays.setTextColor(Color.parseColor("#FF3B30")); // 红色
            } else {
                holder.list_restDays.setText(finalDays + " 天");
                holder.list_restDays.setTextColor(Color.parseColor("#007AFF")); // 蓝色
            }
        }

        // 点击进入详情页
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), DetailActivity.class);
            // 将用户点击的是第几个项目告诉详情页
            intent.putExtra("DETAIL_INDEX", holder.getAdapterPosition());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    // 强制要求的 ViewHolder
    public static class DaysViewHolder extends RecyclerView.ViewHolder {
        TextView list_name;
        TextView list_restDays;

        public DaysViewHolder(@NonNull View itemView) {
            super(itemView);
            list_name = itemView.findViewById(R.id.list_name);
            list_restDays = itemView.findViewById(R.id.list_restDays);
        }
    }
}