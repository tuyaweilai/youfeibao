package cn.iocoder.yudao.module.contract.controller.admin.type.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同类型 Response VO")
@Data
public class ContractTypeRespVO {

    @Schema(description = "类型ID", example = "1")
    private Long id;

    @Schema(description = "类型编码", example = "PURCHASE")
    private String code;

    @Schema(description = "类型名称", example = "采购合同")
    private String name;

    @Schema(description = "描述", example = "用于采购相关的合同")
    private String description;

    @Schema(description = "是否系统预定义", example = "false")
    private Boolean systemDefined;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

} 