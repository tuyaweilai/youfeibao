package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 管理后台 - 审核履约异常授权单 Request VO（#47 T09）。
 *
 * <p>通过时可按类型收紧授权范围：超量交货给「追加量」，过期 / 跨场站交货给「有效期」。
 * 未填时按提交的内容生效（追加量默认取提交的交货量）。
 */
@Schema(description = "管理后台 - 审核履约异常授权单 Request VO")
@Data
public class PurchaseOrderExceptionReviewReqVO {

    @Schema(description = "授权单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "授权单编号不能为空")
    private Long id;

    @Schema(description = "是否通过：true-通过，false-拒绝", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审核结论不能为空")
    private Boolean approved;

    @Schema(description = "超量交货的授权追加量（不填按提交的交货量）")
    private BigDecimal approvedQuantity;

    @Schema(description = "授权有效期止（不填按提交的期望有效期）")
    private LocalDate validUntil;

    @Schema(description = "审核意见")
    private String reviewRemark;

}
