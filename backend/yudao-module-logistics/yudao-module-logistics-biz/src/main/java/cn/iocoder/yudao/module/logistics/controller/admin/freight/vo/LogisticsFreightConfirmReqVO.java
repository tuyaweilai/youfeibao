package cn.iocoder.yudao.module.logistics.controller.admin.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 确认承运商应付 Request VO")
@Data
public class LogisticsFreightConfirmReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "实际应付（确认的对账金额）", requiredMode = Schema.RequiredMode.REQUIRED, example = "320.00")
    @NotNull(message = "实际应付不能为空")
    private BigDecimal actualAmount;

    @Schema(description = "差异原因（实际与应有有差异时必填）", example = "临时加了一趟短驳")
    private String varianceReason;

    @Schema(description = "确认备注")
    private String confirmRemark;

}
