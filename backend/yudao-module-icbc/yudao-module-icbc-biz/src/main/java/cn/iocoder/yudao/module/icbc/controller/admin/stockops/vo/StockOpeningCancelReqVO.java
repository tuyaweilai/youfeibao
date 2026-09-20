package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 作废期初 Request VO（#54 T16）。
 *
 * <p>已生效的期初作废会经 {@code StockApi} 冲销入库的库存（业务类型 {@code OPENING_IN_CANCEL}）。
 */
@Schema(description = "管理后台 - 作废期初 Request VO")
@Data
public class StockOpeningCancelReqVO {

    @Schema(description = "期初记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "期初记录编号不能为空")
    private Long id;

    @Schema(description = "作废原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "数量录错，重新导入")
    @NotBlank(message = "作废期初必须说明原因")
    private String reason;

}
