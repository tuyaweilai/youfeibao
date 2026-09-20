package cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 管理后台 - 进项发票勾稽 Request VO（#49 T11）。
 *
 * <p>勾稽到哪张单据由调用方指明：{@code bizType} 是稳定编码（收购单 / 采购订单 / 入库单），
 * {@code bizId} + {@code bizNo} 定位单据，{@code bizAmount} 是调用方给出的单据金额——
 * 本模块不 import 采购订单 / 入库单的类，金额上限只认这个值（第二轮并行约定）。
 */
@Schema(description = "管理后台 - 进项发票勾稽 Request VO")
@Data
public class InputInvoiceLinkReqVO {

    @Schema(description = "进项发票编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "进项发票编号不能为空")
    private Long invoiceId;

    @Schema(description = "单据类型：ACQUISITION-收购单，PURCHASE_ORDER-采购订单，STOCK_IN-入库单",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String bizType;

    @Schema(description = "单据编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "单据编号不能为空")
    private Long bizId;

    @Schema(description = "单据号快照（如采购订单号）")
    private String bizNo;

    @Schema(description = "单据金额（勾稽金额上限）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "单据金额不能为空")
    private BigDecimal bizAmount;

    @Schema(description = "本次勾稽金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "勾稽金额不能为空")
    private BigDecimal linkedAmount;

    @Schema(description = "备注")
    private String remark;

}
