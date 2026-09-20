package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 作废非销售出库单 Request VO（#54 T16）。
 *
 * <p>已过账的作废会按相反方向冲销库存，所以必须说明原因。
 */
@Schema(description = "管理后台 - 作废非销售出库单 Request VO")
@Data
public class StockOutCancelReqVO {

    @Schema(description = "出库单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "出库单编号不能为空")
    private Long id;

    @Schema(description = "作废原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "数量录错")
    @NotBlank(message = "作废出库单必须说明原因")
    private String reason;

}
