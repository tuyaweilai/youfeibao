package cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 - 进项发票 Response VO（#49 T11）。
 *
 * <p>{@link #remainingAmount} 是推导值：价税合计 − 已勾稽合计，即还能勾稽多少。
 */
@Schema(description = "管理后台 - 进项发票 Response VO")
@Data
public class InputInvoiceRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "发票号码")
    private String invoiceNo;

    @Schema(description = "发票代码")
    private String invoiceCode;

    @Schema(description = "票种：1-专用发票，2-普通发票")
    private Integer invoiceType;

    @Schema(description = "票种名称")
    private String invoiceTypeName;

    @Schema(description = "开票日期")
    private LocalDate invoiceDate;

    @Schema(description = "销方名称")
    private String sellerName;

    @Schema(description = "销方纳税人识别号")
    private String sellerTaxNo;

    @Schema(description = "不含税金额")
    private BigDecimal amount;

    @Schema(description = "税额")
    private BigDecimal taxAmount;

    @Schema(description = "价税合计")
    private BigDecimal totalAmount;

    @Schema(description = "已勾稽金额")
    private BigDecimal linkedAmount;

    @Schema(description = "可勾稽余额（价税合计 − 已勾稽金额）")
    private BigDecimal remainingAmount;

    @Schema(description = "状态：0-已登记，1-部分勾稽，2-已勾稽")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "勾稽记录")
    private List<InputInvoiceLinkRespVO> links;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
