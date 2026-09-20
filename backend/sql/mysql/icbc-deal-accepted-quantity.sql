-- #58：采购订单成交记录区分「验收量」与「结算量」。
--
-- 背景：成交记录（`icbc_purchase_order_deal`）原先只有一个 `quantity`，被「验收」与「结算」两个口径共用。
-- 关联收购单后这两个口径会分叉——验收是**实物接收量**（毛重 − 皮重 − 拒收，ADR 0028 / #53 的
-- `accepted_weight`），结算是**计价基准**（毛重 − 皮重 − 扣杂，ADR 0019 的 `settlement_weight`）。
-- 所以加一列 `accepted_quantity`：验收口径取它（为空时回退 `quantity`，兼容历史数据），
-- 结算口径仍取 `quantity`。
--
-- 叠加在 `erp.sql` 之外，幂等：先查 information_schema 再执行。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `icbc_t58_add_accepted_quantity`;

DELIMITER $$
CREATE PROCEDURE `icbc_t58_add_accepted_quantity`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'icbc_purchase_order_deal'
                     AND column_name = 'accepted_quantity') THEN
        ALTER TABLE `icbc_purchase_order_deal`
            ADD COLUMN `accepted_quantity` decimal(16, 4) NULL COMMENT '验收量（实物接收量；为空时按 quantity 计，兼容历史数据）' AFTER `quantity`;
    END IF;
END$$
DELIMITER ;

CALL `icbc_t58_add_accepted_quantity`();

DROP PROCEDURE IF EXISTS `icbc_t58_add_accepted_quantity`;
