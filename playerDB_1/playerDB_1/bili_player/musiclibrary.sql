CREATE TABLE `music_library` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `album_name` varchar(50) NOT NULL COMMENT '专辑名称',
  `bv` varchar(20) NOT NULL COMMENT 'BV号',
  `title` varchar(100) DEFAULT NULL COMMENT '歌曲标题',
  `singer` varchar(50) DEFAULT NULL COMMENT '歌手/UP主',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '封面链接',
  `audio_url` varchar(255) DEFAULT NULL COMMENT '音频链接',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入你原来的数据 (album_name 可以自定义，比如 "专辑1", "专辑2")
INSERT INTO `music_library` (`album_name`, `bv`) VALUES 
('物华弥新·卷一', 'BV1MnSEBxEji'), ('物华弥新·卷一', 'BV1RRbszXEyW'), ('物华弥新·卷一', 'BV1ynGa6wEVs'),
('物华弥新·卷二', 'BV1wk3LzFEBY'), ('物华弥新·卷二', 'BV1M6jnzeEaG'), ('物华弥新·卷二', 'BV18p9jY6E6F'),
('物华弥新·卷三', 'BV1HvkMYgEzC'), ('物华弥新·卷三', 'BV1tSDkYTErT'), ('物华弥新·卷三', 'BV1A2C1YHEes');