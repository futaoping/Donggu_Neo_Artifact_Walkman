package com.bili.biliaudioserver.controller;

import com.bili.biliaudioserver.entity.MusicRecord;
import com.bili.biliaudioserver.repository.MusicRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime; // 【新增导入】用于获取当前时间
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/record")
@CrossOrigin // 允许跨域
public class RecordController {

    @Autowired
    private MusicRecordRepository recordRepository;

    /**
     * 1. 添加/更新播放历史（去重并置顶）
     * 逻辑：如果数据库已有该BV号记录，则更新其播放时间为“现在”，使其在倒序查询时排在最前；
     *       如果没有，则作为新记录插入。
     */
    @PostMapping("/addHistory")
    public Map<String, Object> addHistory(@RequestBody MusicRecord record) {
        record.setRecordType(1); // 1代表历史

        // 【核心逻辑】：查询数据库里是否已经有这首歌的历史记录
        MusicRecord existingRecord = recordRepository.findByBvAndRecordType(record.getBv(), 1);

        if (existingRecord != null) {
            // 如果已经存在（重复播放），只更新它的播放时间为“当前时间”
            // 这样在按时间倒序查询时，它就会自动跑到最上面
            existingRecord.setCreateTime(LocalDateTime.now());

            // 顺便更新一下歌曲信息（防止B站那边改了标题或封面）
            existingRecord.setTitle(record.getTitle());
            existingRecord.setUpName(record.getUpName());
            existingRecord.setCoverUrl(record.getCoverUrl());
            existingRecord.setAudioUrl(record.getAudioUrl());

            recordRepository.save(existingRecord);
        } else {
            // 如果不存在（第一次播放），直接插入新记录
            recordRepository.save(record);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "记录成功");
        return res;
    }

    /**
     * 2. 获取列表 (type=1历史, type=2收藏)
     * 按创建时间倒序排列，最新的在最上面
     */
    @GetMapping("/getList")
    public Map<String, Object> getList(@RequestParam Integer type) {
        List<MusicRecord> list = recordRepository.findByRecordTypeOrderByCreateTimeDesc(type);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", list);
        return res;
    }
    // 3. 删除单条历史记录（改用先查后删，确保 100% 生效）
    @DeleteMapping("/deleteHistory")
    public Map<String, Object> deleteHistory(@RequestParam String bv) {
        // 先根据 BV 号查出这条记录
        MusicRecord record = recordRepository.findByBvAndRecordType(bv, 1);

        if (record != null) {
            // 如果找到了，直接删除这个实体对象
            recordRepository.delete(record);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "删除成功");
        return res;
    }

    // 4. 清空所有历史记录
    @DeleteMapping("/clearHistory")
    public Map<String, Object> clearHistory() {
        List<MusicRecord> historyList = recordRepository.findByRecordTypeOrderByCreateTimeDesc(1);
        recordRepository.deleteAll(historyList);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "清空成功");
        return res;
    }
}