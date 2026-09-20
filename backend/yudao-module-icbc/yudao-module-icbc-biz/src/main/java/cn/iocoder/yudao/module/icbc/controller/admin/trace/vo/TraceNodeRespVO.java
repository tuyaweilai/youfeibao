package cn.iocoder.yudao.module.icbc.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 - 关联单据查询节点（一张原单）（#55 T17）。
 *
 * <p>点击编号可查看原单（{@link #detailPath}，前端据此跳到既有页面）、附件、操作历史与上下游。
 * 一对多的下级明细放在 {@link #children}（磅次 / 库位明细 / 版本 / 成交记录 / 发票明细等）。
 */
@Schema(description = "管理后台 - 关联单据查询节点")
@Data
public class TraceNodeRespVO {

    @Schema(description = "单据类型：HANDOVER_BATCH / ACQUISITION / PURCHASE_ORDER / PURCHASE_ORDER_ITEM / STOCK_IN / STOCK_IN_ITEM / SETTLEMENT / PAYMENT / INVOICE / RED_INVOICE", example = "STOCK_IN")
    private String bizType;

    @Schema(description = "单据主键")
    private Long bizId;

    @Schema(description = "单据编号（点击查看原单）", example = "SIN17645472000001234")
    private String bizNo;

    @Schema(description = "标题（如「入库单」/「采购订单」）", example = "入库单")
    private String title;

    @Schema(description = "状态码（各单据自己的枚举）")
    private Integer status;

    @Schema(description = "状态名", example = "已过账")
    private String statusName;

    @Schema(description = "数量")
    private BigDecimal quantity;

    @Schema(description = "重量")
    private BigDecimal weight;

    @Schema(description = "金额（元）")
    private BigDecimal amount;

    @Schema(description = "计量单位", example = "吨")
    private String unit;

    @Schema(description = "业务发生时间")
    private LocalDateTime time;

    @Schema(description = "前端路由（原单页面），如 /warehouse/stock-in?id=1")
    private String detailPath;

    @Schema(description = "下级明细（一对多时全部展开）")
    private List<TraceNodeRespVO> children;

}
