package com.example.music_mp3;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlbumPagerAdapter extends RecyclerView.Adapter<AlbumPagerAdapter.AlbumVH> {
    private final Context context;
    private final List<Album> albumList;
    private final OnAlbumPageCreateListener listener;

    public interface OnAlbumPageCreateListener {
        void onPageCreate(View pageView, Album album, int pageIndex);
    }

    public AlbumPagerAdapter(Context ctx, List<Album> list, OnAlbumPageCreateListener l) {
        context = ctx;
        albumList = list;
        listener = l;
    }

    @NonNull
    @Override
    public AlbumVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 【关键修改】：这里必须加载单页布局 item_album_page，不能再加载 activity_main
        View view = LayoutInflater.from(context).inflate(R.layout.item_album_page, parent, false);
        return new AlbumVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumVH holder, int position) {
        Album album = albumList.get(position);
        if(listener != null){
            // 触发 MainActivity 中的 initSingleAlbumPage 方法
            listener.onPageCreate(holder.itemView, album, position);
        }
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    public static class AlbumVH extends RecyclerView.ViewHolder{
        public AlbumVH(@NonNull View itemView) {
            super(itemView);
        }
    }
}