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

    @Schema(description = "实名认证状态值（0 未认证 / 1 认证中 / 2 认证通过 / 3 认证未通过）", example = "2")
    private Integer realNameStatus;

    @Schema(description = "实名未通过的原因（未通过时非空，落点页展示并给重试入口）", example = "人脸比对不通过")
    private String realNameMsg;

    @Schema(description = "收方入驻结果名", example = "入驻成功")
    private String onboardingStateName;

    @Schema(description = "收款账户变更状态名（换卡在途时非空，例如「银行审核中」）", example = "银行审核中")
    private String bankCardChangeStatusName;

    @Schema(description = "下一步该做什么")
    private String nextStep;

    @Schema(description = "是否可用于开票")
    private Boolean invoiceEligible;

    @Schema(description = "是否有待签署的电子框架收购协议：为 true 时落点页要给出可点的「去签署」"
            + "（#95：向导第 5 步转达的链接落在这页，入口必须在这里，否则本人做完实名就断在那儿）",
            example = "true")
    private Boolean pendingAgreement;

    @Schema(description = "待签署协议编号（pendingAgreement 为 true 时非空，供页面指认是哪一份）")
    private String pendingAgreementNo;

    @Schema(description = "给自然人看的一句话说明")
    private String message;

}
