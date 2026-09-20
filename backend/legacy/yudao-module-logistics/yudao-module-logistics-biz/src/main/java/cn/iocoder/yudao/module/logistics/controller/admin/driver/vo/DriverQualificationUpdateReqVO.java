package cn.iocoder.yudao.module.logistics.controller.admin.driver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 司机资质信息更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DriverQualificationUpdateReqVO extends DriverQualificationCreateReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "主键ID不能为空")
    private Long id;

} 