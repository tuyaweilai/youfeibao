package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 补缴缴款 Request VO")
@Data
public class TaxSupplementPayReqVO {

    @Schema(description = "补缴记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "补缴记录编号不能为空")
    private Long id;

    @Schema(description = "实缴金额（元）；为空时取应补缴金额", example = "400.00")
    private BigDecimal paidAmount;

    @Schema(description = "缴款时间；为空时取当前时间")
    private LocalDateTime paidAt;

    @Schema(description = "缴款凭证号", example = "2026081500005678")
    private String voucherNo;

    @Schema(description = "缴款凭证文件地址")
    private String voucherFileUrl;

    @Schema(description = "备注")
    private String remark;

}
