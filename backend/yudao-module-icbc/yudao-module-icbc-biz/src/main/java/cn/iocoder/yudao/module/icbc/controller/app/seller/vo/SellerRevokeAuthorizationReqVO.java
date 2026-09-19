package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 自然人端 - 自助撤销企业授权请求。
 */
@Schema(description = "自然人端 - 自助撤销企业授权请求")
@Data
public class SellerRevokeAuthorizationReqVO {

    @Schema(description = "自然人主体编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "自然人主体编号不能为空")
    private Long naturalPersonId;

    @Schema(description = "租户编号（要撤销哪一家回收企业的授权）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "租户编号不能为空")
    private Long tenantId;

    @Schema(description = "撤销原因（选填）")
    private String reason;

}
