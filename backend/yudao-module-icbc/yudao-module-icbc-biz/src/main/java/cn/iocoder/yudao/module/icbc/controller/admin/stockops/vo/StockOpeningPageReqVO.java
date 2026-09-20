package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 期初记录分页 Request VO（#54 T16）。
 */
@Schema(description = "管理后台 - 期初记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StockOpeningPageReqVO extends PageParam {

    @Schema(description = "导入批次号", example = "OP2026")
    private String openingNo;

    @Schema(description = "品类编号", example = "1")
    private Long goodsConfigId;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "状态：1-已过账，2-已作废", example = "1")
    private Integer status;

}
