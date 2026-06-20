package com.bili.biliaudioserver.entity;

public class BiliVideoInfo {
    // 视频标题
    private String title;
    // UP主
    private String upName;
    // 封面图直链
    private String coverUrl;
    // cid
    private Long cid;
    // 后端音频代理接口地址
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

    public Long getCid() {
        return cid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public String getAudioProxyUrl() {
        return audioProxyUrl;
    }

    public void setAudioProxyUrl(String audioProxyUrl) {
        this.audioProxyUrl = audioProxyUrl;
    }
}