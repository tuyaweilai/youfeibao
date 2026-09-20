package cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工作台一屏（#56 T18，ADR 0021：每个数字都能追到来源与口径）。
 */
@Schema(description = "管理后台 - 工作台概览")
@Data
public class WorkbenchOverviewRespVO {

    @Schema(description = "八类待办")
    private List<WorkbenchTodoRespVO> todos;

    @Schema(description = "预警：额度 / 资质到期 / 开票就绪")
    private List<WorkbenchWarningRespVO> warnings;

    @Schema(description = "开票就绪徽标")
    private WorkbenchReadinessRespVO readiness;

}
