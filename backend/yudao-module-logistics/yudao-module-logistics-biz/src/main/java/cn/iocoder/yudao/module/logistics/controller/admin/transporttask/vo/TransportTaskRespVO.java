package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 物流运输任务 Response VO")
@Data
public class TransportTaskRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TT202401010001")
    private String taskNo;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORD202401010001")
    private String orderNo;

    @Schema(description = "所属企业ID", example = "1024")
    private Long enterpriseId;

    @Schema(description = "车辆ID", example = "1024")
    private Long vehicleId;

    @Schema(description = "司机ID", example = "1024")
    private Long driverId;

    @Schema(description = "分配时间", example = "2024-01-01 09:00:00")
    private LocalDateTime assignTime;

    @Schema(description = "接受时间", example = "2024-01-01 09:30:00")
    private LocalDateTime acceptTime;

    @Schema(description = "取货企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long pickupEnterpriseId;

    @Schema(description = "取货地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区xxx街道xxx号")
    private String pickupAddress;

    @Schema(description = "取货联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String pickupContactName;

    @Schema(description = "取货联系人电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String pickupContactPhone;

    @Schema(description = "预计取货时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 10:00:00")
    private LocalDateTime expectedPickupTime;

    @Schema(description = "实际取货时间", example = "2024-01-01 10:15:00")
    private LocalDateTime actualPickupTime;

    @Schema(description = "送货企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long deliveryEnterpriseId;

    @Schema(description = "送货地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "上海市浦东新区xxx街道xxx号")
    private String deliveryAddress;

    @Schema(description = "送货联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    private String deliveryContactName;

    @Schema(description = "送货联系人电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13900139000")
    private String deliveryContactPhone;

    @Schema(description = "预计送货时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 16:00:00")
    private LocalDateTime expectedDeliveryTime;

    @Schema(description = "实际送货时间", example = "2024-01-01 15:45:00")
    private LocalDateTime actualDeliveryTime;

    @Schema(description = "废料代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    private String wasteCode;

    @Schema(description = "废料名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "废机油")
    private String wasteName;

    @Schema(description = "预计数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.50")
    private BigDecimal estimatedQuantity;

    @Schema(description = "实际数量", example = "98.30")
    private BigDecimal actualQuantity;

    @Schema(description = "数量单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "吨")
    private String quantityUnit;

    @Schema(description = "任务状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer taskStatus;

    @Schema(description = "任务状态名称", example = "待分配")
    private String taskStatusName;

    @Schema(description = "当前位置", example = "北京市朝阳区xxx街道")
    private String currentLocation;

    @Schema(description = "当前纬度", example = "39.9042")
    private BigDecimal currentLatitude;

    @Schema(description = "当前经度", example = "116.4074")
    private BigDecimal currentLongitude;

    @Schema(description = "是否异常", example = "false")
    private Boolean isAbnormal;

    @Schema(description = "异常类型", example = "1")
    private Integer abnormalType;

    @Schema(description = "异常类型名称", example = "车辆故障")
    private String abnormalTypeName;

    @Schema(description = "异常原因", example = "车辆发动机故障")
    private String abnormalReason;

    @Schema(description = "是否临时任务", example = "false")
    private Boolean isTemporary;

    @Schema(description = "备注", example = "紧急运输任务")
    private String remark;

    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long tenantId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 08:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 18:00:00")
    private LocalDateTime updateTime;

} 