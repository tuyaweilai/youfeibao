package cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 / 自然人端 - 到站预约 Response VO")
@Data
public class AppointmentRespVO {

    @Schema(description = "预约编号")
    private Long id;

    @Schema(description = "预约编号")
    private String appointmentNo;

    @Schema(description = "自然人主体编号")
    private Long naturalPersonId;

    @Schema(description = "收方（出售者）档案编号")
    private Long payeeId;

    @Schema(description = "租户编号（回收企业）")
    private Long tenantId;

    @Schema(description = "回收企业名称")
    private String enterpriseName;

    @Schema(description = "出售者姓名")
    private String sellerName;

    // ==================== 场站 ====================

    @Schema(description = "场站编号")
    private Long stationId;

    @Schema(description = "场站码")
    private String stationCode;

    @Schema(description = "场站名称")
    private String stationName;

    // ==================== 货物与车辆 ====================

    @Schema(description = "品类配置编号")
    private Long goodsConfigId;

    @Schema(description = "品类名称")
    private String categoryName;

    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "预计数量")
    private BigDecimal expectedQuantity;

    @Schema(description = "预计数量文案（一律以「约」标注，如「约 12.5 吨」）")
    private String expectedQuantityText;

    @Schema(description = "车牌号")
    private String plateNo;

    // ==================== 到站 ====================

    @Schema(description = "预计到站时间")
    private LocalDateTime expectedArrivalTime;

    @Schema(description = "状态：0-待到站，1-已到场，2-未到场，9-已取消")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "实际到场时间")
    private LocalDateTime arrivedAt;

    @Schema(description = "到场后建的收购单编号")
    private Long acquisitionId;

    @Schema(description = "取消时间")
    private LocalDateTime cancelledAt;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "未到场说明")
    private String noShowReason;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "口径说明：预约不是订单，不占额度、不产生开票、不进五流")
    private String scopeNote;

}
