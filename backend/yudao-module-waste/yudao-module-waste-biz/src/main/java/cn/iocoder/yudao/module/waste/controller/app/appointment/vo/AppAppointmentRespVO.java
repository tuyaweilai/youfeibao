package cn.iocoder.yudao.module.waste.controller.app.appointment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "用户 APP - 危废转移预约 Response VO")
@Data
public class AppAppointmentRespVO {

    @Schema(description = "预约ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "预约单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "AP20231201001234")
    private String appointmentNo;

    @Schema(description = "产废企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long producerEnterpriseId;

    @Schema(description = "产废企业名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京某医院")
    private String producerEnterpriseName;

    @Schema(description = "产废企业联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String producerContactName;

    @Schema(description = "产废企业联系人电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String producerContactPhone;

    @Schema(description = "回收企业ID", example = "2")
    private Long recyclerEnterpriseId;

    @Schema(description = "回收企业名称", example = "北京危废处理中心")
    private String recyclerEnterpriseName;

    @Schema(description = "分配方式", example = "1")
    private Integer assignmentType;

    @Schema(description = "分配时间")
    private LocalDateTime assignmentTime;

    @Schema(description = "分配操作员", example = "admin")
    private String assignmentOperator;

    @Schema(description = "废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    private String wasteCode;

    @Schema(description = "废物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗废物")
    private String wasteName;

    @Schema(description = "废物类别", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗")
    private String wasteCategory;

    @Schema(description = "预估数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.50")
    private BigDecimal estimatedQuantity;

    @Schema(description = "数量单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "吨")
    private String quantityUnit;

    @Schema(description = "废物描述", example = "医疗废物，包含注射器、输液袋等")
    private String wasteDescription;

    @Schema(description = "取货地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区xxx街道xxx号")
    private String pickupAddress;

    @Schema(description = "取货纬度", example = "39.9042")
    private BigDecimal pickupLatitude;

    @Schema(description = "取货经度", example = "116.4074")
    private BigDecimal pickupLongitude;

    @Schema(description = "送达地址", example = "北京市大兴区xxx处理中心")
    private String deliveryAddress;

    @Schema(description = "送达纬度", example = "39.7285")
    private BigDecimal deliveryLatitude;

    @Schema(description = "送达经度", example = "116.3436")
    private BigDecimal deliveryLongitude;

    @Schema(description = "期望取货时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expectedPickupTime;

    @Schema(description = "期望送达时间")
    private LocalDateTime expectedDeliveryTime;

    @Schema(description = "预约状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer appointmentStatus;

    @Schema(description = "预约状态名称", example = "待处理")
    private String appointmentStatusName;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "拒绝时间")
    private LocalDateTime rejectTime;

    @Schema(description = "拒绝原因", example = "不符合接收条件")
    private String rejectReason;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "取消原因", example = "临时变更计划")
    private String cancelReason;

    @Schema(description = "业务模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer businessMode;

    @Schema(description = "是否紧急", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean isUrgent;

    @Schema(description = "优先级", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer priorityLevel;

    @Schema(description = "订单ID", example = "2048")
    private Long orderId;

    @Schema(description = "合同ID", example = "1024")
    private Long contractId;

    @Schema(description = "备注", example = "请在工作时间取货")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 