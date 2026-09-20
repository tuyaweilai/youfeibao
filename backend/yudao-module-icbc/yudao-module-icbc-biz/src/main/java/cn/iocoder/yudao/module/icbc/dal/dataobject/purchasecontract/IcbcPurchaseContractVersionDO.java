package cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 采购合同版本快照 DO（#45 / T07）。
 *
 * <p>每次送审落一版整份合同快照（含适用品类），只追加、不覆盖；审核结论写在那一版上。
 * 历史版本可回查，能回答「当时约定的是什么、谁审的、过了还是被驳回」。
 */
@TableName("icbc_purchase_contract_version")
@KeySequence("icbc_purchase_contract_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcPurchaseContractVersionDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 采购合同编号 */
    private Long contractId;

    /** 版本号，从 1 递增 */
    private Integer versionNo;

    /** 整份合同快照（JSON，含适用品类） */
    private String snapshotJson;

    /** 快照哈希（SHA-256，证明历史版本未被改动） */
    private String snapshotHash;

    /** 本版变更原因 */
    private String changeReason;

    /** 送审人 */
    private String changedBy;

    /** 本版审核状态，枚举 {@code PurchaseContractAuditStatusEnum} */
    private Integer auditStatus;

    /** 本版审核人 */
    private Long auditedBy;

    /** 本版审核时间 */
    private java.time.LocalDateTime auditedTime;

    /** 本版审核意见 */
    private String auditRemark;

}
