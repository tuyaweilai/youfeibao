package cn.iocoder.yudao.module.waste.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 产废企业付款配置创建 Request VO")
@Data
public class ProducerPaymentConfigCreateReqVO {

    @Schema(description = "企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;

    @Schema(description = "门店ID", example = "2048")
    private Long storeId;

    @Schema(description = "付款方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "付款方式不能为空")
    private Integer paymentMethod;

    @Schema(description = "对公结算配置", example = "{\"bankAccount\":\"123456789\"}")
    private String corporateSettlementConfig;

    @Schema(description = "个人结算配置", example = "{\"alipayAccount\":\"user@example.com\"}")
    private String personalSettlementConfig;

    @Schema(description = "通用配置", example = "{\"autoPayment\":true}")
    private String generalConfig;

    @Schema(description = "是否默认配置", example = "false")
    private Boolean isDefault;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "付款配置备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

} 