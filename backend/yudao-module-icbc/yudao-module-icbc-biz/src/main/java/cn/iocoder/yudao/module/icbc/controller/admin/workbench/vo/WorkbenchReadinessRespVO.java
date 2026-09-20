package cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工作台的开票就绪徽标（#56 T18）。
 *
 * <p>{@code ready=false} 时任一层资质失效都会冻结开票，所以它不只是「提示」——徽标用危险色显示，
 * 点开是 {@code items} 里逐项的补齐方式。
 */
@Schema(description = "管理后台 - 工作台开票就绪徽标")
@Data
public class WorkbenchReadinessRespVO {

    @Schema(description = "是否开票就绪（全部检查项通过）", example = "false")
    private boolean ready;

    @Schema(description = "检查项")
    private List<WorkbenchReadinessItemRespVO> items;

}
