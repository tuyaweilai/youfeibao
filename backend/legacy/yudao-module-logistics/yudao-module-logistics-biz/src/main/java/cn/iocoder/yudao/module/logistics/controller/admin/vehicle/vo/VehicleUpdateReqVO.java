package cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 车辆信息更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class VehicleUpdateReqVO extends VehicleCreateReqVO {

    @Schema(description = "车辆ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "车辆ID不能为空")
    private Long id;

} 