package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 代办税费缴款并归档凭证 Request VO")
@Data
public class TaxDeclarationPayReqVO {

    @Schema(description = "申报月 yyyy-MM", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08")
    @NotBlank(message = "申报月不能为空")
    private String periodMonth;

    @Schema(description = "实缴金额（元）；为空时取应缴合计", example = "1234.56")
    private BigDecimal paidAmount;

    @Schema(description = "缴款时间；为空时取当前时间")
    private LocalDateTime paidAt;

    @Schema(description = "缴纳方式", example = "电子税务局批量扣款")
    private String paymentMethod;

    @Schema(description = "缴款凭证号", example = "2026081500001234")
    private String voucherNo;

    @Schema(description = "缴款凭证文件地址")
    private String voucherFileUrl;

    @Schema(description = "备注")
    private String remark;

}
