package cn.iocoder.yudao.module.waste.controller.app.appointment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "用户 APP - 危废转移预约创建 Request VO")
@Data
public class AppAppointmentCreateReqVO {

    @Schema(description = "产废企业联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "产废企业联系人姓名不能为空")
    @Size(max = 50, message = "产废企业联系人姓名长度不能超过50个字符")
    private String producerContactName;

    @Schema(description = "产废企业联系人电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "产废企业联系人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "产废企业联系人电话格式不正确")
    private String producerContactPhone;

    @Schema(description = "废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    @NotBlank(message = "废物代码不能为空")
    @Size(max = 20, message = "废物代码长度不能超过20个字符")
    private String wasteCode;

    @Schema(description = "废物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗废物")
    @NotBlank(message = "废物名称不能为空")
    @Size(max = 100, message = "废物名称长度不能超过100个字符")
    private String wasteName;

    @Schema(description = "废物类别", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗")
    @NotBlank(message = "废物类别不能为空")
    @Size(max = 50, message = "废物类别长度不能超过50个字符")
    private String wasteCategory;

    @Schema(description = "预估数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.50")
    @NotNull(message = "预估数量不能为空")
    @DecimalMin(value = "0.01", message = "预估数量必须大于0")
    @Digits(integer = 10, fraction = 2, message = "预估数量格式不正确")
    private BigDecimal estimatedQuantity;

    @Schema(description = "数量单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "吨")
    @NotBlank(message = "数量单位不能为空")
    @Size(max = 20, message = "数量单位长度不能超过20个字符")
    private String quantityUnit;

    @Schema(description = "废物描述", example = "医疗废物，包含注射器、输液袋等")
    @Size(max = 500, message = "废物描述长度不能超过500个字符")
    private String wasteDescription;

    @Schema(description = "取货地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区xxx街道xxx号")
    @NotBlank(message = "取货地址不能为空")
    @Size(max = 200, message = "取货地址长度不能超过200个字符")
    private String pickupAddress;

    @Schema(description = "取货纬度", example = "39.9042")
    private BigDecimal pickupLatitude;

    @Schema(description = "取货经度", example = "116.4074")
    private BigDecimal pickupLongitude;

    @Schema(description = "送达地址", example = "北京市大兴区xxx处理中心")
    @Size(max = 200, message = "送达地址长度不能超过200个字符")
    private String deliveryAddress;

    @Schema(description = "送达纬度", example = "39.7285")
    private BigDecimal deliveryLatitude;

    @Schema(description = "送达经度", example = "116.3436")
    private BigDecimal deliveryLongitude;

    @Schema(description = "期望取货时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "期望取货时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expectedPickupTime;

    @Schema(description = "期望送达时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expectedDeliveryTime;

    @Schema(description = "是否紧急", example = "false")
    private Boolean isUrgent;

    @Schema(description = "优先级", example = "1")
    @Min(value = 0, message = "优先级不能小于0")
    @Max(value = 100, message = "优先级不能大于100")
    private Integer priorityLevel;

    @Schema(description = "合同ID", example = "1024")
    private Long contractId;

    @Schema(description = "备注", example = "请在工作时间取货")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

} 