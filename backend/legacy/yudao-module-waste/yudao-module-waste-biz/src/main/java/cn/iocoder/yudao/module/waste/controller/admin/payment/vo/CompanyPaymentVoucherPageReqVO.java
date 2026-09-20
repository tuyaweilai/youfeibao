package cn.iocoder.yudao.module.waste.controller.admin.payment.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 对公付款凭证分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CompanyPaymentVoucherPageReqVO extends PageParam {

    @Schema(description = "订单ID", example = "1024")
    private Long orderId;

    @Schema(description = "凭证号", example = "PAY202312010001")
    private String voucherNumber;

    @Schema(description = "付款类型", example = "1")
    private Integer paymentType;

    @Schema(description = "付款方企业ID", example = "2048")
    private Long payerEnterpriseId;

    @Schema(description = "付款方企业名称", example = "ABC环保公司")
    private String payerEnterpriseName;

    @Schema(description = "收款方企业ID", example = "3072")
    private Long payeeEnterpriseId;

    @Schema(description = "收款方企业名称", example = "XYZ回收公司")
    private String payeeEnterpriseName;

    @Schema(description = "交易流水号", example = "TXN202312010001")
    private String transactionNumber;

    @Schema(description = "产废企业确认状态", example = "1")
    private Integer producerConfirmStatus;

    @Schema(description = "回收企业确认状态", example = "1")
    private Integer recyclerConfirmStatus;

    @Schema(description = "交易时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] transactionTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 