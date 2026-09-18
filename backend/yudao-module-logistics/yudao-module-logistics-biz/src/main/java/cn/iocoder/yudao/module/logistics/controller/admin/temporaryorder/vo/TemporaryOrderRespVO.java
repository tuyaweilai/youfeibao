package cn.iocoder.yudao.module.logistics.controller.admin.temporaryorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 物流临时订单 Response VO")
@Data
public class TemporaryOrderRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TO202401010001")
    private String orderNo;

    @Schema(description = "运输任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long taskId;

    @Schema(description = "司机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long driverId;

    @Schema(description = "司机姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String driverName;

    @Schema(description = "废料类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    private String wasteType;

    @Schema(description = "废料名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "废机油")
    private String wasteName;

    @Schema(description = "预计数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.50")
    private BigDecimal estimatedQuantity;

    @Schema(description = "数量单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "吨")
    private String quantityUnit;

    @Schema(description = "取货地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区xxx街道xxx号")
    private String pickupLocation;

    @Schema(description = "取货纬度", example = "39.9042")
    private BigDecimal pickupLatitude;

    @Schema(description = "取货经度", example = "116.4074")
    private BigDecimal pickupLongitude;

    @Schema(description = "取货时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 10:00:00")
    private LocalDateTime pickupTime;

    @Schema(description = "产废方姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    private String producerName;

    @Schema(description = "产废方电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String producerPhone;

    @Schema(description = "产废方身份证号", example = "110101199001011234")
    private String producerIdCard;

    @Schema(description = "发现现场照片URLs", example = "[\"http://example.com/photo1.jpg\",\"http://example.com/photo2.jpg\"]")
    private String discoveryPhotos;

    @Schema(description = "预估价值", example = "1000.00")
    private BigDecimal estimatedValue;

    @Schema(description = "支付状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer paymentStatus;

    @Schema(description = "支付状态名称", example = "未支付")
    private String paymentStatusName;

    @Schema(description = "支付金额", example = "800.00")
    private BigDecimal paymentAmount;

    @Schema(description = "支付时间", example = "2024-01-01 12:00:00")
    private LocalDateTime paymentTime;

    @Schema(description = "支付方式", example = "微信支付")
    private String paymentMethod;

    @Schema(description = "支付凭证URL", example = "http://example.com/voucher.jpg")
    private String paymentVoucherUrl;

    @Schema(description = "是否已转为正式订单", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean convertedToFormal;

    @Schema(description = "正式订单ID", example = "2048")
    private Long formalOrderId;

    @Schema(description = "转换时间", example = "2024-01-01 14:00:00")
    private LocalDateTime conversionTime;

    @Schema(description = "备注", example = "临时发现的废料")
    private String remark;

    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long tenantId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 08:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 18:00:00")
    private LocalDateTime updateTime;

} 