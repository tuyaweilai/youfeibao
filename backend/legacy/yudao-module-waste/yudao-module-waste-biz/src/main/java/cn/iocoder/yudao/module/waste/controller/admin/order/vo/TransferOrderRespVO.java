package cn.iocoder.yudao.module.waste.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 危废转移订单 Response VO")
@Data
public class TransferOrderRespVO {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WO202412010001")
    private String orderNo;

    // ========== 关联信息 ==========
    @Schema(description = "关联预约单ID", example = "1024")
    private Long appointmentId;

    @Schema(description = "关联报价记录ID", example = "2048")
    private Long quotationId;

    // ========== 企业信息 ==========
    @Schema(description = "产废企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long producingEnterpriseId;

    @Schema(description = "产废门店ID", example = "512")
    private Long producingStoreId;

    @Schema(description = "回收企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long recyclingEnterpriseId;

    // ========== 物流关联（通过接口协作） ==========
    @Schema(description = "关联的物流运输任务ID", example = "4096")
    private Long transportTaskId;

    @Schema(description = "关联的车辆过磅ID", example = "8192")
    private Long vehicleWeighingId;

    @Schema(description = "物流状态快照", example = "1")
    private Integer logisticsStatus;

    // ========== 废物信息 ==========
    @Schema(description = "危险废物代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "HW01")
    private String wasteCode;

    @Schema(description = "危险废物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "医疗废物")
    private String wasteName;

    @Schema(description = "预估数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.50")
    private BigDecimal estimatedQuantity;

    @Schema(description = "收运员确认数量", example = "98.20")
    private BigDecimal confirmedQuantity;

    @Schema(description = "基于过磅的分摊数量", example = "97.80")
    private BigDecimal allocatedQuantity;

    @Schema(description = "数量单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "吨")
    private String quantityUnit;

    @Schema(description = "包装方式", example = "桶装")
    private String packagingType;

    // ========== 价格信息 ==========
    @Schema(description = "单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "1200.00")
    private BigDecimal unitPrice;

    @Schema(description = "预估总金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "120000.00")
    private BigDecimal estimatedAmount;

    @Schema(description = "最终结算金额", example = "117360.00")
    private BigDecimal finalAmount;

    // ========== 业务状态 ==========
    @Schema(description = "业务状态 (0:待确认, 1:已确认, 2:待结算, 3:已结算, 4:已完成, 5:已取消)", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer businessStatus;

    // ========== 付款管理 ==========
    @Schema(description = "关联的产废企业付款配置ID", example = "128")
    private Long paymentConfigId;

    @Schema(description = "实际付款方式 (1:对公结算, 2:个人结算)", example = "1")
    private Integer paymentMethodType;

    @Schema(description = "付款状态 (0:未付款, 1:已付款, 2:付款失败, 3:待凭证上传, 4:凭证已上传, 5:凭证已确认)", example = "1")
    private Integer paymentStatus;

    @Schema(description = "付款完成时间", example = "2024-12-01 15:30:00")
    private LocalDateTime paymentCompletedTime;

    @Schema(description = "对公付款凭证ID", example = "256")
    private Long paymentVoucherId;

    // ========== 收货确认 ==========
    @Schema(description = "收货确认时间", example = "2024-12-01 14:30:00")
    private LocalDateTime pickupConfirmedTime;

    @Schema(description = "收货确认人", example = "张三")
    private String pickupConfirmedBy;

    @Schema(description = "收货确认GPS位置", example = "116.397470,39.908823")
    private String pickupConfirmedLocation;

    // ========== 分摊信息 ==========
    @Schema(description = "在车辆总重量中的分摊比例", example = "0.65000")
    private BigDecimal allocationRatio;

    @Schema(description = "订单分摊完成时间", example = "2024-12-01 16:00:00")
    private LocalDateTime allocationCompletedTime;

    @Schema(description = "与预估量的差异", example = "-2.70")
    private BigDecimal varianceFromEstimate;

    @Schema(description = "差异率 (%)", example = "-2.68")
    private BigDecimal varianceRate;

    // ========== 订单来源 ==========
    @Schema(description = "订单来源类型 (0:预约转订单, 1:扫街临时订单, 2:补单)", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer sourceType;

    @Schema(description = "关联订单ID (用于补单场景)", example = "256")
    private Long relatedOrderId;

    @Schema(description = "用户备注", example = "特殊要求说明")
    private String userRemark;

    @Schema(description = "内部备注", example = "内部处理说明")
    private String internalRemark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-12-01 10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-12-01 16:00:00")
    private LocalDateTime updateTime;

} 