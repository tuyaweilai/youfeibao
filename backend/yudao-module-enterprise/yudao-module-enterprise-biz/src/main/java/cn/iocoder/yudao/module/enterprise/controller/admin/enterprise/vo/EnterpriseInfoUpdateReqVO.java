package cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

/**
 * 管理后台 - 企业信息更新 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业信息更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseInfoUpdateReqVO extends EnterpriseInfoBaseVO {

    @Schema(description = "企业ID", required = true, example = "1024")
    @NotNull(message = "企业ID不能为空")
    private Long id;

} 