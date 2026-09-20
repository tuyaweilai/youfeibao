package cn.iocoder.yudao.module.logistics.controller.admin.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 登记外部付款凭证（V8 #75）：**不接对公付款通道**（ADR 0006），
 * 只把线下付款的凭证号 / 附件 / 金额 / 时间登记进来。
 */
@Schema(description = "管理后台 - 登记承运商运费外部付款凭证 Request VO")
@Data
public class LogisticsFreightPayReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "外部付款凭证号（与附件至少填一个）", example = "PAY20260920001")
    private String paymentVoucherNo;

    @Schema(description = "外部付款凭证附件 URL", example = "https://file/pay-voucher.jpg")
    private String paymentVoucherUrl;

    @Schema(description = "实付金额", example = "320.00")
    private BigDecimal paymentAmount;

    @Schema(description = "付款时间")
    private LocalDateTime paidAt;

    @Schema(description = "付款备注")
    private String paymentRemark;

}
