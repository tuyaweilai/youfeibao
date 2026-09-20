package cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 车辆 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsVehicleRespVO extends LogisticsVehicleSaveReqVO {

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
