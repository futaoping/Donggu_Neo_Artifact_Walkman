package com.bili.biliaudioserver.controller;

import com.bili.biliaudioserver.entity.MusicLibrary;
import com.bili.biliaudioserver.repository.MusicLibraryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/library")
@CrossOrigin // 允许跨域
public class LibraryController {

    @Autowired
    private MusicLibraryRepository libraryRepository;

    // 获取所有专辑及 BV 数据 (按专辑名分组)
    @GetMapping("/getAll")
    public Map<String, Object> getAll() {
        List<MusicLibrary> allSongs = libraryRepository.findAllByOrderByAlbumNameAsc();

        // 按 albumName 分组，提取 BV 号
        Map<String, List<String>> groupedByAlbum = allSongs.stream()
                .collect(Collectors.groupingBy(
                        MusicLibrary::getAlbumName,
                        LinkedHashMap::new, // 保持插入顺序
                        Collectors.mapping(MusicLibrary::getBv, Collectors.toList())
                ));

        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", groupedByAlbum);
        // 返回格式示例: { "物华弥新·卷一": ["BV1...", "BV2..."], "物华弥新·卷二": [...] }
        return res;
    }
    // 【新增】添加歌曲到数据库接口
    @PostMapping("/addSong")
    public Map<String, Object> addSong(@RequestBody Map<String, String> params) {
        String albumName = params.get("albumName");
        String bv = params.get("bv");

        if (albumName == null || bv == null) {
            Map<String, Object> res = new HashMap<>();
            res.put("code", 400);
            res.put("msg", "参数缺失");
            return res;
        }

        // 创建新实体并保存
        MusicLibrary newSong = new MusicLibrary();
        newSong.setAlbumName(albumName);
        newSong.setBv(bv);

        libraryRepository.save(newSong);

        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "添加成功");
        return res;
    }
    // 【新增】删除歌曲接口
    @GetMapping("/deleteSong")
    public Map<String, Object> deleteSong(@RequestParam String albumName, @RequestParam String bv) {
        Map<String, Object> res = new HashMap<>();
        try {
            // 查找并删除对应的歌曲
            List<MusicLibrary> list = libraryRepository.findAll();
            for (MusicLibrary item : list) {
                if (item.getAlbumName().equals(albumName) && item.getBv().equals(bv)) {
                    libraryRepository.delete(item);
                    break;
                }
            }
            res.put("code", 200);
            res.put("msg", "删除成功");
        } catch (Exception e) {
            res.put("code", 500);
            res.put("msg", "删除失败");
        }
        return res;
    }
}