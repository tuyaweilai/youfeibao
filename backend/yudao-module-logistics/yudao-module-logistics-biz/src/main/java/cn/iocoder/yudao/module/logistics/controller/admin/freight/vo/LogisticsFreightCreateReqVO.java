package cn.iocoder.yudao.module.logistics.controller.admin.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 承运商运费汇集 Request VO")
@Data
public class LogisticsFreightCreateReqVO {

    @Schema(description = "运输任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "运输任务不能为空")
    private Long taskId;

    @Schema(description = "承运合同编号（运价来自合同）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "承运合同不能为空")
    private Long contractId;

    @Schema(description = "计费量（按车填趟数、按吨填吨数、按公里填公里数）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "计费量不能为空")
    private BigDecimal billQuantity;

    @Schema(description = "备注")
    private String remark;

}
