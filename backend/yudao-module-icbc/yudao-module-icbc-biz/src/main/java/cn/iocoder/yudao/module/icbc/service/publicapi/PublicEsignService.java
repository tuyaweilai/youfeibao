package cn.iocoder.yudao.module.icbc.service.publicapi;

import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicAgreementSignRespVO;

/**
 * 公开端点 - 合同组签署（#95，ADR 0036）。
 *
 * <p>自然人在自己手机上点「去签署」时，用建档令牌换一枚**现生成现用**的签署链接并直接跳转；
 * 不由后端发短信、不缓存复用（ADR 0023 / 0036）。
 */
public interface PublicEsignService {

    /**
     * 用建档令牌为本人现生成一枚合同组签署链接。
     *
     * @param token 公开令牌（用途 ONBOARDING，绑定收方）
     * @return 一次性签署链接
     */
    PublicAgreementSignRespVO createSignUrl(String token);

}
