package cn.iocoder.yudao.module.waste.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 对公付款凭证 Response VO")
@Data
public class CompanyPaymentVoucherRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;

    @Schema(description = "凭证号", requiredMode = Schema.RequiredMode.REQUIRED, example = "PAY202312010001")
    private String voucherNumber;

    @Schema(description = "付款类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer paymentType;

    @Schema(description = "付款金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    private BigDecimal paymentAmount;

    @Schema(description = "付款方企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long payerEnterpriseId;

    @Schema(description = "付款方企业名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "ABC环保公司")
    private String payerEnterpriseName;

    @Schema(description = "付款方银行账户", requiredMode = Schema.RequiredMode.REQUIRED, example = "1234567890123456789")
    private String payerBankAccount;

    @Schema(description = "付款方开户银行", requiredMode = Schema.RequiredMode.REQUIRED, example = "中国工商银行北京分行")
    private String payerBankName;

    @Schema(description = "收款方企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3072")
    private Long payeeEnterpriseId;

    @Schema(description = "收款方企业名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "XYZ回收公司")
    private String payeeEnterpriseName;

    @Schema(description = "收款方银行账户", requiredMode = Schema.RequiredMode.REQUIRED, example = "9876543210987654321")
    private String payeeBankAccount;

    @Schema(description = "收款方开户银行", requiredMode = Schema.RequiredMode.REQUIRED, example = "中国建设银行上海分行")
    private String payeeBankName;

    @Schema(description = "交易流水号", example = "TXN202312010001")
    private String transactionNumber;

    @Schema(description = "交易时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-12-01 14:30:00")
    private LocalDateTime transactionTime;

    @Schema(description = "付款用途", requiredMode = Schema.RequiredMode.REQUIRED, example = "危废处理费用")
    private String paymentPurpose;

    @Schema(description = "凭证图片URL", example = "https://example.com/voucher.jpg")
    private String voucherImageUrl;

    @Schema(description = "产废企业确认状态", example = "1")
    private Integer producerConfirmStatus;

    @Schema(description = "产废企业确认时间", example = "2023-12-01 15:00:00")
    private LocalDateTime producerConfirmTime;

    @Schema(description = "产废企业确认人", example = "张三")
    private String producerConfirmBy;

    @Schema(description = "回收企业确认状态", example = "1")
    private Integer recyclerConfirmStatus;

    @Schema(description = "回收企业确认时间", example = "2023-12-01 15:30:00")
    private LocalDateTime recyclerConfirmTime;

    @Schema(description = "回收企业确认人", example = "李四")
    private String recyclerConfirmBy;

    @Schema(description = "备注", example = "付款凭证备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 