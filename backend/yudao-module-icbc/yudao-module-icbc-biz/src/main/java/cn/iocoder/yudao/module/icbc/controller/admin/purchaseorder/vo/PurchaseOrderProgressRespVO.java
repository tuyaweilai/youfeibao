package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 采购订单执行进度 Response VO（#46 T08，AC「执行进度入口」）。
 *
 * <p>本票只给**计划 / 已收**两个口径（已收 = 成交记录数量之和），把「执行进度入口」立起来；
 * 计划 / 验收 / 入库 / 结算 / 未履行五个口径分列属 #47（T09），到时在这里扩，不另起一套。
 */
@Schema(description = "管理后台 - 采购订单执行进度 Response VO")
@Data
public class PurchaseOrderProgressRespVO {

    @Schema(description = "采购订单编号")
    private Long orderId;

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "状态名")
    private String statusName;

    @Schema(description = "计划总量")
    private BigDecimal totalQuantity;

    @Schema(description = "已收总量")
    private BigDecimal receivedQuantity;

    @Schema(description = "未收总量（计划 − 已收，可为负表示超收）")
    private BigDecimal remainingQuantity;

    @Schema(description = "计划总金额")
    private BigDecimal totalAmount;

    @Schema(description = "口径说明（本票只有计划 / 已收，五口径见 #47）")
    private String scopeNote;

    @Schema(description = "逐条明细的进度")
    private List<ItemProgress> items;

    /**
     * 单条明细的进度。
     */
    @Schema(description = "采购订单明细进度")
    @Data
    public static class ItemProgress {

        @Schema(description = "明细编号")
        private Long itemId;

        @Schema(description = "品类名称")
        private String categoryName;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "计划量")
        private BigDecimal quantity;

        @Schema(description = "已收量")
        private BigDecimal receivedQuantity;

        @Schema(description = "未收量")
        private BigDecimal remainingQuantity;

        @Schema(description = "成交笔数")
        private Integer dealCount;
    }

}
