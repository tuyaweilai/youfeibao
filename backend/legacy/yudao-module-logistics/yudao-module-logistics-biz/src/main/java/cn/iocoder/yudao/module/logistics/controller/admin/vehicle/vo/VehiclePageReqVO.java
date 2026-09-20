package cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 车辆信息分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class VehiclePageReqVO extends PageParam {

    @Schema(description = "所属企业ID", example = "1024")
    private Long enterpriseId;

    @Schema(description = "车牌号", example = "京A12345")
    private String plateNumber;

    @Schema(description = "车辆类型", example = "厢式货车")
    private String vehicleType;

    @Schema(description = "车辆状态", example = "0")
    private Integer status;

    @Schema(description = "GPS设备ID", example = "GPS001")
    private String gpsDeviceId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 