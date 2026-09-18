package cn.iocoder.yudao.module.waste.controller.admin.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 批量智能报价生成 Response VO")
@Data
public class SmartQuotationBatchGenerateRespVO {

    @Schema(description = "生成的报价ID列表")
    private List<Long> quotationIds;

    @Schema(description = "成功生成数量", example = "5")
    private Integer successCount;

    @Schema(description = "总企业数量", example = "8")
    private Integer totalCount;

    @Schema(description = "消息", example = "批量智能报价生成完成，成功生成 5 个报价")
    private String message;
} 