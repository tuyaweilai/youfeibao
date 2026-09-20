package cn.iocoder.yudao.module.logistics.controller.admin.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 运输费用（内部成本）新增/修改（V8 #75）：自有车的路桥 / 燃油等，**按实际承担方**记。
 */
@Schema(description = "管理后台 - 运输费用（内部成本）新增/修改 Request VO")
@Data
public class LogisticsTransportCostSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "运输任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "运输任务不能为空")
    private Long taskId;

    @Schema(description = "费用类型：1-路桥费，2-燃油费，3-其他", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "费用类型不能为空")
    private Integer costType;

    @Schema(description = "费用名称", example = "绕行过路费")
    private String name;

    @Schema(description = "金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "80.00")
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @Schema(description = "承担方：1-承运商承担，2-本企业承担", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "承担方不能为空")
    private Integer bearer;

    @Schema(description = "发生日期")
    private LocalDate occurDate;

    @Schema(description = "备注")
    private String remark;

}
