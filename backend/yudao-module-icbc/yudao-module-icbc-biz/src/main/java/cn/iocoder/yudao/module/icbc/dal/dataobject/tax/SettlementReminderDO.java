package cn.iocoder.yudao.module.icbc.dal.dataobject.tax;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 汇算清缴提醒 DO（issue #13）。
 *
 * <p>出售者须在次年 3 月 31 日前自行汇算清缴。平台不替他申报，但要提醒他，并把
 * 「当年开了多少票、已预缴多少税」交给他——这既是他自行汇算的依据，也是回收企业
 * 提供开票与已缴税款信息这一义务的落地。一条记录 = 一个出售者 × 一个纳税年度。
 *
 * <p>提醒通过落库 + 页面可见 + 免登录对账链接触达（本产品不接站内信推送），
 * 见 {@link cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum#SETTLEMENT_STATEMENT}。
 */
@TableName("icbc_settlement_reminder")
@KeySequence("icbc_settlement_reminder_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementReminderDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 出售者档案编号 */
    private Long payeeId;

    /** 出售者姓名 */
    private String sellerName;

    /** 身份证号码 */
    private String idCardNo;

    /** 纳税年度 */
    private Integer taxYear;

    /** 汇算清缴截止日：次年 3 月 31 日 */
    private LocalDate deadline;

    /** 当年开票张数 */
    private Integer invoiceCount;

    /** 当年开票金额（元） */
    private BigDecimal invoicedAmount;

    /** 当年已预缴税费合计（元） */
    private BigDecimal paidTaxAmount;

    /** 当年已预缴个人所得税（元） */
    private BigDecimal iitAmount;

    /** 状态，见 {@link cn.iocoder.yudao.module.icbc.enums.SettlementReminderStatusEnum} */
    private Integer status;

    /** 提醒时间（生成 / 触达） */
    private LocalDateTime remindedAt;

    /** 处理说明 */
    private String handleRemark;

    /** 备注 */
    private String remark;

}
