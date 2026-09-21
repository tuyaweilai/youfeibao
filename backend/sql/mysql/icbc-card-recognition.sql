-- 连接字符集：文件里有中文注释与种子。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- ========================================
-- 卡证识别：平台级配置（#103，ADR 0037 Consequences「后台要落配置项：OCR 的供应商 / 密钥 / 地域 / 超时」）
--
-- 与电子签章**各立一处**（ADR 0037）：OCR 的额度与密钥是平台共享的，回收企业不该自己配腾讯密钥，
-- 所以这张表**没有租户维度**，是平台级唯一一行。它必须登记进 `yudao.tenant.ignore-tables`
-- （见 `backend/yudao-server/src/main/resources/application.yaml`），否则租户上下文会把行过滤掉，
-- 后台页面永远显示「未配置」。
--
-- **密钥只落后端、界面不回显明文**：`secret_id` / `secret_key` 只写不读——保存时留空表示「不改动」，
-- 查询响应里只回「已配置」与否。
--
-- `provider` 取代了 #93 的启动期 `icbc.card-recognition.mode`：它现在是**运行期**判定的一栏
-- （`stub` / `tencent`），保存后无需重启即生效。DB 为空时回落 yaml / env（页面标注「来自配置文件」）。
--
-- `last_check_result` / `last_check_time` 是连通性自检的分类结果（OK / AUTH_FAILED / NETWORK /
-- VENDOR_ERROR）。**只存分类**：不存密钥、不整段存厂商原始报文（它是给人看的状态，不是调试垃圾桶）。
--
-- 幂等：CREATE TABLE IF NOT EXISTS。
-- ========================================
CREATE TABLE IF NOT EXISTS `icbc_card_recognition_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider` varchar(16) DEFAULT NULL COMMENT '供应商：stub-未启用（现场手工录入）/ tencent-腾讯云 OCR',
  `secret_id` varchar(128) DEFAULT NULL COMMENT '腾讯云 API 密钥 SecretId（只写不读，界面不回显明文）',
  `secret_key` varchar(255) DEFAULT NULL COMMENT '腾讯云 API 密钥 SecretKey（只写不读，界面不回显明文）',
  `region` varchar(64) DEFAULT NULL COMMENT '地域（OCR 是全局服务，签名需要，如 ap-guangzhou）',
  `endpoint` varchar(255) DEFAULT NULL COMMENT 'OCR 服务域名（如 ocr.tencentcloudapi.com）',
  `timeout` int DEFAULT NULL COMMENT '接口超时（毫秒）',
  `last_check_result` varchar(32) DEFAULT NULL COMMENT '最近一次连通性自检分类：OK / AUTH_FAILED / NETWORK / VENDOR_ERROR',
  `last_check_time` datetime DEFAULT NULL COMMENT '最近一次连通性自检时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卡证识别平台级参数（平台级唯一一份，不属任何租户）';
