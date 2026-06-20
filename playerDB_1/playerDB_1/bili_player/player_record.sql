USE bili_player;

CREATE TABLE `music_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bv` varchar(50) NOT NULL COMMENT 'B站BV号',
  `title` varchar(255) DEFAULT NULL COMMENT '视频标题',
  `up_name` varchar(100) DEFAULT NULL COMMENT 'UP主名称',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图URL',
  `audio_url` varchar(500) DEFAULT NULL COMMENT '音频代理URL',
  `record_type` tinyint NOT NULL COMMENT '记录类型: 1-播放历史, 2-我的收藏',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_bv_type` (`bv`, `record_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户音乐播放与收藏记录表';