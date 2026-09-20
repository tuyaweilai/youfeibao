package cn.iocoder.yudao.module.waste.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 对公付款凭证创建 Request VO")
@Data
public class CompanyPaymentVoucherCreateReqVO {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "凭证号", requiredMode = Schema.RequiredMode.REQUIRED, example = "PAY202312010001")
    @NotBlank(message = "凭证号不能为空")
    @Size(max = 50, message = "凭证号长度不能超过50个字符")
    private String voucherNumber;

    @Schema(description = "付款类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "付款类型不能为空")
    private Integer paymentType;

    @Schema(description = "付款金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "付款金额不能为空")
    @DecimalMin(value = "0.01", message = "付款金额必须大于0")
    private BigDecimal paymentAmount;

    @Schema(description = "付款方企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "付款方企业ID不能为空")
    private Long payerEnterpriseId;

    @Schema(description = "付款方企业名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "ABC环保公司")
    @NotBlank(message = "付款方企业名称不能为空")
    @Size(max = 200, message = "付款方企业名称长度不能超过200个字符")
    private String payerEnterpriseName;

    @Schema(description = "付款方银行账户", requiredMode = Schema.RequiredMode.REQUIRED, example = "1234567890123456789")
    @NotBlank(message = "付款方银行账户不能为空")
    @Size(max = 50, message = "付款方银行账户长度不能超过50个字符")
    private String payerBankAccount;

    @Schema(description = "付款方开户银行", requiredMode = Schema.RequiredMode.REQUIRED, example = "中国工商银行北京分行")
    @NotBlank(message = "付款方开户银行不能为空")
    @Size(max = 200, message = "付款方开户银行长度不能超过200个字符")
    private String payerBankName;

    @Schema(description = "收款方企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3072")
    @NotNull(message = "收款方企业ID不能为空")
    private Long payeeEnterpriseId;

    @Schema(description = "收款方企业名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "XYZ回收公司")
    @NotBlank(message = "收款方企业名称不能为空")
    @Size(max = 200, message = "收款方企业名称长度不能超过200个字符")
    private String payeeEnterpriseName;

    @Schema(description = "收款方银行账户", requiredMode = Schema.RequiredMode.REQUIRED, example = "9876543210987654321")
    @NotBlank(message = "收款方银行账户不能为空")
    @Size(max = 50, message = "收款方银行账户长度不能超过50个字符")
    private String payeeBankAccount;

    @Schema(description = "收款方开户银行", requiredMode = Schema.RequiredMode.REQUIRED, example = "中国建设银行上海分行")
    @NotBlank(message = "收款方开户银行不能为空")
    @Size(max = 200, message = "收款方开户银行长度不能超过200个字符")
    private String payeeBankName;

    @Schema(description = "交易流水号", example = "TXN202312010001")
    @Size(max = 100, message = "交易流水号长度不能超过100个字符")
    private String transactionNumber;

    @Schema(description = "交易时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-12-01 14:30:00")
    @NotNull(message = "交易时间不能为空")
    private LocalDateTime transactionTime;

    @Schema(description = "付款用途", requiredMode = Schema.RequiredMode.REQUIRED, example = "危废处理费用")
    @NotBlank(message = "付款用途不能为空")
    @Size(max = 200, message = "付款用途长度不能超过200个字符")
    private String paymentPurpose;

    @Schema(description = "凭证图片URL", example = "https://example.com/voucher.jpg")
    @Size(max = 500, message = "凭证图片URL长度不能超过500个字符")
    private String voucherImageUrl;

    @Schema(description = "产废企业确认状态", example = "1")
    private Integer producerConfirmStatus;

    @Schema(description = "回收企业确认状态", example = "1")
    private Integer recyclerConfirmStatus;

    @Schema(description = "备注", example = "付款凭证备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

} 