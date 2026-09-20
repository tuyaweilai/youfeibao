package cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 管理后台 - 进项发票登记 / 修改 Request VO（#49 T11）。
 *
 * <p>登记的是票面事实：票种、号码、金额、税额、开票日期与销方。{@code amount} 是不含税金额，
 * {@code taxAmount} 是税额，二者相加即价税合计（服务端计算，勾稽上限就是它）。
 */
@Schema(description = "管理后台 - 进项发票登记/修改 Request VO")
@Data
public class InputInvoiceSaveReqVO {

    @Schema(description = "主键（修改时必填）")
    private Long id;

    @Schema(description = "发票号码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "发票号码不能为空")
    private String invoiceNo;

    @Schema(description = "发票代码（数电票可空）")
    private String invoiceCode;

    @Schema(description = "票种：1-专用发票，2-普通发票", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "票种不能为空")
    private Integer invoiceType;

    @Schema(description = "开票日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开票日期不能为空")
    private LocalDate invoiceDate;

    @Schema(description = "销方名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "销方名称不能为空")
    private String sellerName;

    @Schema(description = "销方纳税人识别号（为空时按销方名称参与唯一性判定）")
    private String sellerTaxNo;

    @Schema(description = "不含税金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "不含税金额不能为空")
    @DecimalMin(value = "0", message = "不含税金额不能为负")
    private BigDecimal amount;

    @Schema(description = "税额（默认 0）")
    @DecimalMin(value = "0", message = "税额不能为负")
    private BigDecimal taxAmount;

    @Schema(description = "备注")
    private String remark;

}
