package cn.iocoder.yudao.module.logistics.controller.admin.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 承运商运费修改 Request VO（仅待确认应付可改）")
@Data
public class LogisticsFreightUpdateReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "计费量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "计费量不能为空")
    private BigDecimal billQuantity;

    @Schema(description = "实际应付（对账金额；留空表示还没对出来）", example = "320.00")
    private BigDecimal actualAmount;

    @Schema(description = "差异原因（实际与应有有差异时必填）", example = "临时加了一趟短驳")
    private String varianceReason;

    @Schema(description = "备注")
    private String remark;

}
