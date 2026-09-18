package cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 物流现金代付记录 Response VO")
@Data
public class CashAdvanceRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "运输任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long taskId;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;

    @Schema(description = "司机ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long driverId;

    @Schema(description = "司机姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String driverName;

    @Schema(description = "支付金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    private BigDecimal paymentAmount;

    @Schema(description = "支付时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 10:00:00")
    private LocalDateTime paymentTime;

    @Schema(description = "支付地点", example = "北京市朝阳区xxx街道xxx号")
    private String paymentLocation;

    @Schema(description = "支付方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "CASH")
    private String paymentMethod;

    @Schema(description = "收款人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    private String payeeName;

    @Schema(description = "收款人电话", example = "13800138000")
    private String payeePhone;

    @Schema(description = "支付现场照片URLs", example = "[\"http://example.com/photo1.jpg\",\"http://example.com/photo2.jpg\"]")
    private String paymentPhotos;

    @Schema(description = "收据照片URLs", example = "[\"http://example.com/receipt1.jpg\",\"http://example.com/receipt2.jpg\"]")
    private String receiptPhotos;

    @Schema(description = "通知状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer notifyStatus;

    @Schema(description = "通知状态名称", example = "未通知")
    private String notifyStatusName;

    @Schema(description = "通知时间", example = "2024-01-01 12:00:00")
    private LocalDateTime notifyTime;

    @Schema(description = "对账状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer reconcileStatus;

    @Schema(description = "对账状态名称", example = "未对账")
    private String reconcileStatusName;

    @Schema(description = "对账时间", example = "2024-01-01 14:00:00")
    private LocalDateTime reconcileTime;

    @Schema(description = "对账备注", example = "对账完成")
    private String reconcileRemark;

    @Schema(description = "对账操作员ID", example = "2048")
    private Long reconcileOperatorId;

    @Schema(description = "对账操作员姓名", example = "王五")
    private String reconcileOperatorName;

    @Schema(description = "备注", example = "现金代付记录")
    private String remark;

    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long tenantId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 08:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 18:00:00")
    private LocalDateTime updateTime;

} 