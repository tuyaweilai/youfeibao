package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代办税费申报明细：一个出售者在申报月内的一条。
 *
 * <p>{@link #overExempt} 为真即「当月销售额超过 10 万元、需要单独列出代办申报」的那条。
 */
@Schema(description = "管理后台 - 代办税费申报明细 Response VO")
@Data
public class TaxDeclarationItemRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "申报单编号", example = "2048")
    private Long declarationId;

    @Schema(description = "申报月 yyyy-MM", example = "2026-08")
    private String periodMonth;

    @Schema(description = "出售者档案编号", example = "4096")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "脱敏身份证号", example = "1101********1234")
    private String idCardMasked;

    @Schema(description = "当月本租户开票张数", example = "3")
    private Integer invoiceCount;

    @Schema(description = "当月本租户净销售额（元）")
    private BigDecimal salesAmount;

    @Schema(description = "按 3% 减按 1% 的销售额（元）")
    private BigDecimal amountAtOnePercent;

    @Schema(description = "放弃减按、按 3% 的销售额（元）")
    private BigDecimal amountAtThreePercent;

    @Schema(description = "征收率未识别的销售额（元）")
    private BigDecimal otherAmount;

    @Schema(description = "该自然人当月在平台各租户的净销售额合计（10 万元免征线口径）")
    private BigDecimal crossTenantMonthAmount;

    @Schema(description = "增值税是否免征（未超 10 万元）")
    private Boolean vatExempt;

    @Schema(description = "当月销售额（跨租户）是否超过 10 万元，需单独列出代办申报")
    private Boolean overExempt;

    @Schema(description = "应缴增值税（元）")
    private BigDecimal vatAmount;

    @Schema(description = "应缴附加税费（元）")
    private BigDecimal surchargeAmount;

    @Schema(description = "应缴个人所得税（元）")
    private BigDecimal iitAmount;

    @Schema(description = "应缴税费合计（元）")
    private BigDecimal totalTaxAmount;

    @Schema(description = "实缴金额（元）")
    private BigDecimal paidAmount;

    @Schema(description = "状态：0-待申报，1-已申报待缴款，2-已缴款")
    private Integer status;

    @Schema(description = "状态名称", example = "待申报")
    private String statusName;

    @Schema(description = "缴款时间")
    private LocalDateTime paidAt;

    @Schema(description = "备注")
    private String remark;

}
