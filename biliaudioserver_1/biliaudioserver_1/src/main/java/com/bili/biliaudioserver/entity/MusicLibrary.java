package com.bili.biliaudioserver.entity;

import javax.persistence.*;

@Entity
@Table(name = "music_library")
public class MusicLibrary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "album_name")
    private String albumName;

    private String bv;

    // 必须生成 Getter 和 Setter (或者用 Lombok 的 @Data)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAlbumName() { return albumName; }
    public void setAlbumName(String albumName) { this.albumName = albumName; }
    public String getBv() { return bv; }
    public void setBv(String bv) { this.bv = bv; }
}