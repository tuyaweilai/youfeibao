package cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 免登录触达通知（#36，ADR 0023）。
 *
 * <p>没有账号的自然人凭短信 / 收货员转达的一次性令牌打开链接，先看到**这一条通知说了什么**，
 * 再决定要不要用手机号验证进屋。**不含身份证等敏感信息**；金额一律「本平台累计」口径，
 * 不出现「已到账」（ADR 0021）。
 */
@Schema(description = "公开端点 - 触达通知 Response VO")
@Data
public class PublicNoticeRespVO {

    @Schema(description = "令牌用途")
    private String purpose;

    @Schema(description = "场站所属租户编号（用于登录时定位回收企业）")
    private Long tenantId;

    @Schema(description = "收方（出售者）档案编号")
    private Long payeeId;

    @Schema(description = "出售者姓名")
    private String sellerName;

    @Schema(description = "发给本人的提示语（按 ADR 0021 口径）")
    private String message;

    @Schema(description = "通知条目：待确认结算 / 付款异常 / 已开出票")
    private List<NoticeItem> items;

    @Schema(description = "口径说明")
    private String scopeNote;

    @Schema(description = "公开端点 - 触达通知条目")
    @Data
    public static class NoticeItem {

        @Schema(description = "类型：SETTLEMENT_PENDING / PAYMENT_EXCEPTION / INVOICE_ISSUED")
        private String type;

        @Schema(description = "类型名")
        private String typeName;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "状态说明（可核验的说法）")
        private String statusName;

        @Schema(description = "结算单编号（类型=结算单待确认时为待确认的结算单）")
        private Long settlementId;

        @Schema(description = "合作方订单号（付款异常 / 发票开出）")
        private String partnerOrderId;

        @Schema(description = "金额（元，本平台累计口径）")
        private BigDecimal amount;

        @Schema(description = "收购单张数（结算待确认）")
        private Integer acquisitionCount;

        @Schema(description = "发票号（发票已开出）")
        private String invoiceNo;

        @Schema(description = "确认截止时间（待确认结算）")
        private LocalDateTime deadlineTime;

        @Schema(description = "下一步提示")
        private String nextStep;

    }

}
