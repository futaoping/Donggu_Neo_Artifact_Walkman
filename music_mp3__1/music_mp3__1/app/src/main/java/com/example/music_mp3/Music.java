package com.example.music_mp3;

public class Music {
    private int id;
    private String bv;      // 【新增】B站BV号
    private String name;
    private String singer;
    private String url;
    private String coverUrl;

    // 【新增】bv 的 getter 和 setter
    public String getBv() { return bv; }
    public void setBv(String bv) { this.bv = bv; }

    // ... 下面保留你原来的其他 getter 和 setter ...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSinger() { return singer; }
    public void setSinger(String singer) { this.singer = singer; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
}