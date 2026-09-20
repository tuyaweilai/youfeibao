package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理后台 - 采购订单状态流转 Request VO（#46 T08，用户故事 19）。
 *
 * <p>目标状态见 {@code PurchaseOrderStatusEnum}：草稿 → 执行中 ⇄ 暂停 → 完成 → 关闭。
 * 非法流转由服务端拦截。暂停必填原因。
 */
@Schema(description = "管理后台 - 采购订单状态流转 Request VO")
@Data
public class PurchaseOrderStatusUpdateReqVO {

    @Schema(description = "采购订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购订单编号不能为空")
    private Long id;

    @Schema(description = "目标状态：1-执行中，2-暂停，3-完成，4-关闭", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "目标状态不能为空")
    private Integer status;

    @Schema(description = "原因（暂停 / 关闭时填，暂停必填）")
    private String reason;

}
