package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 非销售出库单保存 Request VO（#54 T16）。
 *
 * <p>报损 / 退货出库 / 内部领用三种，**不挂客户**：一期不做销售出库。
 */
@Schema(description = "管理后台 - 非销售出库单保存 Request VO")
@Data
public class StockOutSaveReqVO {

    @Schema(description = "出库类型：10-报损，20-退货出库，30-内部领用",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "出库类型不能为空")
    private Integer outType;

    @Schema(description = "出库明细（至少一条）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "出库至少需要一条「仓库 + 品类 + 数量」明细")
    @Valid
    private List<StockOutItemReqVO> items;

    @Schema(description = "备注（出库事由）", example = "雨天受潮，废纸报损")
    private String remark;

}
