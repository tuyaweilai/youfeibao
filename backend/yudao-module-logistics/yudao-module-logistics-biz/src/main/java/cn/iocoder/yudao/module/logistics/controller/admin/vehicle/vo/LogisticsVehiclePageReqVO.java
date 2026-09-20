package cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 车辆分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsVehiclePageReqVO extends PageParam {

    @Schema(description = "车牌号（模糊）", example = "浙A")
    private String plateNo;

    @Schema(description = "车辆类型", example = "厢式货车")
    private String vehicleType;

    @Schema(description = "车辆状态：0-可用，1-运输中，2-维护中", example = "0")
    private Integer status;

}
