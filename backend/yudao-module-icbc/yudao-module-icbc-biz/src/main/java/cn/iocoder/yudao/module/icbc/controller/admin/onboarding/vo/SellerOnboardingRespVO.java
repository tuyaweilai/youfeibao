package cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 出售者建档总览：状态机 + 协议 + 授权 + 能否开票
 */
@Schema(description = "管理后台 - 出售者建档总览")
@Data
public class SellerOnboardingRespVO {

    @Schema(description = "出售者（收方）编号")
    private Long payeeId;

    @Schema(description = "自然人主体编号（平台级身份，跨企业复用）")
    private Long naturalPersonId;

    @Schema(description = "平台级外部用户编号（工行 outUserId）")
    private String outUserId;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "身份证号")
    private String idCardNo;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "实人认证状态：0-未认证，1-认证中，2-认证通过，3-认证未通过")
    private Integer realNameStatus;

    @Schema(description = "实人认证状态名")
    private String realNameStatusName;

    @Schema(description = "实人认证失败原因")
    private String realNameMsg;

    @Schema(description = "收方入驻结果：READY / REJECTED / OPENACCT_FAILED / FAILED_AND_REJECTED")
    private String onboardingState;

    @Schema(description = "收方入驻结果名")
    private String onboardingStateName;

    @Schema(description = "下一步该做什么")
    private String nextStep;

    @Schema(description = "收方审核结果：pass / reject")
    private String auditResult;

    @Schema(description = "审核拒绝原因")
    private String rejectReason;

    @Schema(description = "审核状态：0-待审核，1-审核通过，2-审核拒绝")
    private Integer status;

    @Schema(description = "工行侧开户状态（openacctStatus，原样透传）")
    private String icbcOpenacctStatus;

    @Schema(description = "工行收方状态：0-不可用，1-可用")
    private String icbcReceiverStatus;

    @Schema(description = "工行账户标识（mediumId）")
    private String icbcMediumId;

    @Schema(description = "有效框架收购协议（未签署时为空）")
    private FrameworkAgreementRespVO frameworkAgreement;

    @Schema(description = "首次授权（未授权时为空）")
    private SellerAuthorizationRespVO authorization;

    @Schema(description = "是否可用于开票")
    private Boolean invoiceEligible;

    @Schema(description = "不可开票的原因（可开票时为空）")
    private String invoiceBlockReason;

    @Schema(description = "历史协议")
    private List<FrameworkAgreementRespVO> agreementHistory;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
