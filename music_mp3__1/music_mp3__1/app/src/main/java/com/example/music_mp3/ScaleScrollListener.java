package com.example.music_mp3;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ScaleScrollListener extends RecyclerView.OnScrollListener {
    private static final float SCALE_CENTER = 1.0f;
    private static final float SCALE_SIDE = 0.75f;
    private final LinearLayoutManager layoutManager;

    public ScaleScrollListener(LinearLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        // 防止除以0
        if (recyclerView.getWidth() == 0) return;

        int visibleItemCount = layoutManager.getChildCount();
        // 获取 RecyclerView 的中心点 X 坐标
        float recyclerCenterX = recyclerView.getWidth() / 2f;

        for (int i = 0; i < visibleItemCount; i++) {
            View itemView = recyclerView.getChildAt(i);
            if (itemView == null) continue;

            float itemCenterX = itemView.getLeft() + itemView.getWidth() / 2f;

            // 【修改点】：把分母改小，让缩放效果更明显
            // 原来除以总宽度，现在除以总宽度的 1/3 (或者你可以试 1/2)
            // 意思是：当卡片偏离中心 1/3 屏幕宽度时，缩放达到最小值 0.75
            float maxDistance = recyclerView.getWidth() / 3f;
            float offsetRatio = Math.abs(itemCenterX - recyclerCenterX) / maxDistance;

            // 计算缩放比例
            float scale = SCALE_CENTER - offsetRatio * (SCALE_CENTER - SCALE_SIDE);
            scale = Math.max(scale, SCALE_SIDE); // 限制最小缩放

            itemView.setScaleX(scale);
            itemView.setScaleY(scale);
            // 透明度也跟随缩放变化
            itemView.setAlpha(0.4f + scale * 0.6f);
        }
    }
}