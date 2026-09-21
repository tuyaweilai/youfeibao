package cn.iocoder.yudao.module.icbc.service.onboarding;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo.SellerRealNameReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.IcbcStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.service.esign.FrameworkAgreementEsignService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.onboarding.impl.SellerOnboardingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.SELLER_APP_ENTRY_NOT_CONFIGURED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 「入口地址缺配置就别发起实名」的单元测试（#82）。
 *
 * <p>单独一个类，是因为它要的是**没配**入口地址的环境；同包的
 * {@link SellerOnboardingServiceImplTest} 为了验证跳转地址本身，必须配上一个。
 *
 * <p>以前这里会静默把空跳转地址带给工行（而工行把两个跳转地址标为必输），
 * 用户签完只能自己关窗口、再让人手动点「查询结果」。
 */
@Import({SellerOnboardingServiceImpl.class, UnitTestConfiguration.class})
@TestPropertySource(properties = {
        "icbc.public-token.secret=test-public-token-secret-0123456789abcdef"
        // 故意不配 icbc.notify.seller-app-url 与 icbc.station.entry-url
})
@Transactional
@Rollback
public class SellerOnboardingJumpUrlRequiredTest extends BaseDbUnitTest {

    @Resource
    private SellerOnboardingService sellerOnboardingService;
    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @MockBean
    private IcbcGateway icbcGateway;

    /** 协议落库会注入电子签（#95）；本测试只测实名入口缺配置时的报错，置空。 */
    @MockBean
    private FrameworkAgreementEsignService frameworkAgreementEsignService;

    @Test
    public void testStartRealName_failsClearlyWhenNoSellerAppEntryConfigured() {
        SellerRealNameReqVO reqVO = new SellerRealNameReqVO();
        reqVO.setPayeeId(insertPayee().getId());

        // 缺配置 = 办不成：当场报明确错误，而不是带着空跳转地址去求工行
        assertServiceException(() -> sellerOnboardingService.startRealName(reqVO),
                SELLER_APP_ENTRY_NOT_CONFIGURED);
        verify(icbcGateway, never()).submitFaceVerification(any());
    }

    private PayeeInfoDO insertPayee() {
        NaturalPersonRegisterReqVO personReq = new NaturalPersonRegisterReqVO();
        personReq.setName("张三");
        personReq.setIdCardNo("110101199001010088");
        personReq.setMobile("13800000088");
        IcbcNaturalPersonDO person = naturalPersonService.register(personReq);

        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId("USER_NO_APP_URL")
                .naturalPersonId(person.getId())
                .name(person.getName())
                .idCardNo(person.getIdCardNo())
                .mobile(person.getMobile())
                .bankCardNo("6222021234567890")
                .businessType("RECYCLE")
                .status(IcbcStatusEnum.AuditStatus.PENDING.getStatus())
                .realNameStatus(PayeeRealNameStatusEnum.NOT_STARTED.getStatus())
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

}
