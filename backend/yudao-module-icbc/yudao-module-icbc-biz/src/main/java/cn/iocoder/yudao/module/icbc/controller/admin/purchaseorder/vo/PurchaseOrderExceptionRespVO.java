package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理后台 - 履约异常授权单 Response VO（#47 T09）。
 */
@Schema(description = "管理后台 - 履约异常授权单 Response VO")
@Data
public class PurchaseOrderExceptionRespVO {

    @Schema(description = "授权单编号")
    private Long id;

    @Schema(description = "授权单号")
    private String exceptionNo;

    @Schema(description = "采购订单编号")
    private Long orderId;

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "采购订单明细编号")
    private Long itemId;

    @Schema(description = "品类名称")
    private String categoryName;

    @Schema(description = "异常类型")
    private String exceptionType;

    @Schema(description = "异常类型名")
    private String exceptionTypeName;

    @Schema(description = "异常口径说明")
    private String exceptionTypeDefinition;

    @Schema(description = "本次交货场站编号")
    private Long stationId;

    @Schema(description = "本次交货场站名称")
    private String stationName;

    @Schema(description = "本次申请的交货量")
    private BigDecimal requestedQuantity;

    @Schema(description = "审核通过的授权追加量（超量交货）")
    private BigDecimal approvedQuantity;

    @Schema(description = "授权有效期止")
    private LocalDate validUntil;

    @Schema(description = "提交原因")
    private String reason;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态名")
    private String statusName;

    @Schema(description = "提交人")
    private Long requestedBy;

    @Schema(description = "提交时间")
    private LocalDateTime requestedTime;

    @Schema(description = "审核人")
    private Long reviewedBy;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedTime;

    @Schema(description = "审核意见")
    private String reviewRemark;

    @Schema(description = "授权现在是否仍然生效（已通过 + 在有效期内）")
    private Boolean effective;

    @Schema(description = "口径说明：这条授权放宽了什么")
    private String scopeNote;

}
