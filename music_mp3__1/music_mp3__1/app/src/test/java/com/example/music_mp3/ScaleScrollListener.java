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
        int visibleItemCount = layoutManager.getChildCount();

        for (int i = 0; i < visibleItemCount; i++) {
            View itemView = recyclerView.getChildAt(i);
            if (itemView == null) continue;

            float itemCenterX = itemView.getLeft() + itemView.getWidth() / 2f;
            float recyclerCenterX = recyclerView.getWidth() / 2f;
            float offsetRatio = Math.abs(itemCenterX - recyclerCenterX) / recyclerView.getWidth();

            float scale = SCALE_CENTER - offsetRatio * (SCALE_CENTER - SCALE_SIDE);
            scale = Math.max(scale, SCALE_SIDE);

            itemView.setScaleX(scale);
            itemView.setScaleY(scale);
            itemView.setAlpha(0.4f + scale * 0.6f);
        }
    }
}