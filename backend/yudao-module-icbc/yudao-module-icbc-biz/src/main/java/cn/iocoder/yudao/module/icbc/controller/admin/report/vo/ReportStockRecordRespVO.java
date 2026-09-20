package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 库存表（入出流水）Response VO（#57 T19）。
 *
 * <p>一行 = 一条库存变更流水：正数入库、负数出库。只给数量口径，不展示成本。
 */
@Schema(description = "管理后台 - 库存表（入出流水）Response VO")
@Data
public class ReportStockRecordRespVO {

    @Schema(description = "流水编号")
    private Long id;

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

    @Schema(description = "出入库数量（正入负出）")
    private BigDecimal count;

    @Schema(description = "变动后的库存量")
    private BigDecimal totalCount;

    @Schema(description = "业务类型名")
    private String bizTypeName;

    @Schema(description = "业务编号")
    private Long bizId;

    @Schema(description = "业务单号")
    private String bizNo;

    @Schema(description = "流水时间")
    private LocalDateTime createTime;

}
