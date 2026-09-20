package cn.iocoder.yudao.module.icbc.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 关联单据查询的一个阶段（一栏）（#55 T17）。
 *
 * <p>数量 / 重量 / 金额三者按阶段各自的口径取值：采购订单是计划量、现场收货是实物量与结算重量、
 * 仓储入库是已过账量、结算确认是计价结果、付款与发票是资金与票面金额。取不到的字段留空，
 * 绝不用 0 冒充「没有」。
 */
@Schema(description = "管理后台 - 关联单据查询阶段")
@Data
public class TraceStageRespVO {

    @Schema(description = "阶段编码，枚举 TraceStageCodeEnum", example = "STOCK_IN")
    private String code;

    @Schema(description = "阶段名称", example = "仓储入库")
    private String name;

    @Schema(description = "阶段口径说明")
    private String definition;

    @Schema(description = "状态，枚举 TraceStageStatusEnum：0-未开始 1-处理中 2-已完成 3-异常 4-无需该环节")
    private Integer status;

    @Schema(description = "状态名", example = "已完成")
    private String statusName;

    @Schema(description = "是否无需该环节（如「直接收购」没有采购订单）", example = "false")
    private Boolean notApplicable;

    @Schema(description = "该环节存在但关联缺失（如已结算却查不到入库记录）", example = "false")
    private Boolean missingLink;

    @Schema(description = "数量（按该阶段口径）", example = "14.7400")
    private BigDecimal quantity;

    @Schema(description = "重量（按该阶段口径）", example = "14.7400")
    private BigDecimal weight;

    @Schema(description = "金额（元，按该阶段口径）", example = "12345.67")
    private BigDecimal amount;

    @Schema(description = "计量单位", example = "吨")
    private String unit;

    @Schema(description = "本阶段的附加说明（为什么是这个状态 / 缺什么）")
    private String note;

    @Schema(description = "本阶段的单据明细（一对多时全部展开）")
    private List<TraceNodeRespVO> nodes;

}
