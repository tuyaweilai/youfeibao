package cn.iocoder.yudao.module.logistics.controller.admin.driver.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 司机资质信息分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DriverQualificationPageReqVO extends PageParam {

    @Schema(description = "所属企业ID", example = "1024")
    private Long enterpriseId;

    @Schema(description = "司机编号", example = "D001")
    private String driverCode;

    @Schema(description = "驾驶证号码", example = "310101199001010001")
    private String drivingLicenseNo;

    @Schema(description = "司机状态", example = "0")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 