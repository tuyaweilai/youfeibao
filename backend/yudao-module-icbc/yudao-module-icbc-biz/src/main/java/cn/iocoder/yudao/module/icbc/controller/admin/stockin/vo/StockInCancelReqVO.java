package cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 作废入库单 Request VO（#52 T14）。
 *
 * <p>已过账的入库单作废时先按相反方向冲销库存（业务类型 {@code RECEIPT_IN_CANCEL}），
 * 所以必须说明原因：货已经进过库，冲销是敏感动作。
 */
@Schema(description = "管理后台 - 作废入库单 Request VO")
@Data
public class StockInCancelReqVO {

    @Schema(description = "入库单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "入库单编号不能为空")
    private Long id;

    @Schema(description = "作废原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "堆位选错，重新入库")
    @NotBlank(message = "作废入库单必须说明原因")
    private String reason;

}
