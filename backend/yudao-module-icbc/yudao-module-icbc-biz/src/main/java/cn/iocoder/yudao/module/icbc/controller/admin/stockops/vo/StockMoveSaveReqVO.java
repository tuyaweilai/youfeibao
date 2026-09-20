package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 跨仓调拨单保存 Request VO（#54 T16）。
 */
@Schema(description = "管理后台 - 跨仓调拨单保存 Request VO")
@Data
public class StockMoveSaveReqVO {

    @Schema(description = "调拨明细（至少一条）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "调拨至少需要一条「源 → 目标 + 数量」明细")
    @Valid
    private List<StockMoveItemReqVO> items;

    @Schema(description = "备注（调拨事由）", example = "1 号库腾位，转到 2 号库")
    private String remark;

}
