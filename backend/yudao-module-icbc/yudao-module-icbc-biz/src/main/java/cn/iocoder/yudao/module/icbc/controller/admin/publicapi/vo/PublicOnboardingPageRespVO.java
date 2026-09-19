package cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公开端点 - 出售者建档页面 Response VO。
 *
 * <p>自然人凭 ONBOARDING 令牌取回「当前该做的工行页面」：实名认证或收方入驻。
 * {@code formHtml} 是工行 UI 页面接口返回的自动提交表单，前端只负责承载，不拼工行 URL（ADR 0009）。
 */
@Schema(description = "公开端点 - 出售者建档页面 Response VO")
@Data
public class PublicOnboardingPageRespVO {

    @Schema(description = "出售者（收方）编号", example = "1024")
    private Long payeeId;

    @Schema(description = "当前步骤：REAL_NAME-实名认证，ONBOARDING-收方入驻，DONE-无需再办", example = "REAL_NAME")
    private String step;

    @Schema(description = "步骤名", example = "实名认证")
    private String stepName;

    @Schema(description = "工行页面自动提交表单 HTML；DONE 时为空")
    private String formHtml;

    @Schema(description = "给自然人看的一句话说明")
    private String message;

}
