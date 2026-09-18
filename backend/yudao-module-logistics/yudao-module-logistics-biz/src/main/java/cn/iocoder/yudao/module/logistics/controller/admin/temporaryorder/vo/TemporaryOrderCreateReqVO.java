package cn.iocoder.yudao.module.logistics.controller.admin.temporaryorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 物流临时订单创建 Request VO")
@Data
public class TemporaryOrderCreateReqVO {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TO202401010001")
    @NotBlank(message = "订单编号不能为空")
    @Size(max = 64, message = "订单编号长度不能超过 64 个字符")
    private String orderNo;

    @Schema(description = "运输任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "运输任务ID不能为空")
    private Long taskId;

    @Schema(description = "司机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "司机ID不能为空")
    private Long driverId;

    @Schema(description = "司机姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "司机姓名不能为空")
    @Size(max = 64, message = "司机姓名长度不能超过 64 个字符")
    private String driverName;

    @Schema(description = "废料类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    @NotBlank(message = "废料类型不能为空")
    @Size(max = 50, message = "废料类型长度不能超过 50 个字符")
    private String wasteType;

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

    @Schema(description = "取货地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区xxx街道xxx号")
    @NotBlank(message = "取货地址不能为空")
    @Size(max = 500, message = "取货地址长度不能超过 500 个字符")
    private String pickupLocation;

    @Schema(description = "取货纬度", example = "39.9042")
    @DecimalMin(value = "-90", message = "纬度值不能小于-90")
    @DecimalMax(value = "90", message = "纬度值不能大于90")
    @Digits(integer = 3, fraction = 7, message = "纬度格式不正确")
    private BigDecimal pickupLatitude;

    @Schema(description = "取货经度", example = "116.4074")
    @DecimalMin(value = "-180", message = "经度值不能小于-180")
    @DecimalMax(value = "180", message = "经度值不能大于180")
    @Digits(integer = 3, fraction = 7, message = "经度格式不正确")
    private BigDecimal pickupLongitude;

    @Schema(description = "取货时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 10:00:00")
    @NotNull(message = "取货时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime pickupTime;

    @Schema(description = "产废方姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotBlank(message = "产废方姓名不能为空")
    @Size(max = 64, message = "产废方姓名长度不能超过 64 个字符")
    private String producerName;

    @Schema(description = "产废方电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "产废方电话不能为空")
    @Size(max = 20, message = "产废方电话长度不能超过 20 个字符")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "产废方电话格式不正确")
    private String producerPhone;

    @Schema(description = "产废方身份证号", example = "110101199001011234")
    @Size(max = 20, message = "产废方身份证号长度不能超过 20 个字符")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$", 
             message = "产废方身份证号格式不正确")
    private String producerIdCard;

    @Schema(description = "发现现场照片URLs", example = "[\"http://example.com/photo1.jpg\",\"http://example.com/photo2.jpg\"]")
    private String discoveryPhotos;

    @Schema(description = "预估价值", example = "1000.00")
    @DecimalMin(value = "0", message = "预估价值必须大于等于0")
    @Digits(integer = 8, fraction = 2, message = "预估价值格式不正确")
    private BigDecimal estimatedValue;

    @Schema(description = "支付状态", example = "0")
    @Min(value = 0, message = "支付状态值不能小于0")
    @Max(value = 3, message = "支付状态值不能大于3")
    private Integer paymentStatus;

    @Schema(description = "备注", example = "临时发现的废料")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

} 