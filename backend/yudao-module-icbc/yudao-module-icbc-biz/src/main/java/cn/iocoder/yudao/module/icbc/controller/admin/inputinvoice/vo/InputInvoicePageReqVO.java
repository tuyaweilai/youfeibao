package cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

/**
 * 管理后台 - 进项发票分页 Request VO（#49 T11）。
 */
@Schema(description = "管理后台 - 进项发票分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InputInvoicePageReqVO extends PageParam {

    @Schema(description = "发票号码（模糊）")
    private String invoiceNo;

    @Schema(description = "销方名称（模糊）")
    private String sellerName;

    @Schema(description = "销方纳税人识别号")
    private String sellerTaxNo;

    @Schema(description = "票种：1-专用发票，2-普通发票")
    private Integer invoiceType;

    @Schema(description = "状态：0-已登记，1-部分勾稽，2-已勾稽")
    private Integer status;

    @Schema(description = "开票日期起")
    private LocalDate invoiceDateStart;

    @Schema(description = "开票日期止")
    private LocalDate invoiceDateEnd;

}
