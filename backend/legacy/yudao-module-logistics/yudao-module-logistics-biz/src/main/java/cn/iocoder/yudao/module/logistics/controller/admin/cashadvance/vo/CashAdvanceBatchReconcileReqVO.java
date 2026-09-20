package cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - 现金代付批量对账确认 Request VO")
@Data
public class CashAdvanceBatchReconcileReqVO {

    @Schema(description = "对账记录列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "对账记录列表不能为空")
    @Valid
    private List<CashAdvanceReconcileReqVO> reconcileList;

    @Schema(description = "批量操作备注", example = "批量对账处理")
    private String batchRemark;
} 