-- 测试用数据库表创建脚本

-- 1. 市场价格基准表
CREATE TABLE IF NOT EXISTS `waste_price_benchmark` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '价格基准ID',
  `waste_code` varchar(50) NOT NULL COMMENT '危险废物代码',
  `waste_name` varchar(100) NOT NULL COMMENT '危险废物名称',
  `price` decimal(10,2) NOT NULL COMMENT '基准价格',
  `price_unit` varchar(20) NOT NULL COMMENT '价格单位 (如:元/桶,元/吨,元/千克)',
  `region_code` varchar(20) DEFAULT '' COMMENT '适用地区编码 (空为全国通用)',
  `region_name` varchar(100) DEFAULT '' COMMENT '适用地区名称',
  `effective_date` date NOT NULL COMMENT '生效日期',
  `expire_date` date DEFAULT NULL COMMENT '失效日期',
  `price_source` varchar(100) DEFAULT '' COMMENT '价格来源 (如:市场调研,政府指导价等)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 (0:草稿, 1:生效中, 2:已失效)',
  `remark` varchar(255) DEFAULT '' COMMENT '备注说明',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` boolean NOT NULL DEFAULT false COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危险废物市场价格基准表'; 