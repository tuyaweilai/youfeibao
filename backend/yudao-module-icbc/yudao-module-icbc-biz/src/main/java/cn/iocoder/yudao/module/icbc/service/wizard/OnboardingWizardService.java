package cn.iocoder.yudao.module.icbc.service.wizard;

import cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo.*;

import javax.validation.Valid;

/**
 * 建档向导 Service（现场端五步壳，见 #91）。
 *
 * <p>向导把一个人的**建档**拆成五步：拍身份证正反面 → 确认识别结果（可改）→ 拍银行卡 →
 * 确认卡信息（可改）→ 签署框架收购协议。分步状态**只在现场端本地暂存**，后端只提供两件事：
 * <ul>
 *   <li><b>识别</b>：无状态的上传 + 识别，图片识别完即弃、不进文件服务（ADR 0037）；</li>
 *   <li><b>落库</b>：第 4 步确认后一次性写收方档案（必要时登记 / 复用自然人主体）与框架收购协议。</li>
 * </ul>
 *
 * <p>本接口不新增门禁：完成与否仍由既有要件判定（实名通过 / 入驻 READY / 生效协议 / 两项授权）。
 */
public interface OnboardingWizardService {

    /**
     * 识别身份证人像面：姓名 / 证件号 / 住址**只在空缺处回填**，人工输入的值优先。
     *
     * @param reqVO 图片与当前表单值
     * @return 合并后的值 + 质量分 / 告警 / 硬拦原因
     */
    IdCardFrontRecognizeRespVO recognizeIdCardFront(@Valid IdCardFrontRecognizeReqVO reqVO);

    /**
     * 识别身份证国徽面：证件签发日期与证件有效期**只在空缺处回填**。
     */
    IdCardBackRecognizeRespVO recognizeIdCardBack(@Valid IdCardBackRecognizeReqVO reqVO);

    /**
     * 识别银行卡：卡号 / 开户行 / 是否我行卡**只在空缺处回填**。
     */
    BankCardRecognizeRespVO recognizeBankCard(@Valid BankCardRecognizeReqVO reqVO);

    /**
     * 第 4 步确认后一次性落库：登记 / 复用自然人主体、写收方档案、把框架收购协议落成
     * 电子签或纸质签（以 {@code EsignPort.isAvailable} 为唯一判据，未开通即 {@code PAPER}）。
     *
     * <p>电子签方式会在同一事务里发起**合同组签署**（框架收购协议 + 反向发票合规告知函，
     * 企业先盖章、自然人后签署），协议落「待签署」，签完靠回调推到生效（#95 / ADR 0036）；
     * 拿不到合同组任务号则整体回滚，不留拿不到签署链接的半成品。
     *
     * @return 收方档案编号、自然人主体编号、协议编号与本次的签署方式 / 协议状态 / 可读说明
     */
    OnboardingWizardSubmitRespVO submit(@Valid OnboardingWizardSubmitReqVO reqVO);

}
