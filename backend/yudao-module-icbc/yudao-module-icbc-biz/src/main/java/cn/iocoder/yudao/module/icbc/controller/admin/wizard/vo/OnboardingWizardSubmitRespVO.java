package cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 建档向导 - 提交结果。
 *
 * <p>向导走完交给现场两件事：**收方档案编号**（本人后续实名 / 进度都按它走）与本次的
 * **签署方式**（未开通电子签章时是 {@code PAPER}，向导照常走完）。
 * 二维码与可复制链接由前端用既有的公开令牌机制生成（{@code /icbc/public-token/create}），
 * 后端不额外造一条触达通道（ADR 0023）。
 */
@Schema(description = "管理后台 - 建档向导：提交结果")
@Data
@Builder
public class OnboardingWizardSubmitRespVO {

    @Schema(description = "收方档案编号", example = "1024")
    private Long payeeId;

    @Schema(description = "自然人主体编号（平台级身份，跨企业复用）", example = "2048")
    private Long naturalPersonId;

    @Schema(description = "框架收购协议编号", example = "4096")
    private Long agreementId;

    @Schema(description = "框架收购协议签署方式：ELECTRONIC-电子签章，PAPER-纸质签署", example = "PAPER")
    private String signMethod;

}
