package cn.iocoder.yudao.module.logistics.controller.admin.temporaryorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 物流临时订单更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class TemporaryOrderUpdateReqVO extends TemporaryOrderCreateReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "主键ID不能为空")
    private Long id;

} 