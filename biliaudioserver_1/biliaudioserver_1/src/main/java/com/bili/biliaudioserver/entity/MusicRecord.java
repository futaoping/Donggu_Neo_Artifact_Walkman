package com.bili.biliaudioserver.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "music_record")
public class MusicRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bv;
    private String title;
    private String upName;
    private String coverUrl;
    private String audioUrl;
    private Integer recordType; // 1-历史, 2-收藏
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) this.createTime = LocalDateTime.now();
    }

    // 省略 Getter 和 Setter (IDEA 中按 Alt+Insert 快速生成，或者手动写)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBv() { return bv; }
    public void setBv(String bv) { this.bv = bv; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUpName() { return upName; }
    public void setUpName(String upName) { this.upName = upName; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public Integer getRecordType() { return recordType; }
    public void setRecordType(Integer recordType) { this.recordType = recordType; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}