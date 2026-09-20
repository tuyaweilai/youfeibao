package cn.iocoder.yudao.module.waste.controller.admin.appointment.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 危废转移预约分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppointmentPageReqVO extends PageParam {

    @Schema(description = "预约单号", example = "AP202412010001")
    private String appointmentNo;

    @Schema(description = "产废企业ID", example = "1024")
    private Long producerEnterpriseId;

    @Schema(description = "产废企业名称", example = "芋道科技")
    private String producerEnterpriseName;

    @Schema(description = "回收企业ID", example = "2048")
    private Long recyclerEnterpriseId;

    @Schema(description = "回收企业名称", example = "回收公司")
    private String recyclerEnterpriseName;

    @Schema(description = "危险废物代码", example = "HW01")
    private String wasteCode;

    @Schema(description = "危险废物名称", example = "医疗废物")
    private String wasteName;

    @Schema(description = "废物类别", example = "医疗")
    private String wasteCategory;

    @Schema(description = "预约状态", example = "0")
    private Integer appointmentStatus;

    @Schema(description = "分配方式", example = "0")
    private Integer assignmentType;

    @Schema(description = "业务模式", example = "0")
    private Integer businessMode;

    @Schema(description = "是否紧急", example = "false")
    private Boolean isUrgent;

    @Schema(description = "期望取货时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] expectedPickupTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 