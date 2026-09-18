-- 物流运输模块数据库表结构脚本（完整版）
-- 创建日期：2024年05月
-- 更新日期：2024年05月
-- 版本：V2.1

-- 确保使用了正确的字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 物流模块主表
source logistics-schema-part1.sql;

-- 过磅和代付表
source logistics-schema-part2.sql;

-- 基础数据表（车辆和司机资质）
source logistics-schema-part3.sql;

-- 扩展功能表
source logistics-schema-part4.sql;

-- 添加系统配置信息
INSERT INTO `infra_config` (`id`, `category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1001, 'logistics', 1, '过磅异常检测阈值(%)', 'logistics.weighing.anomaly.threshold.percent', '10', '0', '过磅重量与预估重量差异百分比阈值', 'admin', NOW(), 'admin', NOW(), b'0'),
(1002, 'logistics', 1, '车辆轨迹上报间隔(秒)', 'logistics.track.report.interval.seconds', '60', '0', '车辆轨迹定位上报时间间隔', 'admin', NOW(), 'admin', NOW(), b'0'),
(1003, 'logistics', 1, '在途预警时长(小时)', 'logistics.in-transit.alert.duration.hours', '24', '0', '车辆在途时长超过该值触发预警', 'admin', NOW(), 'admin', NOW(), b'0'),
(1004, 'logistics', 1, '任务超时提醒(小时)', 'logistics.task.timeout.alert.hours', '2', '0', '运输任务各节点超时提醒阈值', 'admin', NOW(), 'admin', NOW(), b'0'),
(1005, 'logistics', 1, '扫街回收临时订单有效期(天)', 'logistics.temporary-order.validity.days', '7', '0', '临时订单有效期，超过后需重新创建', 'admin', NOW(), 'admin', NOW(), b'0');

-- 注意：此处不再添加物流公司示例数据，因为使用系统现有的企业表

SET FOREIGN_KEY_CHECKS = 1; 