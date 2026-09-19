package cn.iocoder.yudao.module.icbc.dal.dataobject.tax;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 需补缴税费 DO（issue #13）。
 *
 * <p>补缴是申报之后才暴露出来的差额：申报单已缴款后重新计算发现销售额增加、跨期红冲调整、
 * 或事后被要求补税。它不能悄悄改掉一份已缴清的申报单，所以单独立一条，可跟进、可结案。
 * 待补缴累计金额按 1% 与 3% 分列，正是为了填申报表时对得上征收率。
 */
@TableName("icbc_tax_supplement")
@KeySequence("icbc_tax_supplement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxSupplementDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 补缴单号 */
    private String supplementNo;

    /** 关联申报单编号（可空：手工登记时可不挂） */
    private Long declarationId;

    /** 所属申报月 yyyy-MM */
    private String periodMonth;

    /** 出售者档案编号（可空） */
    private Long payeeId;

    /** 出售者姓名（可空） */
    private String sellerName;

    /** 补缴原因 */
    private String reason;

    /** 按 3% 减按 1% 部分对应的补缴金额（元） */
    private BigDecimal amountAtOnePercent;

    /** 放弃减按、按 3% 部分对应的补缴金额（元） */
    private BigDecimal amountAtThreePercent;

    /** 应补缴金额合计（元） */
    private BigDecimal amount;

    /** 状态，见 {@link cn.iocoder.yudao.module.icbc.enums.TaxSupplementStatusEnum} */
    private Integer status;

    /** 实缴金额（元） */
    private BigDecimal paidAmount;

    /** 缴款时间 */
    private LocalDateTime paidAt;

    /** 缴款凭证号 */
    private String voucherNo;

    /** 缴款凭证文件地址 */
    private String voucherFileUrl;

    /** 备注 */
    private String remark;

}
