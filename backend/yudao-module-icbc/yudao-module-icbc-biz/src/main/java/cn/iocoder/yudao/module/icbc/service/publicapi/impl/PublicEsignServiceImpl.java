package cn.iocoder.yudao.module.icbc.service.publicapi.impl;

import cn.iocoder.yudao.module.icbc.util.PublicTenantCall;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicAgreementSignRespVO;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.service.esign.FrameworkAgreementEsignService;
import cn.iocoder.yudao.module.icbc.service.publicapi.PublicEsignService;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenPayload;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 公开端点 - 合同组签署实现（#95）。
 *
 * <p>与其它公开端点同一套做法：先用令牌解析出租户与业务单，再显式切到该租户下执行，
 * 把多租户隔离补回来。**链接生成成功后才占用令牌次数**——一次失败的「去签署」不该
 * 烧掉本人一次打开机会（照发票下载的做法）。
 */
@Service
@Validated
public class PublicEsignServiceImpl implements PublicEsignService {

    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private FrameworkAgreementEsignService frameworkAgreementEsignService;

    @Override
    public PublicAgreementSignRespVO createSignUrl(String token) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.ONBOARDING);
        Long payeeId = Long.valueOf(payload.getBusinessKey());
        String signUrl = PublicTenantCall.execute(payload.getTenantId(),
                () -> frameworkAgreementEsignService.createSignUrl(payeeId));
        publicTokenService.consume(payload);
        PublicAgreementSignRespVO resp = new PublicAgreementSignRespVO();
        resp.setSignUrl(signUrl);
        return resp;
    }

}
