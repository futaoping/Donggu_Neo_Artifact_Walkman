package com.example.music_mp3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class SongWheelAdapter extends RecyclerView.Adapter<SongWheelAdapter.ViewHolder> {

    private final List<Music> musicList;
    private final OnItemClickListener listener;
    private final int totalCount;

    // 【新增】长按监听变量
    private OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onLongClick(int position, Music music);
    }

    public interface OnItemClickListener {
        void onItemClick(int realPosition);
    }

    // 【修改】构造函数增加第三个参数：长按监听器
    public SongWheelAdapter(List<Music> musicList, OnItemClickListener listener, OnItemLongClickListener longClickListener) {
        this.musicList = musicList;
        this.listener = listener;
        this.longClickListener = longClickListener; // 赋值
        // 防止空列表时计算 totalCount 出错
        this.totalCount = musicList.isEmpty() ? 0 : musicList.size() * 100;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song_wheel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 你的防崩溃逻辑：列表为空直接返回
        if (musicList == null || musicList.isEmpty()) return;

        int realPos = position % musicList.size();
        Music music = musicList.get(realPos);

        Glide.with(holder.itemView.getContext()).clear(holder.ivWheelCover);
        holder.ivWheelCover.setTag(position);
        holder.tvWheelSongName.setText(music.getName());

        String coverUrl = music.getCoverUrl();
        if (coverUrl != null && !coverUrl.trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(coverUrl)
                    .centerCrop()
                    .into(holder.ivWheelCover);
        } else {
            holder.ivWheelCover.setImageDrawable(null);
        }

        // 原有的点击事件
        holder.itemView.setOnClickListener(v -> listener.onItemClick(realPos));

        // 【新增】长按事件 (系统默认长按约 0.5~1 秒触发)
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    // 传入真实的 Music 对象给 Activity 处理
                    longClickListener.onLongClick(pos, musicList.get(pos % musicList.size()));
                }
            }
            return true; // 返回 true 表示消费了长按事件，不再向下传递
        });
    }

    @Override
    public int getItemCount() {
        // 你的防崩溃逻辑：空列表返回 0
        if (musicList == null || musicList.isEmpty()) return 0;
        return totalCount;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWheelSongName;
        ImageView ivWheelCover;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWheelSongName = itemView.findViewById(R.id.tvWheelSongName);
            ivWheelCover = itemView.findViewById(R.id.iv_item_cover);
        }
    }
}