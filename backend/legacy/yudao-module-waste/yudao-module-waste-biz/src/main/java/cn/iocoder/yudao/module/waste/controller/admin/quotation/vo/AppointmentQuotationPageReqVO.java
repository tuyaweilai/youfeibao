package cn.iocoder.yudao.module.waste.controller.admin.quotation.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 预约报价记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppointmentQuotationPageReqVO extends PageParam {

    @Schema(description = "预约单ID", example = "1024")
    private Long appointmentId;

    @Schema(description = "回收企业ID", example = "2048")
    private Long recyclingEnterpriseId;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "接受人", example = "张三")
    private String acceptedBy;

    @Schema(description = "拒绝人", example = "李四")
    private String rejectedBy;

    @Schema(description = "撤回人", example = "王五")
    private String withdrawnBy;

    @Schema(description = "报价时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] quotationTime;

    @Schema(description = "有效期至")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] validUntil;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 