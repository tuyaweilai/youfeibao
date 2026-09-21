package cn.iocoder.yudao.module.icbc.dal.dataobject.payee;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.icbc.enums.PayeeBankCardChangeStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 出售者换银行卡 DO（收款账户变更，#37）。
 *
 * <p>收方档案（{@link PayeeInfoDO}）里的 {@code bankCardNo} 是**唯一生效中的那张卡**；
 * 本表只承载「在途 / 历史」的变更：新卡审核通过时才把 {@code newBankCardNo} 搬到档案上。
 * 这样「不允许多张卡」是结构上成立的，而不是靠约定。
 *
 * @see cn.iocoder.yudao.module.icbc.enums.PayeeBankCardChangeStatusEnum
 */
@TableName("icbc_payee_bank_card_change")
@KeySequence("icbc_payee_bank_card_change_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcPayeeBankCardChangeDO extends TenantBaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 变更单号（租户内唯一）
     */
    private String changeNo;

    /**
     * 收方（出售者）档案编号
     */
    private Long payeeId;

    /**
     * 自然人主体编号（平台级身份）
     */
    private Long naturalPersonId;

    /**
     * 状态，枚举 {@link PayeeBankCardChangeStatusEnum}
     */
    private Integer status;

    /**
     * 原卡尾号快照（钱原本要打到的卡）
     */
    private String oldCardTail;

    /**
     * 待变更的新银行卡号（审核通过前不生效）
     */
    private String newBankCardNo;

    /**
     * 新卡开户银行
     */
    private String newBankName;

    /**
     * 新卡开户支行
     */
    private String newBankBranch;

    /**
     * 是否我行用户：0-非我行用户，1-我行用户（为空按 1 上送，见 {@link cn.iocoder.yudao.module.icbc.enums.IcbcAccountCodeEnum}）
     */
    private String accountCode;

    /**
     * 证件签发日期 yyyy-MM-dd（收方入驻入参快照）
     */
    private String idSignDate;

    /**
     * 证件截止日期 yyyy-MM-dd（收方入驻入参快照）
     */
    private String idValidityPeriod;

    /**
     * 工行审核结果（原样透传）：pass / reject
     */
    private String auditResult;

    /**
     * 审核拒绝原因 / 取消原因
     */
    private String rejectReason;

    /**
     * 发起来源：SELLER_PORTAL / FIELD / ADMIN
     */
    private String requestSource;

    /**
     * 发起 IP（留痕）
     */
    private String requestIp;

    /**
     * 发起时间
     */
    private LocalDateTime requestedAt;

    /**
     * 有结果时间（生效 / 拒绝 / 取消）
     */
    private LocalDateTime resolvedAt;

    /**
     * 备注
     */
    private String remark;

}
