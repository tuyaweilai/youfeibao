package cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

/**
 * 管理后台 - 企业资质更新 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业资质更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseQualificationUpdateReqVO extends EnterpriseQualificationBaseVO {

    @Schema(description = "资质ID", required = true, example = "1024")
    @NotNull(message = "资质ID不能为空")
    private Long id;

} 