package cn.iocoder.yudao.module.icbc.service.onboarding;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.PayeeOnboardingOutcomeEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyParser;
import cn.iocoder.yudao.module.icbc.service.callback.handler.FaceVerifyNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.callback.handler.PayeeOnboardingNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.callback.impl.CallbackNotifyServiceImpl;
import cn.iocoder.yudao.module.icbc.service.onboarding.impl.SellerOnboardingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 出售者建档两类回调走同一入口的端到端测试。
 *
 * <p>报文没有 {@code notifyType}，由解析器推断类型后分发到对应处理器，最终回写档案。
 * 这条链路证明「先落表后处理、状态机异步收敛」在真实入口上是通的。
 */
@Import({CallbackNotifyServiceImpl.class, IcbcNotifyParser.class,
        FaceVerifyNotifyHandler.class, PayeeOnboardingNotifyHandler.class,
        SellerOnboardingServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class SellerOnboardingNotifyTest extends BaseDbUnitTest {

    @Resource
    private CallbackNotifyServiceImpl callbackNotifyService;

    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @MockBean
    private IcbcGateway icbcGateway;

    @Test
    public void testFaceVerifyCallbackUpdatesPayee() {
        PayeeInfoDO payee = insertPayee("USER_FACE_N", "110101199001010041", "13800000041");

        String result = callbackNotifyService.receive(
                "{\"appId\":\"A\",\"transNode\":\"N1\",\"outUserId\":\"USER_FACE_N\",\"verifyResult\":\"1\"}");

        assertEquals("SUCCESS", result);
        assertEquals(PayeeRealNameStatusEnum.PASSED.getStatus(),
                payeeInfoMapper.selectById(payee.getId()).getRealNameStatus());
    }

    @Test
    public void testOnboardingCallbackAdvancesState() {
        PayeeInfoDO payee = insertPayee("USER_ONB_N", "110101199001010042", "13800000042");

        String result = callbackNotifyService.receive(
                "{\"appId\":\"A\",\"outUserId\":\"USER_ONB_N\",\"result\":\"pass\","
                        + "\"openacctStatus\":\"02\",\"mediumId\":\"MEDIUM_N\"}");

        assertEquals("SUCCESS", result);
        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals(PayeeOnboardingOutcomeEnum.READY.getCode(), updated.getOnboardingState());
        assertEquals("MEDIUM_N", updated.getIcbcMediumId());
    }

    private PayeeInfoDO insertPayee(String partnerPayeeId, String idCardNo, String mobile) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId(partnerPayeeId)
                .name("张三")
                .idCardNo(idCardNo)
                .mobile(mobile)
                .bankCardNo("6222021234567890")
                .realNameStatus(PayeeRealNameStatusEnum.NOT_STARTED.getStatus())
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

}
