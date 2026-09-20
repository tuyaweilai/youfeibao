package cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 入库单保存 Request VO（#52 T14）。
 *
 * <p>库管为一张收购单选定仓库 / 库位 / 批次与实际入库量。明细可多条（拆多个库位），
 * 累计不得超过可入库实物量。
 */
@Schema(description = "管理后台 - 入库单保存 Request VO")
@Data
public class StockInSaveReqVO {

    @Schema(description = "收购单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "收购单编号不能为空")
    private Long acquisitionId;

    @Schema(description = "入库明细（至少一条；可拆多个库位 / 批次）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "确认入库至少需要一条「仓库 + 库位 / 批次 + 数量」明细")
    @Valid
    private List<StockInItemReqVO> items;

    @Schema(description = "备注", example = "先入 1 号库，剩下的下雨后再入")
    private String remark;

}
