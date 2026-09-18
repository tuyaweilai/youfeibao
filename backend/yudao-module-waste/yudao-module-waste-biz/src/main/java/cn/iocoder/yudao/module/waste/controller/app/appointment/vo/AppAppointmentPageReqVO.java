package cn.iocoder.yudao.module.waste.controller.app.appointment.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "用户 APP - 危废转移预约分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppAppointmentPageReqVO extends PageParam {

    @Schema(description = "预约单号", example = "AP20231201001234")
    private String appointmentNo;

    @Schema(description = "废物代码", example = "HW01")
    private String wasteCode;

    @Schema(description = "废物名称", example = "医疗废物")
    private String wasteName;

    @Schema(description = "预约状态", example = "0")
    private Integer appointmentStatus;

    @Schema(description = "是否紧急", example = "false")
    private Boolean isUrgent;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "期望取货时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] expectedPickupTime;

} 