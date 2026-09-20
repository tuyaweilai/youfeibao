package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 库存表（在库量）Response VO（#57 T19）。
 *
 * <p>一行 = 一个「品类 + 仓库 + 库位 + 批次」的在库数量。**只给数量口径，不展示成本**
 * （ADR 0027：本期库存不做成本核算）。库龄由批次入库时间起算；批次未指定时为空。
 */
@Schema(description = "管理后台 - 库存表（在库量）Response VO")
@Data
public class ReportStockBalanceRespVO {

    @Schema(description = "品类编号")
    private Long goodsConfigId;

    @Schema(description = "品类名称")
    private String categoryName;

    @Schema(description = "仓库编号")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "库位编号")
    private Long locationId;

    @Schema(description = "库位名称")
    private String locationName;

    @Schema(description = "批次编号")
    private Long batchId;

    @Schema(description = "批次编号文本")
    private String batchNo;

    @Schema(description = "批次入库时间（库龄基准）")
    private LocalDateTime batchInTime;

    @Schema(description = "库龄（天，按批次入库时间起算；无批次时为空）")
    private Long ageDays;

    @Schema(description = "在库数量")
    private BigDecimal count;

}
