package cn.iocoder.yudao.module.waste.controller.admin.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 批量基于报价生成电子合同 Response VO")
@Data
public class QuotationContractBatchGenerateRespVO {

    @Schema(description = "成功生成的合同列表")
    private List<QuotationContractGenerateRespVO> successContracts;

    @Schema(description = "失败消息列表")
    private List<String> failureMessages;

    @Schema(description = "生成的合同ID列表")
    private List<Long> contractIds;

    @Schema(description = "成功生成数量", example = "5")
    private Integer successCount;

    @Schema(description = "失败数量", example = "3")
    private Integer failureCount;

    @Schema(description = "总报价数量", example = "8")
    private Integer totalCount;

    @Schema(description = "消息", example = "批量生成合同完成，成功生成 5 个合同")
    private String message;
} 