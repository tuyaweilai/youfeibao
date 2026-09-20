package cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    // 日期在协议上统一用 ISO 字符串（yyyy-MM-dd）：本项目第一次把 LocalDate 放到接口上，
    // 不写 @JsonFormat 的话 Jackson 会序列化成 [2026,9,17] 这种数组，前端的日期列与比较全得跟着变形。
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "行驶证到期日（过期不得派出；可由管理员带原因授权放行）", example = "2030-12-31")
    private LocalDate drivingLicenseExpiryDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "保险到期日（同上）", example = "2030-12-31")
    private LocalDate insuranceExpiryDate;

    @Schema(description = "车辆照片 URL 列表")
    private List<String> photos;

    @Schema(description = "车载定位设备号（一期只登记）", example = "GPS-001")
    private String gpsDeviceId;

    @Schema(description = "车辆状态：0-可用，1-运输中，2-维护中", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "车辆状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
