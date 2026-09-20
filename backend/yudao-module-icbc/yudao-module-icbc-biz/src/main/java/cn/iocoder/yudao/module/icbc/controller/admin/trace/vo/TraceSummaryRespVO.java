package cn.iocoder.yudao.module.icbc.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 关联单据查询的汇总（#55 T17，AC5）。
 *
 * <p>口径只有一处：**每张收购单各计一次**。汇总不把同一结算单 / 同一采购订单再加一遍
 * （否则一次收货拆成两张收购单时会重复计数），也不从库存数量反推金额。
 */
@Schema(description = "管理后台 - 关联单据查询汇总")
@Data
public class TraceSummaryRespVO {

    @Schema(description = "命中的收购单张数（每张只计一次）", example = "12")
    private Long acquisitionCount;

    @Schema(description = "结算重量合计（吨 / 品类单位；计价基准）", example = "28.3300")
    private BigDecimal totalSettlementWeight;

    @Schema(description = "已过账入库量合计", example = "14.7400")
    private BigDecimal totalStockedWeight;

    @Schema(description = "收购金额合计（元）", example = "12345.67")
    private BigDecimal totalAmount;

    @Schema(description = "命中集合中有差异 / 缺失关联的收购单张数", example = "3")
    private Long differenceCount;

    @Schema(description = "计数口径说明")
    private String countNote;

}
