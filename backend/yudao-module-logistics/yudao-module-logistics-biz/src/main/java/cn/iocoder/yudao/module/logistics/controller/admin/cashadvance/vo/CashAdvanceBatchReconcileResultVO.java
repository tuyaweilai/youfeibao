package cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 现金代付批量对账确认结果 Response VO")
@Data
public class CashAdvanceBatchReconcileResultVO {

    @Schema(description = "总记录数", example = "10")
    private Integer totalCount;

    @Schema(description = "成功处理数", example = "8")
    private Integer successCount;

    @Schema(description = "失败处理数", example = "2")
    private Integer failureCount;

    @Schema(description = "成功记录列表")
    private List<ReconcileResult> successList;

    @Schema(description = "失败记录列表")
    private List<ReconcileResult> failureList;

    @Schema(description = "对账结果项")
    @Data
    public static class ReconcileResult {
        
        @Schema(description = "代付记录ID", example = "1024")
        private Long id;

        @Schema(description = "对账状态", example = "true")
        private Boolean success;

        @Schema(description = "失败原因", example = "记录不存在")
        private String failureReason;
    }
} 