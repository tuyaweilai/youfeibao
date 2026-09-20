package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 盘点单保存 Request VO（#54 T16）。
 *
 * <p>登记时只录**实盘数**；账面数量与差额在过账时由 ERP 在同一个事务里算（见
 * {@code StockApi#adjustTo}），避免登记与过账之间发生出入库后差额对不上。
 */
@Schema(description = "管理后台 - 盘点单保存 Request VO")
@Data
public class StockCheckSaveReqVO {

    @Schema(description = "盘点明细（至少一条）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "盘点至少需要一条「仓库 + 品类 + 实盘数」明细")
    @Valid
    private List<StockCheckItemReqVO> items;

    @Schema(description = "备注（盘点范围 / 事由）", example = "9 月月末全库盘点")
    private String remark;

}
