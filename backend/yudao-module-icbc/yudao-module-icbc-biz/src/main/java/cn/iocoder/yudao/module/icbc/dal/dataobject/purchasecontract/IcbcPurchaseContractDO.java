package cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购合同 DO（#45 / T07，ADR 0027）。
 *
 * <p>粒度是「一个合同 → 多个采购订单 → 多次收货」。合同**需要审核**，审核通过前不得作为
 * 有效采购依据（{@code assertUsableAsPurchaseBasis} 是唯一门禁）。
 *
 * <p>与 {@code icbc_framework_agreement}（框架收购协议）是两件事：后者是自然人出售者对开票与
 * 代办税费的授权附件，是**开票前置**；本合同是**采购条款**。两者不合并。
 *
 * <p>交易对方沿用 ADR 0029 的「主体类型六态」：{@link #counterpartyType} 属于自然人时
 * {@link #payeeId} 指向 {@code icbc_payee_info}，其余五类 {@link #supplierId} 指向
 * {@code erp_supplier}，恰好一个非空。{@link #counterpartyName} 是签约时的名称快照，
 * 对方改名不影响历史合同。
 *
 * <p>状态见 {@code PurchaseContractStatusEnum}。{@link #versionNo} 是「已送审的最新版本号」，
 * 每次送审落一版快照到 {@code icbc_purchase_contract_version}（只追加）。过期不落库，
 * 由 {@link #endDate} 推导。
 */
@TableName("icbc_purchase_contract")
@KeySequence("icbc_purchase_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcPurchaseContractDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 合同编号（平台生成，租户内唯一） */
    private String contractNo;

    /** 合同名称 */
    private String name;

    /** 交易对方主体类型，枚举 {@code SellerSubjectTypeEnum}（六态，判定规则是「是否属于自然人」） */
    private Integer counterpartyType;

    /** 自然人出售者档案编号（主体类型为自然人时非空） */
    // updateStrategy = ALWAYS：换对手方时要把另一个 id 真正清成 NULL，
    // MyBatis-Plus 默认 NOT_NULL 策略会跳过 null，清不掉。所有更新路径都传整份 DO，故安全。
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long payeeId;

    /** 单位供货方编号（主体类型为非自然人时非空，指向 erp_supplier） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long supplierId;

    /** 交易对方名称快照（签约时固化，对方改名不影响历史合同） */
    private String counterpartyName;

    /** 有效期起 */
    private LocalDate startDate;

    /** 有效期止（早于今天即视为过期） */
    private LocalDate endDate;

    /** 数量约定 */
    private String quantityAgreement;

    /** 计量标准（毛重 / 皮重 / 扣杂口径） */
    private String measureStandard;

    /** 质量标准（等级 / 杂质 / 水分等验收指标） */
    private String qualityStandard;

    /** 价格规则（固定单价 / 按交货日价格表等约定） */
    private String priceRule;

    /** 运输责任 */
    private String transportResponsibility;

    /** 付款条款 */
    private String paymentTerms;

    /** 附件地址（多个用英文逗号分隔） */
    private String attachmentUrls;

    /** 状态，枚举 {@code PurchaseContractStatusEnum} */
    private Integer status;

    /** 已送审的最新版本号（0 表示尚未送审） */
    private Integer versionNo;

    /** 最近一次送审人 */
    private Long submittedBy;

    /** 最近一次送审时间 */
    private LocalDateTime submittedTime;

    /** 审核人 */
    private Long auditedBy;

    /** 审核时间 */
    private LocalDateTime auditedTime;

    /** 审核意见 */
    private String auditRemark;

    /** 关闭人 */
    private Long closedBy;

    /** 关闭时间 */
    private LocalDateTime closedTime;

    /** 关闭原因 */
    private String closeReason;

    /** 备注 */
    private String remark;

}
