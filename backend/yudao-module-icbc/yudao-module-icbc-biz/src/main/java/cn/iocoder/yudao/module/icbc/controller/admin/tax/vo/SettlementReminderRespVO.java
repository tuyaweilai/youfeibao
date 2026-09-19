package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出售者汇算清缴提醒：次年 3 月 31 日前须自行汇算清缴。
 */
@Schema(description = "管理后台 - 汇算清缴提醒 Response VO")
@Data
public class SettlementReminderRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "出售者档案编号", example = "2048")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "脱敏身份证号", example = "1101********1234")
    private String idCardMasked;

    @Schema(description = "纳税年度", example = "2026")
    private Integer taxYear;

    @Schema(description = "汇算清缴截止日", example = "2027-03-31")
    private LocalDate deadline;

    @Schema(description = "距截止日天数（负数表示已逾期）")
    private Integer daysLeft;

    @Schema(description = "是否已过截止日")
    private Boolean overdue;

    @Schema(description = "当年开票张数")
    private Integer invoiceCount;

    @Schema(description = "当年开票金额（元）")
    private BigDecimal invoicedAmount;

    @Schema(description = "当年已预缴税费合计（元）")
    private BigDecimal paidTaxAmount;

    @Schema(description = "当年已预缴个人所得税（元）")
    private BigDecimal iitAmount;

    @Schema(description = "状态：0-待提醒，1-已提醒")
    private Integer status;

    @Schema(description = "状态名称", example = "待提醒")
    private String statusName;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "提醒时间")
    private LocalDateTime remindedAt;

    @Schema(description = "处理说明")
    private String handleRemark;

    @Schema(description = "备注")
    private String remark;

}
