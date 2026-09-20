package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - 库存表（在库量）分页 Request VO（#57 T19）。
 */
@Schema(description = "管理后台 - 库存表（在库量）分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ReportStockBalancePageReqVO extends PageParam {

    @Schema(description = "品类编号")
    private Long goodsConfigId;

    @Schema(description = "仓库编号")
    private Long warehouseId;

    @Schema(description = "库位编号")
    private Long locationId;

    @Schema(description = "批次编号")
    private Long batchId;

}
