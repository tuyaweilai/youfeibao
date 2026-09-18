package cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 品类与税收分类编码配置新增/修改 Request VO")
@Data
public class IcbcGoodsConfigSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "品类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "品类名称不能为空")
    private String name;

    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "税率")
    private BigDecimal taxRate;

    @Schema(description = "商品和服务税收分类合并编码")
    private String mergedCode;

    @Schema(description = "状态：0-启用，1-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
