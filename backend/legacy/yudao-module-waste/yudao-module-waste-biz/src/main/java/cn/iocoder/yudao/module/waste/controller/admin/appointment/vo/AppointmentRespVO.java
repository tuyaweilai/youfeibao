package cn.iocoder.yudao.module.waste.controller.admin.appointment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 危废转移预约 Response VO")
@Data
public class AppointmentRespVO {

    @Schema(description = "预约ID", example = "1024")
    private Long id;

    @Schema(description = "预约单号", example = "AP202412010001")
    private String appointmentNo;

    // ========== 产废企业信息 ==========
    @Schema(description = "产废企业ID", example = "1024")
    private Long producerEnterpriseId;

    @Schema(description = "产废企业名称", example = "芋道科技")
    private String producerEnterpriseName;

    @Schema(description = "产废企业联系人", example = "张三")
    private String producerContactName;

    @Schema(description = "产废企业联系电话", example = "13800138000")
    private String producerContactPhone;

    // ========== 回收企业信息 ==========
    @Schema(description = "回收企业ID", example = "2048")
    private Long recyclerEnterpriseId;

    @Schema(description = "回收企业名称", example = "回收公司")
    private String recyclerEnterpriseName;

    @Schema(description = "分配方式", example = "0")
    private Integer assignmentType;

    @Schema(description = "分配时间", example = "2024-12-01 09:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime assignmentTime;

    @Schema(description = "分配操作人", example = "admin")
    private String assignmentOperator;

    // ========== 废物信息 ==========
    @Schema(description = "危险废物代码", example = "HW01")
    private String wasteCode;

    @Schema(description = "危险废物名称", example = "医疗废物")
    private String wasteName;

    @Schema(description = "废物类别", example = "医疗")
    private String wasteCategory;

    @Schema(description = "预估数量", example = "100.50")
    private BigDecimal estimatedQuantity;

    @Schema(description = "数量单位", example = "吨")
    private String quantityUnit;

    @Schema(description = "废物描述", example = "医疗废物详细描述")
    private String wasteDescription;

    // ========== 地址信息 ==========
    @Schema(description = "取货地址", example = "北京市朝阳区xxx街道xxx号")
    private String pickupAddress;

    @Schema(description = "取货地址纬度", example = "39.908823")
    private BigDecimal pickupLatitude;

    @Schema(description = "取货地址经度", example = "116.397470")
    private BigDecimal pickupLongitude;

    @Schema(description = "送货地址", example = "上海市浦东新区xxx路xxx号")
    private String deliveryAddress;

    @Schema(description = "送货地址纬度", example = "31.230416")
    private BigDecimal deliveryLatitude;

    @Schema(description = "送货地址经度", example = "121.473701")
    private BigDecimal deliveryLongitude;

    // ========== 时间信息 ==========
    @Schema(description = "期望取货时间", example = "2024-12-01 10:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expectedPickupTime;

    @Schema(description = "期望送达时间", example = "2024-12-01 18:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expectedDeliveryTime;

    // ========== 状态信息 ==========
    @Schema(description = "预约状态", example = "0")
    private Integer appointmentStatus;

    @Schema(description = "确认时间", example = "2024-12-01 11:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime confirmTime;

    @Schema(description = "拒绝时间", example = "2024-12-01 11:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime rejectTime;

    @Schema(description = "拒绝原因", example = "不符合处理要求")
    private String rejectReason;

    @Schema(description = "取消时间", example = "2024-12-01 11:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime cancelTime;

    @Schema(description = "取消原因", example = "客户取消")
    private String cancelReason;

    // ========== 业务信息 ==========
    @Schema(description = "业务模式", example = "0")
    private Integer businessMode;

    @Schema(description = "是否紧急", example = "false")
    private Boolean isUrgent;

    @Schema(description = "优先级", example = "0")
    private Integer priorityLevel;

    // ========== 关联信息 ==========
    @Schema(description = "关联订单ID", example = "1024")
    private Long orderId;

    @Schema(description = "关联合同ID", example = "1024")
    private Long contractId;

    @Schema(description = "备注", example = "特殊要求说明")
    private String remark;

    @Schema(description = "创建时间", example = "2024-12-01 08:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-12-01 12:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime updateTime;

} 