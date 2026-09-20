package cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 车辆新增/修改 Request VO")
@Data
public class LogisticsVehicleSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "车牌号（租户内唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "浙A12345")
    @NotEmpty(message = "车牌号不能为空")
    private String plateNo;

    @Schema(description = "车辆类型", example = "厢式货车")
    private String vehicleType;

    @Schema(description = "载重能力（吨）", example = "10.5")
    private BigDecimal capacityTon;

    @Schema(description = "车辆状态：0-可用，1-运输中，2-维护中", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "车辆状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
