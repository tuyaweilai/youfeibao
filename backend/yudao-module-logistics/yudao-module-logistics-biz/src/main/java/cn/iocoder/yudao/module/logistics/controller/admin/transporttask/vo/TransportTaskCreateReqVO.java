package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 物流运输任务创建 Request VO")
@Data
public class TransportTaskCreateReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TT202401010001")
    @NotBlank(message = "任务编号不能为空")
    @Size(max = 64, message = "任务编号长度不能超过 64 个字符")
    private String taskNo;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORD202401010001")
    @NotBlank(message = "订单编号不能为空")
    @Size(max = 64, message = "订单编号长度不能超过 64 个字符")
    private String orderNo;

    @Schema(description = "所属企业ID", example = "1024")
    private Long enterpriseId;

    @Schema(description = "车辆ID", example = "1024")
    private Long vehicleId;

    @Schema(description = "司机ID", example = "1024")
    private Long driverId;

    @Schema(description = "取货企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "取货企业ID不能为空")
    private Long pickupEnterpriseId;

    @Schema(description = "取货地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区xxx街道xxx号")
    @NotBlank(message = "取货地址不能为空")
    @Size(max = 500, message = "取货地址长度不能超过 500 个字符")
    private String pickupAddress;

    @Schema(description = "取货联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "取货联系人姓名不能为空")
    @Size(max = 64, message = "取货联系人姓名长度不能超过 64 个字符")
    private String pickupContactName;

    @Schema(description = "取货联系人电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "取货联系人电话不能为空")
    @Size(max = 20, message = "取货联系人电话长度不能超过 20 个字符")
    private String pickupContactPhone;

    @Schema(description = "预计取货时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 10:00:00")
    @NotNull(message = "预计取货时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expectedPickupTime;

    @Schema(description = "送货企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "送货企业ID不能为空")
    private Long deliveryEnterpriseId;

    @Schema(description = "送货地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "上海市浦东新区xxx街道xxx号")
    @NotBlank(message = "送货地址不能为空")
    @Size(max = 500, message = "送货地址长度不能超过 500 个字符")
    private String deliveryAddress;

    @Schema(description = "送货联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotBlank(message = "送货联系人姓名不能为空")
    @Size(max = 64, message = "送货联系人姓名长度不能超过 64 个字符")
    private String deliveryContactName;

    @Schema(description = "送货联系人电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13900139000")
    @NotBlank(message = "送货联系人电话不能为空")
    @Size(max = 20, message = "送货联系人电话长度不能超过 20 个字符")
    private String deliveryContactPhone;

    @Schema(description = "预计送货时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 16:00:00")
    @NotNull(message = "预计送货时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expectedDeliveryTime;

    @Schema(description = "废料代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    @NotBlank(message = "废料代码不能为空")
    @Size(max = 50, message = "废料代码长度不能超过 50 个字符")
    private String wasteCode;

    @Schema(description = "废料名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "废机油")
    @NotBlank(message = "废料名称不能为空")
    @Size(max = 100, message = "废料名称长度不能超过 100 个字符")
    private String wasteName;

    @Schema(description = "预计数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.50")
    @NotNull(message = "预计数量不能为空")
    @DecimalMin(value = "0", message = "预计数量必须大于等于0")
    @Digits(integer = 8, fraction = 2, message = "预计数量格式不正确")
    private BigDecimal estimatedQuantity;

    @Schema(description = "数量单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "吨")
    @NotBlank(message = "数量单位不能为空")
    @Size(max = 10, message = "数量单位长度不能超过 10 个字符")
    private String quantityUnit;

    @Schema(description = "任务状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "任务状态不能为空")
    @Min(value = 0, message = "任务状态值不能小于0")
    @Max(value = 7, message = "任务状态值不能大于7")
    private Integer taskStatus;

    @Schema(description = "是否临时任务", example = "false")
    private Boolean isTemporary;

    @Schema(description = "备注", example = "紧急运输任务")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

} 