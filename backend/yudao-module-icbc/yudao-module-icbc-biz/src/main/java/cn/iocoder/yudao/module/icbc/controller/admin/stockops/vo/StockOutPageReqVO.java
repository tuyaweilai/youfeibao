package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 非销售出库单分页 Request VO（#54 T16）。
 */
@Schema(description = "管理后台 - 非销售出库单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StockOutPageReqVO extends PageParam {

    @Schema(description = "出库单号", example = "SO2026")
    private String stockOutNo;

    @Schema(description = "出库类型：10-报损，20-退货出库，30-内部领用", example = "10")
    private Integer outType;

    @Schema(description = "状态：0-待过账，1-已过账，2-已作废", example = "1")
    private Integer status;

}
