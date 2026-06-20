package com.example.music_mp3;

public class Album {
    public String[] bvList;
    public String albumName; // 【新增】专辑名称

    public Album(String[] bvList) {
        this.bvList = bvList;
    }

    // 【新增】Setter 方法
    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }
}