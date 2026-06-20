package com.bili.biliaudioserver.repository;

import com.bili.biliaudioserver.entity.MusicLibrary;
import com.bili.biliaudioserver.entity.MusicRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MusicRecordRepository extends JpaRepository<MusicRecord, Long> {
    // 按时间倒序查询历史记录或收藏
    List<MusicRecord> findByRecordTypeOrderByCreateTimeDesc(Integer recordType);

    // 查询是否已收藏
    MusicRecord findByBvAndRecordType(String bv, Integer recordType);
    // 按专辑名排序查询

    // 删除收藏
    void deleteByBvAndRecordType(String bv, Integer recordType);
}