package cn.iocoder.yudao.module.icbc.service.onboarding;

import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.PayeeOnboardingOutcomeEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyParser;
import cn.iocoder.yudao.module.icbc.service.callback.handler.FaceVerifyNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.callback.handler.PayeeOnboardingNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.callback.impl.CallbackNotifyServiceImpl;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.onboarding.impl.SellerOnboardingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 出售者建档两类回调走同一入口的端到端测试。
 *
 * <p>报文没有 {@code notifyType}，由解析器推断类型后分发到对应处理器。实人认证回写**自然人主体**，
 * 收方入驻要靠报文里的 {@code appIdSub}（子商户 = 回收企业）定位到本租户的**收方档案**——
 * 这条链路证明「先落表后处理、状态机异步收敛」在真实入口上是通的。
 */
@Import({CallbackNotifyServiceImpl.class, IcbcNotifyParser.class,
        FaceVerifyNotifyHandler.class, PayeeOnboardingNotifyHandler.class,
        SellerOnboardingServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class SellerOnboardingNotifyTest extends BaseDbUnitTest {

    /** 库存域只通过 erp-api 的 StockApi 接入（#52）；单元测试不跨模块，用 Mock。 */
    @MockBean
    private StockApi stockApi;

    private static final String OUT_VENDOR_ID = "PAYER_SUB_NOTIFY";

    @Resource
    private CallbackNotifyServiceImpl callbackNotifyService;

    @Resource
    private NaturalPersonService naturalPersonService;

    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PayerInfoMapper payerInfoMapper;

    @MockBean
    private IcbcGateway icbcGateway;

    @Test
    public void testFaceVerifyCallbackUpdatesNaturalPerson() {
        PayeeInfoDO payee = insertPayee("USER_FACE_N", "110101199001010041", "13800000041");
        IcbcNaturalPersonDO person = personOf(payee);

        String result = callbackNotifyService.receive(
                "{\"appId\":\"A\",\"transNode\":\"N1\",\"outUserId\":\"" + person.getOutUserId()
                        + "\",\"verifyResult\":\"1\"}");

        assertEquals("SUCCESS", result);
        assertEquals(PayeeRealNameStatusEnum.PASSED.getStatus(),
                naturalPersonService.getNaturalPerson(person.getId()).getRealNameStatus());
    }

    @Test
    public void testOnboardingCallbackAdvancesStateOfOwnTenantPayee() {
        PayeeInfoDO payee = insertPayee("USER_ONB_N", "110101199001010042", "13800000042");
        IcbcNaturalPersonDO person = personOf(payee);
        insertPayer(OUT_VENDOR_ID);

        String result = callbackNotifyService.receive(
                "{\"appId\":\"A\",\"appIdSub\":\"" + OUT_VENDOR_ID + "\",\"outUserId\":\"" + person.getOutUserId()
                        + "\",\"result\":\"pass\"}");

        assertEquals("SUCCESS", result);
        PayeeInfoDO updated = payeeInfoMapper.selectById(payee.getId());
        assertEquals(PayeeOnboardingOutcomeEnum.READY.getCode(), updated.getOnboardingState());
    }

    @Test
    public void testOnboardingCallbackWithoutSubMerchantIsRecordedAsFailure() {
        // 报文缺 appIdSub 时无法确定是哪家回收企业的收方档案：通知落失败可重放，
        // 由平台运营在通知监控里人工处理——不猜、不跨企业乱写（ADR 0017）
        PayeeInfoDO payee = insertPayee("USER_ONB_NO_VENDOR", "110101199001010043", "13800000043");
        IcbcNaturalPersonDO person = personOf(payee);

        String result = callbackNotifyService.receive(
                "{\"appId\":\"A\",\"outUserId\":\"" + person.getOutUserId()
                        + "\",\"result\":\"pass\",\"openacctStatus\":\"02\"}");

        assertEquals("FAILURE", result);
        assertNull(payeeInfoMapper.selectById(payee.getId()).getOnboardingState());
    }

    private PayeeInfoDO insertPayee(String partnerPayeeId, String idCardNo, String mobile) {
        NaturalPersonRegisterReqVO reqVO = new NaturalPersonRegisterReqVO();
        reqVO.setName("张三");
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        IcbcNaturalPersonDO person = naturalPersonService.register(reqVO);

        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId(partnerPayeeId)
                .naturalPersonId(person.getId())
                .name("张三")
                .idCardNo(idCardNo)
                .mobile(mobile)
                .bankCardNo("6222021234567890")
                .realNameStatus(PayeeRealNameStatusEnum.NOT_STARTED.getStatus())
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private IcbcNaturalPersonDO personOf(PayeeInfoDO payee) {
        return naturalPersonService.getNaturalPerson(payee.getNaturalPersonId());
    }

    private void insertPayer(String outVendorId) {
        PayerInfoDO payer = new PayerInfoDO();
        payer.setPartnerPayerId(outVendorId);
        payer.setName("某某再生资源有限公司");
        payer.setTenantId(1L);
        payerInfoMapper.insert(payer);
    }

}
