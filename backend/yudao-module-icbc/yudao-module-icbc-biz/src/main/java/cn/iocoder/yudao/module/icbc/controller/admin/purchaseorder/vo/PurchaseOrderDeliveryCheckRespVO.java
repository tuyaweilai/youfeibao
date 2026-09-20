package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 校验一次交货是否被允许 Response VO（#47 T09）。
 *
 * <p>三件事分开说：这次交货有没有异常（{@code violations}）、每条异常被什么放宽了
 * （{@code resolvedByExceptionId}）、企业配置对这条异常是拦还是要求授权审核（{@code rule}）。
 * {@code allowed=false} 时 {@code resolution} 告诉调用方下一步是「改数量 / 换场站」还是「去提交授权审核」。
 */
@Schema(description = "管理后台 - 校验一次交货是否被允许 Response VO")
@Data
public class PurchaseOrderDeliveryCheckRespVO {

    @Schema(description = "采购订单编号")
    private Long orderId;

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "采购订单明细编号")
    private Long itemId;

    @Schema(description = "本次交货量")
    private BigDecimal quantity;

    @Schema(description = "是否允许登记这次交货（所有异常都被放行才算允许）")
    private Boolean allowed;

    @Schema(description = "结论：OK-允许，BLOCKED-被企业配置拦截，NEEDS_APPROVAL-需先提交授权审核，NOT_DELIVERABLE-订单状态不允许")
    private String resolution;

    @Schema(description = "结论名")
    private String resolutionName;

    @Schema(description = "逐条异常的校验结果")
    private List<Violation> violations;

    @Schema(description = "口径说明")
    private String scopeNote;

    /**
     * 单条异常的校验结果。
     */
    @Schema(description = "履约异常校验结果")
    @Data
    public static class Violation {

        @Schema(description = "异常类型：OVER_QUANTITY / EXPIRED / CROSS_STATION")
        private String exceptionType;

        @Schema(description = "异常类型名")
        private String exceptionTypeName;

        @Schema(description = "异常说明")
        private String message;

        @Schema(description = "企业配置的处理方式：BLOCK / APPROVAL")
        private String rule;

        @Schema(description = "处理方式名")
        private String ruleName;

        @Schema(description = "是否已被生效中的授权放行")
        private Boolean resolved;

        @Schema(description = "放行它的授权单编号（未放行时为空）")
        private Long resolvedByExceptionId;

        @Schema(description = "放行它的授权单号")
        private String resolvedByExceptionNo;

        @Schema(description = "已有待审核的授权申请编号（需先等它审完）")
        private Long pendingExceptionId;

        @Schema(description = "本次超量部分（仅超量交货）")
        private BigDecimal overageQuantity;
    }

}
