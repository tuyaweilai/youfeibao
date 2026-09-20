package cn.iocoder.yudao.module.waste.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 产废企业付款配置 Response VO")
@Data
public class ProducerPaymentConfigRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long enterpriseId;

    @Schema(description = "门店ID", example = "2048")
    private Long storeId;

    @Schema(description = "付款方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
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
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 