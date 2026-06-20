package com.example.music_mp3;

public class BiliVideoBean {
    private String title;
    private String upName;
    private String coverUrl;
    private long cid;
    private String audioProxyUrl;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUpName() {
        return upName;
    }

    public void setUpName(String upName) {
        this.upName = upName;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public long getCid() {
        return cid;
    }

    public void setCid(long cid) {
        this.cid = cid;
    }

    public String getAudioProxyUrl() {
        return audioProxyUrl;
    }

    public void setAudioProxyUrl(String audioProxyUrl) {
        this.audioProxyUrl = audioProxyUrl;
    }
}