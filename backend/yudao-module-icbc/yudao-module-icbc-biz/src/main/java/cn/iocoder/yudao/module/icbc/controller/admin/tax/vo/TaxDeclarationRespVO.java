package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 代办税费申报单：本租户 × 本申报月的清单与合计金额。
 *
 * <p>财务在次月申报期前看它，{@link #status} 走到「已缴款」后 {@link #voucherNo} /
 * {@link #voucherFileUrl} 归档，{@link #invoiceCount} 张发票通过关联表挂在它下面。
 */
@Schema(description = "管理后台 - 代办税费申报单 Response VO")
@Data
public class TaxDeclarationRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "申报单号", example = "TAXDECL-202608")
    private String declarationNo;

    @Schema(description = "申报月 yyyy-MM", example = "2026-08")
    private String periodMonth;

    @Schema(description = "申报期截止日（次月 15 日）", example = "2026-09-15")
    private LocalDate declarationDeadline;

    @Schema(description = "距申报期截止日的天数（负数表示已逾期）", example = "3")
    private Integer daysLeft;

    @Schema(description = "是否已过申报期且未缴款", example = "false")
    private Boolean overdue;

    @Schema(description = "状态：0-待申报，1-已申报待缴款，2-已缴款")
    private Integer status;

    @Schema(description = "状态名称", example = "待申报")
    private String statusName;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "申报数据是否齐备")
    private Boolean dataReady;

    @Schema(description = "缺项数量")
    private Integer missingDataCount;

    @Schema(description = "涉及出售者数", example = "12")
    private Integer sellerCount;

    @Schema(description = "当月销售额超过 10 万元、需单独列出的出售者数", example = "2")
    private Integer overExemptSellerCount;

    @Schema(description = "当月净销售额合计（元）")
    private BigDecimal totalSalesAmount;

    @Schema(description = "按 3% 减按 1% 的销售额（元）")
    private BigDecimal amountAtOnePercent;

    @Schema(description = "放弃减按、按 3% 的销售额（元）")
    private BigDecimal amountAtThreePercent;

    @Schema(description = "征收率未识别的销售额（元）")
    private BigDecimal otherAmount;

    @Schema(description = "应缴增值税合计（元）")
    private BigDecimal vatAmount;

    @Schema(description = "应缴附加税费合计（元）")
    private BigDecimal surchargeAmount;

    @Schema(description = "应缴个人所得税合计（元）")
    private BigDecimal iitAmount;

    @Schema(description = "应缴税费合计（元）")
    private BigDecimal totalTaxAmount;

    @Schema(description = "实缴金额（元）")
    private BigDecimal paidAmount;

    @Schema(description = "关联发票张数")
    private Integer invoiceCount;

    @Schema(description = "申报时间")
    private LocalDateTime declaredAt;

    @Schema(description = "申报人")
    private String declaredBy;

    @Schema(description = "申报备注")
    private String declaredRemark;

    @Schema(description = "缴款时间")
    private LocalDateTime paidAt;

    @Schema(description = "缴纳方式")
    private String paymentMethod;

    @Schema(description = "缴款凭证号")
    private String voucherNo;

    @Schema(description = "缴款凭证文件地址")
    private String voucherFileUrl;

    @Schema(description = "结论（可直接展示给财务）")
    private String message;

    @Schema(description = "申报明细（生成 / 查询单份时返回）")
    private List<TaxDeclarationItemRespVO> items;

}
