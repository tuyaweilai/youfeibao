package cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工作台预警项（#56 T18）：额度、资质到期、开票就绪。
 *
 * <p>预警与待办的区别是「有没有一件事等你做」：待办是具体单据上的动作，预警是「再不管就会出事」。
 */
@Schema(description = "管理后台 - 工作台预警项")
@Data
public class WorkbenchWarningRespVO {

    @Schema(description = "预警编码", example = "QUALIFICATION_EXPIRY")
    private String code;

    @Schema(description = "预警名称", example = "资质到期")
    private String name;

    @Schema(description = "级别：OK / WARN / DANGER", example = "WARN")
    private String level;

    @Schema(description = "命中数量", example = "2")
    private long count;

    @Schema(description = "一句话说明（口径 + 该怎么处理）")
    private String message;

    @Schema(description = "来源明细（最多 10 条）")
    private List<WorkbenchItemRespVO> items;

}
