package cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公开端点 - 出售者建档状态 Response VO。
 *
 * <p>自然人在工行页面办完后回到我们的小程序 / H5，用它刷新结果；后端顺带向工行查询一次
 * 收敛状态（实名结果 / 收方入驻结果）。
 */
@Schema(description = "公开端点 - 出售者建档状态 Response VO")
@Data
public class PublicOnboardingStatusRespVO {

    @Schema(description = "当前步骤：REAL_NAME / ONBOARDING / DONE", example = "ONBOARDING")
    private String step;

    @Schema(description = "实名认证状态名", example = "认证通过")
    private String realNameStatusName;

    @Schema(description = "收方入驻结果名", example = "入驻成功")
    private String onboardingStateName;

    @Schema(description = "下一步该做什么")
    private String nextStep;

    @Schema(description = "是否可用于开票")
    private Boolean invoiceEligible;

    @Schema(description = "给自然人看的一句话说明")
    private String message;

}
