#create database dubbo_admin;

use dubbo_admin;
# UNIQUE KEY 最大长度 191
CREATE TABLE `registry` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `reg_name` varchar(100) NOT NULL DEFAULT '' COMMENT '名称',
  `reg_address` varchar(180) NOT NULL DEFAULT '' COMMENT '注册中心地址',
  `reg_group` varchar(100) NOT NULL DEFAULT '' COMMENT '组',
  `app_code` varchar(100) NOT NULL DEFAULT '' COMMENT '应用唯一编码:应用中心创建',
  `env` smallint(2) NOT NULL DEFAULT '0' COMMENT '环境:1 测试 2 预发 3 生产',
  `state` smallint(2) NOT NULL DEFAULT '0' COMMENT '连接状态 0 下线 1 在线',
  `auto` smallint(2) NOT NULL DEFAULT '0' COMMENT '是否自动连接 0 否 1 是',
  PRIMARY KEY (`id`),
  UNIQUE KEY uniq_index_reg_address(`reg_address`),
  UNIQUE KEY uniq_index_app_code(`app_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '注册中心配置';