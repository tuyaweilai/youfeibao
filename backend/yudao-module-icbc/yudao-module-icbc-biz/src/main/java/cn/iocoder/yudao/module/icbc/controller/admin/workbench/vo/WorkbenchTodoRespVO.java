package cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工作台待办项（#56 T18，T18 的八类待办之一）。
 *
 * <p>{@code code} 与 {@code cn.iocoder.yudao.module.icbc.enums.WorkbenchTodoCodeEnum} 一一对应，
 * 前端按它决定下钻到哪个模块；{@code definition} 是这个数字的口径，界面上与数字一起显示。
 */
@Schema(description = "管理后台 - 工作台待办项")
@Data
public class WorkbenchTodoRespVO {

    @Schema(description = "待办编码", example = "PENDING_SETTLE_CONFIRM")
    private String code;

    @Schema(description = "待办名称", example = "待结算确认")
    private String name;

    @Schema(description = "待处理条数", example = "3")
    private long total;

    @Schema(description = "是否已可计数；false 表示数据源尚未上线，看 unavailableReason", example = "true")
    private boolean available;

    @Schema(description = "不可计数的原因；可计数时为空")
    private String unavailableReason;

    @Schema(description = "口径：这个数字是怎么算出来的")
    private String definition;

    @Schema(description = "来源明细（最多 10 条，超出看模块列表）")
    private List<WorkbenchItemRespVO> items;

}
