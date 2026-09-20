package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 - 采购订单 Response VO（#46 T08）。
 *
 * <p>{@link #expired}、{@link #usableAsPurchaseBasis} 与 {@link #statusName} 都是推导值：
 * 「执行中 + 结束日期不早于今天」才是有效采购依据，超期不落库。
 */
@Schema(description = "管理后台 - 采购订单 Response VO")
@Data
public class PurchaseOrderRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "关联采购合同编号")
    private Long contractId;

    @Schema(description = "采购合同号快照")
    private String contractNo;

    @Schema(description = "交易对方主体类型")
    private Integer counterpartyType;

    @Schema(description = "交易对方主体类型名")
    private String counterpartyTypeName;

    @Schema(description = "自然人出售者档案编号")
    private Long payeeId;

    @Schema(description = "单位供货方编号")
    private Long supplierId;

    @Schema(description = "交易对方名称快照")
    private String counterpartyName;

    @Schema(description = "执行场站编号")
    private Long stationId;

    @Schema(description = "执行场站名称快照")
    private String stationName;

    @Schema(description = "执行开始日期")
    private LocalDate startDate;

    @Schema(description = "执行结束日期")
    private LocalDate endDate;

    @Schema(description = "状态：0-草稿，1-执行中，2-暂停，3-完成，4-关闭")
    private Integer status;

    @Schema(description = "状态名（执行中但已过期时显示「过期」）")
    private String statusName;

    @Schema(description = "是否已过期（结束日期早于今天）")
    private Boolean expired;

    @Schema(description = "是否可作为采购依据（执行中且未过期）")
    private Boolean usableAsPurchaseBasis;

    @Schema(description = "计划总量")
    private BigDecimal totalQuantity;

    @Schema(description = "计划总金额")
    private BigDecimal totalAmount;

    @Schema(description = "暂停原因")
    private String suspendReason;

    @Schema(description = "暂停时间")
    private LocalDateTime suspendedTime;

    @Schema(description = "完成时间")
    private LocalDateTime completedTime;

    @Schema(description = "关闭人")
    private Long closedBy;

    @Schema(description = "关闭时间")
    private LocalDateTime closedTime;

    @Schema(description = "关闭原因")
    private String closeReason;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "品类明细")
    private List<PurchaseOrderItemRespVO> items;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
