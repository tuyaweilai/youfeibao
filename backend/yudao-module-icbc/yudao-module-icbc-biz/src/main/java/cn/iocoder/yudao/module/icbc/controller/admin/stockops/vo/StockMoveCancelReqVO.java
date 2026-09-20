package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 作废跨仓调拨单 Request VO（#54 T16）。
 */
@Schema(description = "管理后台 - 作废跨仓调拨单 Request VO")
@Data
public class StockMoveCancelReqVO {

    @Schema(description = "调拨单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "调拨单编号不能为空")
    private Long id;

    @Schema(description = "作废原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "目标仓库选错")
    @NotBlank(message = "作废调拨单必须说明原因")
    private String reason;

}
