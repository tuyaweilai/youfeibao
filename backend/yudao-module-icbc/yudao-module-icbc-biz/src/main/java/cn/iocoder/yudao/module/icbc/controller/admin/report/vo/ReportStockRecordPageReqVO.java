package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - 库存表（入出流水）分页 Request VO（#57 T19）。
 */
@Schema(description = "管理后台 - 库存表（入出流水）分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ReportStockRecordPageReqVO extends PageParam {

    @Schema(description = "品类编号")
    private Long goodsConfigId;

    @Schema(description = "仓库编号")
    private Long warehouseId;

    @Schema(description = "库位编号")
    private Long locationId;

    @Schema(description = "批次编号")
    private Long batchId;

    @Schema(description = "业务类型（见 ERP 库存业务类型枚举）")
    private Integer bizType;

    @Schema(description = "业务单号")
    private String bizNo;

}
