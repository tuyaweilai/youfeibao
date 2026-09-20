package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 期初记录 Response VO（#54 T16）。
 */
@Schema(description = "管理后台 - 期初记录 Response VO")
@Data
public class StockOpeningRespVO {

    @Schema(description = "期初记录编号", example = "2048")
    private Long id;

    @Schema(description = "导入批次号", example = "OP202609201200001234")
    private String openingNo;

    @Schema(description = "品类编号", example = "1")
    private Long goodsConfigId;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "库位编号（0 = 未指定）", example = "1")
    private Long locationId;

    @Schema(description = "批次编号（0 = 未指定）", example = "1")
    private Long batchId;

    @Schema(description = "期初数量", example = "5000")
    private BigDecimal quantity;

    @Schema(description = "状态：1-已过账，2-已作废", example = "1")
    private Integer status;

    @Schema(description = "状态名", example = "已过账")
    private String statusName;

    @Schema(description = "过账时间")
    private LocalDateTime postedTime;

    @Schema(description = "作废原因")
    private String cancelReason;

    @Schema(description = "作废时间")
    private LocalDateTime cancelledTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
