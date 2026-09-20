package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 管理后台 - 收购接收结论 Request VO（#53 T15）。
 *
 * <p>验收结论可以是**接收 / 部分接收 / 拒收**：接收量留下、退回量与余货出场量离场。
 * 拒收部分不形成采购应付、不进正常库存，但仍在收购单上可追溯（ADR 0028）。
 */
@Schema(description = "管理后台 - 收购接收结论 Request VO")
@Data
public class AcquisitionAcceptanceReqVO {

    @Schema(description = "收购单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "收购单编号不能为空")
    private Long id;

    @Schema(description = "接收量（实际留下 / 进库存的重量）；可为 0（全部拒收）", requiredMode = Schema.RequiredMode.REQUIRED, example = "11500.00")
    @NotNull(message = "接收量不能为空（全部拒收请填 0）")
    private BigDecimal acceptedWeight;

    @Schema(description = "退回量（因质量 / 规格不合格退回出售者的重量）", example = "500.00")
    private BigDecimal rejectedWeight;

    @Schema(description = "余货出场量（未接收、随车带离场站的余货重量）", example = "500.00")
    private BigDecimal residualWeight;

    @Schema(description = "拒收原因；退回量大于 0 时必填", example = "含水率超标，杂质过多")
    private String rejectReason;

    @Schema(description = "备注", example = "现场与出售者当面复磅确认")
    private String remark;

}
