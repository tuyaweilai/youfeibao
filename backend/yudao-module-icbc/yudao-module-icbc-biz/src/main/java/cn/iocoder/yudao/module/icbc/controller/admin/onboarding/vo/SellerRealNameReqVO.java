package cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 发起实人认证请求
 */
@Schema(description = "管理后台 - 发起出售者实人认证请求")
@Data
public class SellerRealNameReqVO {

    @Schema(description = "出售者（收方）编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "出售者编号不能为空")
    private Long payeeId;

}
