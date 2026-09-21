package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 自然人端 - 取「去签署」用的公开令牌（#95）。
 *
 * <p>公开的 {@code /icbc/public/agreement/sign-url} 只认一次性令牌（本人点击时现生成现用，
 * 不通过短信发）。登录态下的本人端先用本请求换一枚绑定收方的 {@code ONBOARDING} 令牌，
 * 再拿它调公开端点换第三方签署链接。
 */
@Schema(description = "自然人端 - 取去签署用的公开令牌请求")
@Data
public class SellerAgreementSignTokenReqVO {

    @Schema(description = "自然人主体编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "自然人主体编号不能为空")
    private Long naturalPersonId;

    @Schema(description = "收方（出售者）档案编号（待签协议挂在它名下）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "收方档案编号不能为空")
    private Long payeeId;

}
