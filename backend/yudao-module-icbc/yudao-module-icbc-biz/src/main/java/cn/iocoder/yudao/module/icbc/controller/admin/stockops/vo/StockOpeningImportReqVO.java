package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 期初导入 Request VO（#54 T16）。
 *
 * <p>一次导入一批（共享一个导入批次号），每行 = 一个「品类 + 仓库 + 库位 + 批次」的期初数量。
 * 导入即过账（经 {@code StockApi} 写 {@code OPENING_IN} 流水）；同一维度已有生效期初时整批拒绝。
 */
@Schema(description = "管理后台 - 期初导入 Request VO")
@Data
public class StockOpeningImportReqVO {

    @Schema(description = "期初明细（至少一条）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "期初导入至少需要一条明细")
    @Valid
    private List<StockOpeningItemReqVO> items;

    @Schema(description = "备注", example = "启用平台前的存量")
    private String remark;

}
