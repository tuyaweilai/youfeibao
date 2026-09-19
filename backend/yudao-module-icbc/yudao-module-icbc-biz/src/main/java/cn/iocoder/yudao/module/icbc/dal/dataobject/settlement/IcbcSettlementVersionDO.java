package cn.iocoder.yudao.module.icbc.dal.dataobject.settlement;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 结算单版本 DO（ADR 0022）。
 *
 * <p>整单快照：结算重量 / 单价 / 调整项 / 逐条收购单明细，加上变更人、变更原因与时间。
 * 版本只追加，不覆盖；当前生效版本由 {@link IcbcSettlementDO#getCurrentVersionId()} 指。
 * 确认时把本版本的 {@code snapshotHash} 落进结算单，证明「他认可的到底是哪一版数据」。
 */
@TableName("icbc_settlement_version")
@KeySequence("icbc_settlement_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcSettlementVersionDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 结算单编号 */
    private Long settlementId;

    /** 版本号，从 1 递增 */
    private Integer versionNo;

    /** 整单快照（JSON：结算重量 / 单价 / 调整项 / 逐条收购单明细） */
    private String snapshotJson;

    /** 快照哈希（SHA-256，确认记录引用它） */
    private String snapshotHash;

    /** 本版合计结算重量 */
    private BigDecimal totalSettlementWeight;

    /** 本版合计金额 */
    private BigDecimal totalAmount;

    /** 本版收购单条数 */
    private Integer acquisitionCount;

    /** 变更原因（改版 / 不改但附说明） */
    private String changeReason;

    /** 变更人 */
    private String changedBy;

    /** 版本来源：GENERATE-生成，ENTERPRISE_CHANGE-企业改，ENTERPRISE_REPLY-企业不改但附说明 */
    private String source;

}
