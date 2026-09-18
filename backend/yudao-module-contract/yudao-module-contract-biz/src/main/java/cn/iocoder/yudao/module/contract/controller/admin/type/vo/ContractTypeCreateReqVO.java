package cn.iocoder.yudao.module.contract.controller.admin.type.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 合同类型创建 Request VO")
@Data
public class ContractTypeCreateReqVO {

    @Schema(description = "类型编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PURCHASE")
    @NotBlank(message = "类型编码不能为空")
    private String code;

    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "采购合同")
    @NotBlank(message = "类型名称不能为空")
    private String name;

    @Schema(description = "描述", example = "用于采购相关的合同")
    private String description;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

} 