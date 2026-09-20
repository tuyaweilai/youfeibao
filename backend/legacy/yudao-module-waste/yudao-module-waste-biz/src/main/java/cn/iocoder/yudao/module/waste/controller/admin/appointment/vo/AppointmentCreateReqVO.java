package cn.iocoder.yudao.module.waste.controller.admin.appointment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 危废转移预约创建 Request VO")
@Data
public class AppointmentCreateReqVO {

    // ========== 产废企业信息 ==========
    @Schema(description = "产废企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "产废企业ID不能为空")
    private Long producerEnterpriseId;

    @Schema(description = "产废企业联系人", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "产废企业联系人不能为空")
    @Size(max = 64, message = "产废企业联系人长度不能超过64个字符")
    private String producerContactName;

    @Schema(description = "产废企业联系电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "产废企业联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "产废企业联系电话格式不正确")
    private String producerContactPhone;

    // ========== 回收企业信息 ==========
    @Schema(description = "回收企业ID", example = "2048")
    private Long recyclerEnterpriseId;

    @Schema(description = "分配方式", example = "0")
    private Integer assignmentType;

    // ========== 废物信息 ==========
    @Schema(description = "危险废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    @NotBlank(message = "危险废物代码不能为空")
    @Size(max = 50, message = "危险废物代码长度不能超过50个字符")
    private String wasteCode;

    @Schema(description = "危险废物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗废物")
    @NotBlank(message = "危险废物名称不能为空")
    @Size(max = 100, message = "危险废物名称长度不能超过100个字符")
    private String wasteName;

    @Schema(description = "废物类别", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗")
    @NotBlank(message = "废物类别不能为空")
    @Size(max = 50, message = "废物类别长度不能超过50个字符")
    private String wasteCategory;

    @Schema(description = "预估数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.50")
    @NotNull(message = "预估数量不能为空")
    @DecimalMin(value = "0.01", message = "预估数量必须大于0")
    @Digits(integer = 8, fraction = 2, message = "预估数量格式不正确")
    private BigDecimal estimatedQuantity;

    @Schema(description = "数量单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "吨")
    @NotBlank(message = "数量单位不能为空")
    @Size(max = 10, message = "数量单位长度不能超过10个字符")
    private String quantityUnit;

    @Schema(description = "废物描述", example = "医疗废物详细描述")
    @Size(max = 500, message = "废物描述长度不能超过500个字符")
    private String wasteDescription;

    // ========== 地址信息 ==========
    @Schema(description = "取货地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区xxx街道xxx号")
    @NotBlank(message = "取货地址不能为空")
    @Size(max = 500, message = "取货地址长度不能超过500个字符")
    private String pickupAddress;

    @Schema(description = "取货地址纬度", example = "39.908823")
    @DecimalMin(value = "-90", message = "纬度值无效")
    @DecimalMax(value = "90", message = "纬度值无效")
    private BigDecimal pickupLatitude;

    @Schema(description = "取货地址经度", example = "116.397470")
    @DecimalMin(value = "-180", message = "经度值无效")
    @DecimalMax(value = "180", message = "经度值无效")
    private BigDecimal pickupLongitude;

    @Schema(description = "送货地址", example = "上海市浦东新区xxx路xxx号")
    @Size(max = 500, message = "送货地址长度不能超过500个字符")
    private String deliveryAddress;

    @Schema(description = "送货地址纬度", example = "31.230416")
    @DecimalMin(value = "-90", message = "纬度值无效")
    @DecimalMax(value = "90", message = "纬度值无效")
    private BigDecimal deliveryLatitude;

    @Schema(description = "送货地址经度", example = "121.473701")
    @DecimalMin(value = "-180", message = "经度值无效")
    @DecimalMax(value = "180", message = "经度值无效")
    private BigDecimal deliveryLongitude;

    // ========== 时间信息 ==========
    @Schema(description = "期望取货时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-12-01 10:00:00")
    @NotNull(message = "期望取货时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expectedPickupTime;

    @Schema(description = "期望送达时间", example = "2024-12-01 18:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expectedDeliveryTime;

    // ========== 业务信息 ==========
    @Schema(description = "业务模式", example = "0")
    private Integer businessMode;

    @Schema(description = "是否紧急", example = "false")
    private Boolean isUrgent;

    @Schema(description = "优先级", example = "0")
    @Min(value = 0, message = "优先级不能小于0")
    @Max(value = 2, message = "优先级不能大于2")
    private Integer priorityLevel;

    @Schema(description = "关联合同ID", example = "1024")
    private Long contractId;

    @Schema(description = "备注", example = "特殊要求说明")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

} 