package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 报送代办税费报告表 Request VO")
@Data
public class TaxDeclarationDeclareReqVO {

    @Schema(description = "申报月 yyyy-MM", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08")
    @NotBlank(message = "申报月不能为空")
    private String periodMonth;

    @Schema(description = "申报人", example = "财务小李")
    private String declaredBy;

    @Schema(description = "申报备注", example = "已通过电子税务局报送《代办税费报告表》《代办税费明细报告表》")
    private String remark;

}
