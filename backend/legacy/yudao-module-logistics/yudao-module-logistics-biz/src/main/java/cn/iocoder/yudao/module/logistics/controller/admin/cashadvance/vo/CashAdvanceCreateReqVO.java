package cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 物流现金代付记录创建 Request VO")
@Data
public class CashAdvanceCreateReqVO {

    @Schema(description = "运输任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "运输任务ID不能为空")
    private Long taskId;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "司机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "司机ID不能为空")
    private Long driverId;

    @Schema(description = "司机姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "司机姓名不能为空")
    @Size(max = 64, message = "司机姓名长度不能超过 64 个字符")
    private String driverName;

    @Schema(description = "支付金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    @Digits(integer = 8, fraction = 2, message = "支付金额格式不正确")
    private BigDecimal paymentAmount;

    @Schema(description = "支付时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 10:00:00")
    @NotNull(message = "支付时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime paymentTime;

    @Schema(description = "支付地点", example = "北京市朝阳区xxx街道xxx号")
    @Size(max = 500, message = "支付地点长度不能超过 500 个字符")
    private String paymentLocation;

    @Schema(description = "支付方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "CASH")
    @NotBlank(message = "支付方式不能为空")
    @Size(max = 20, message = "支付方式长度不能超过 20 个字符")
    private String paymentMethod;

    @Schema(description = "收款人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotBlank(message = "收款人姓名不能为空")
    @Size(max = 64, message = "收款人姓名长度不能超过 64 个字符")
    private String payeeName;

    @Schema(description = "收款人电话", example = "13800138000")
    @Size(max = 20, message = "收款人电话长度不能超过 20 个字符")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收款人电话格式不正确")
    private String payeePhone;

    @Schema(description = "支付现场照片URLs", example = "[\"http://example.com/photo1.jpg\",\"http://example.com/photo2.jpg\"]")
    private String paymentPhotos;

    @Schema(description = "收据照片URLs", example = "[\"http://example.com/receipt1.jpg\",\"http://example.com/receipt2.jpg\"]")
    private String receiptPhotos;

    @Schema(description = "通知状态", example = "0")
    @Min(value = 0, message = "通知状态值不能小于0")
    @Max(value = 2, message = "通知状态值不能大于2")
    private Integer notifyStatus;

    @Schema(description = "对账状态", example = "0")
    @Min(value = 0, message = "对账状态值不能小于0")
    @Max(value = 2, message = "对账状态值不能大于2")
    private Integer reconcileStatus;

    @Schema(description = "备注", example = "现金代付记录")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

} 