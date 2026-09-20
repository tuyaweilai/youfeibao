package cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 承运商新增/修改 Request VO")
@Data
public class LogisticsCarrierSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "承运商名称（租户内唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "某某物流")
    @NotEmpty(message = "承运商名称不能为空")
    private String name;

    @Schema(description = "联系人", example = "王经理")
    private String contactName;

    @Schema(description = "联系电话", example = "13800138000")
    private String contactMobile;

    @Schema(description = "状态：0-合作中，1-已停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
