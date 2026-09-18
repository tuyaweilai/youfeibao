package cn.iocoder.yudao.module.waste.controller.admin.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 智能报价生成 Response VO")
@Data
public class SmartQuotationGenerateRespVO {

    @Schema(description = "报价金额", example = "5000.00")
    private BigDecimal quotedPrice;

    @Schema(description = "是否成功", example = "true")
    private Boolean success;

    @Schema(description = "消息", example = "智能报价生成成功")
    private String message;
} 