package com.ybc.mydays;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 首页倒数日列表适配器
 * 负责将 DaysData 数据模型绑定到 home_list 布局，并处理点击与拖拽排序。
 */
public class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.DaysViewHolder> {

    // ================= 常量定义 (性能优化) =================
    // 使用 16 进制字面量直接定义颜色，避免在滑动时频繁解析字符串
    // 格式为 0xAARRGGBB (AA为透明度，FF表示完全不透明)
    private static final int COLOR_ORANGE = 0xFFFF9500;
    private static final int COLOR_RED = 0xFFFF3B30;
    private static final int COLOR_BLUE = 0xFF007AFF;

    private final List<DaysData> dataList;

    public DaysAdapter(List<DaysData> dataList) {
        this.dataList = dataList;
    }

    /**
     * 处理列表项拖拽排序
     * @param fromPosition 起始位置
     * @param toPosition   目标位置
     */
    public void moveItem(int fromPosition, int toPosition) {
        // 最标准、安全的集合元素移动算法
        DaysData item = dataList.remove(fromPosition);
        dataList.add(toPosition, item);
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
        DaysData data = dataList.get(position);
        holder.tvName.setText(data.getMatterName());

        // 计算真实天数
        long finalDays = DaysUntil.calculateDays(
                data.getMatterYear(), data.getMatterMonth(), data.getMatterDay(),
                data.getRepeatIndex(), data.isCountUp()
        );

        // 渲染文案与颜色
        if (data.isCountUp()) {
            // 累计日模式 (正数)
            holder.tvRestDays.setText(finalDays + " 天");
            holder.tvRestDays.setTextColor(COLOR_ORANGE);
        } else {
            // 倒数日模式
            if (finalDays < 0 && data.getRepeatIndex() == 0) {
                // 不重复且已过期
                holder.tvRestDays.setText("已超 " + Math.abs(finalDays) + " 天");
                holder.tvRestDays.setTextColor(COLOR_RED);
            } else {
                // 尚未过期
                holder.tvRestDays.setText(finalDays + " 天");
                holder.tvRestDays.setTextColor(COLOR_BLUE);
            }
        }

        // 绑定点击事件，跳转详情页
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DetailActivity.class);
            intent.putExtra("DETAIL_INDEX", holder.getAdapterPosition());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    /**
     * 视图持有者
     */
    public static class DaysViewHolder extends RecyclerView.ViewHolder {

        final TextView tvName;
        final TextView tvRestDays;

        public DaysViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.list_name);
            tvRestDays = itemView.findViewById(R.id.list_restDays);
        }
    }
}