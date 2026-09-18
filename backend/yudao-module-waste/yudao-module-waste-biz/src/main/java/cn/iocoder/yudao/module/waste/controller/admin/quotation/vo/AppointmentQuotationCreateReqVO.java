package cn.iocoder.yudao.module.waste.controller.admin.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 预约报价记录创建 Request VO")
@Data
public class AppointmentQuotationCreateReqVO {

    @Schema(description = "预约单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "预约单ID不能为空")
    private Long appointmentId;

    @Schema(description = "回收企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "回收企业ID不能为空")
    private Long recyclingEnterpriseId;

    @Schema(description = "报价金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "报价金额不能为空")
    @DecimalMin(value = "0", message = "报价金额不能小于0")
    private BigDecimal quotedPrice;

    @Schema(description = "报价备注", example = "报价说明")
    @Size(max = 500, message = "报价备注长度不能超过500个字符")
    private String quotationRemark;

    @Schema(description = "有效期至", example = "2023-12-31 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime validUntil;

    @Schema(description = "报价时间", example = "2023-12-01 10:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime quotationTime;

    @Schema(description = "状态", example = "1")
    private Integer status;

} 