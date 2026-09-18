package cn.iocoder.yudao.module.waste.controller.admin.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 基于报价生成电子合同 Response VO")
@Data
public class QuotationContractGenerateRespVO {

    @Schema(description = "生成的合同ID", example = "1")
    private Long contractId;

    @Schema(description = "报价ID", example = "1")
    private Long quotationId;

    @Schema(description = "合同内容", example = "危废处置服务合同...")
    private String contractContent;

    @Schema(description = "合同名称", example = "危废处置服务合同_HW01_123_20231201")
    private String contractName;

    @Schema(description = "合同总金额", example = "10000.00")
    private BigDecimal totalAmount;

    @Schema(description = "生成时间", example = "2023-12-01 10:00:00")
    private LocalDateTime generateTime;

    @Schema(description = "状态", example = "GENERATED")
    private String status;

    @Schema(description = "是否成功", example = "true")
    private Boolean success;

    @Schema(description = "消息", example = "基于报价生成电子合同成功")
    private String message;
} 