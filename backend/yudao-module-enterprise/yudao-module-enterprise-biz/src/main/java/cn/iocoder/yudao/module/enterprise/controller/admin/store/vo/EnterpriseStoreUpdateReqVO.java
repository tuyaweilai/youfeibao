package cn.iocoder.yudao.module.enterprise.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

/**
 * 管理后台 - 企业门店更新 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业门店更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseStoreUpdateReqVO extends EnterpriseStoreBaseVO {

    @Schema(description = "门店ID", required = true, example = "1024")
    @NotNull(message = "门店ID不能为空")
    private Long id;

} 