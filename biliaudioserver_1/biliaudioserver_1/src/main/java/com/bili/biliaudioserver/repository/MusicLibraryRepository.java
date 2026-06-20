package com.bili.biliaudioserver.repository;

import com.bili.biliaudioserver.entity.MusicLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MusicLibraryRepository extends JpaRepository<MusicLibrary, Long> {
    // 按专辑名排序查询
    List<MusicLibrary> findAllByOrderByAlbumNameAsc();
}
