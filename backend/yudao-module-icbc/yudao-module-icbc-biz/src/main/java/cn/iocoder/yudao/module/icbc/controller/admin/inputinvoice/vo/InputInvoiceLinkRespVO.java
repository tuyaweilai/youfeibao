package cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 进项发票勾稽记录 Response VO（#49 T11）。
 */
@Schema(description = "管理后台 - 进项发票勾稽记录 Response VO")
@Data
public class InputInvoiceLinkRespVO {

    @Schema(description = "勾稽记录编号")
    private Long id;

    @Schema(description = "进项发票编号")
    private Long invoiceId;

    @Schema(description = "单据类型编码")
    private String bizType;

    @Schema(description = "单据类型名称")
    private String bizTypeName;

    @Schema(description = "单据编号")
    private Long bizId;

    @Schema(description = "单据号快照")
    private String bizNo;

    @Schema(description = "单据金额")
    private BigDecimal bizAmount;

    @Schema(description = "本次勾稽金额")
    private BigDecimal linkedAmount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
