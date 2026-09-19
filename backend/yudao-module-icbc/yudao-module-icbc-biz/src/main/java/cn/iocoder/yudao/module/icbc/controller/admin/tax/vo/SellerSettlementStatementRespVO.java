package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出售者的汇算清缴对账单：当年开了多少票、已预缴多少税。
 *
 * <p>这是回收企业「向出售者提供开票与已缴税款信息」义务的落地，出售者可凭免登录
 * 链接自己取得，不必登门。
 */
@Schema(description = "管理后台 - 出售者汇算清缴对账单 Response VO")
@Data
public class SellerSettlementStatementRespVO {

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

    @Schema(description = "按月明细")
    private List<MonthStatement> months;

    @Schema(description = "提醒语")
    private String message;

    @Schema(description = "生成时间")
    private LocalDateTime generatedAt;

    /**
     * 对账单里的一个月。
     */
    @Schema(description = "管理后台 - 出售者汇算清缴对账单月度明细")
    @Data
    public static class MonthStatement {

        @Schema(description = "月份 yyyy-MM", example = "2026-08")
        private String month;

        @Schema(description = "开票张数")
        private Integer invoiceCount;

        @Schema(description = "开票金额（元）")
        private BigDecimal invoicedAmount;

        @Schema(description = "已预缴税费（元）")
        private BigDecimal paidTaxAmount;

        @Schema(description = "已预缴个人所得税（元）")
        private BigDecimal iitAmount;

    }

}
