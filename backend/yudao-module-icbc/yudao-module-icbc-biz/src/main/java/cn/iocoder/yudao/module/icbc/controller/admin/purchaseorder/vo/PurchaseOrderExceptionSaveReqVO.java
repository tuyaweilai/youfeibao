package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 管理后台 - 提交履约异常授权审核 Request VO（#47 T09）。
 *
 * <p>超量 / 过期 / 跨场站交货被企业配置成「提交授权审核」时，由现场或采购经办在这里提交一张授权单。
 * 审核通过后才按授权范围放行。
 */
@Schema(description = "管理后台 - 提交履约异常授权审核 Request VO")
@Data
public class PurchaseOrderExceptionSaveReqVO {

    @Schema(description = "采购订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购订单编号不能为空")
    private Long orderId;

    @Schema(description = "采购订单明细编号（超量交货必填）")
    private Long itemId;

    @Schema(description = "异常类型：OVER_QUANTITY / EXPIRED / CROSS_STATION",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "异常类型不能为空")
    private String exceptionType;

    @Schema(description = "本次交货场站编号（跨场站交货必填）")
    private Long stationId;

    @Schema(description = "本次申请的交货量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "本次申请的交货量不能为空")
    private BigDecimal requestedQuantity;

    @Schema(description = "期望的授权有效期止（可空，空表示不设有效期）")
    private LocalDate validUntil;

    @Schema(description = "提交原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "提交原因不能为空")
    private String reason;

}
