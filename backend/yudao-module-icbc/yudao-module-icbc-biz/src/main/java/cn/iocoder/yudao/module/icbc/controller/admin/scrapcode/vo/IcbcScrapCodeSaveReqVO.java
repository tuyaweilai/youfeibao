package cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 平台级报废产品编码新增/修改 Request VO")
@Data
public class IcbcScrapCodeSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "报废产品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "报废产品名称不能为空")
    private String name;

    @Schema(description = "商品和服务税收分类合并编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "税收分类合并编码不能为空")
    private String mergedCode;

    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "税率")
    private BigDecimal taxRate;

    @Schema(description = "状态：0-启用，1-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
