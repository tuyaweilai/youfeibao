package cn.iocoder.yudao.module.icbc.service.payment;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payment.vo.PaymentStatusQueryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.service.payment.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PaymentServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import({UnitTestConfiguration.class, PaymentServiceImpl.class})
@TestPropertySource(properties = "icbc.gateway.mode=fake")
public class PaymentServiceImplTest extends BaseDbUnitTest {

    @Resource
    private PaymentService paymentService;

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Test
    public void testCreatePayment_success() {
        // 准备参数
        PaymentReqVO reqVO = randomPojo(PaymentReqVO.class, o -> {
            o.setAppId("test-app-id");
            o.setOutOrderId("TEST_ORDER_001");
            o.setOutVendorId("010020200513111111");
            o.setOutUserId("10000000000000003");
            o.setVerifiedCode("20201128531215026");
            o.setUkeyId("20201128531215026");
        });

        // 调用
        PaymentRespVO result = paymentService.createPayment(reqVO);

        // 断言
        assertNotNull(result);
        assertEquals("0", result.getReturnCode());
        assertEquals("成功", result.getReturnMsg());
        assertEquals(reqVO.getOutOrderId(), result.getOutOrderId());
        assertNotNull(result.getRedirectUrl());
        assertNotNull(result.getMsgId());
        assertEquals("PENDING", result.getPaymentStatus());

        // 验证数据库记录
        PaymentOrderDO paymentOrder = paymentOrderMapper.selectByPartnerOrderId(reqVO.getOutOrderId());
        assertNotNull(paymentOrder);
        assertEquals(reqVO.getOutOrderId(), paymentOrder.getPartnerOrderId());
        assertEquals(reqVO.getOutVendorId(), paymentOrder.getPayerNo());
        assertEquals(reqVO.getOutUserId(), paymentOrder.getPayeeNo());
        assertEquals(0, paymentOrder.getPaymentStatus());
    }

    @Test
    public void testQueryPaymentStatus_success() {
        // 准备数据
        PaymentOrderDO paymentOrder = randomPojo(PaymentOrderDO.class, o -> {
            o.setId(null);
            o.setPartnerOrderId("TEST_ORDER_002");
            o.setIcbcOrderNo("ICBC_ORDER_002");
            o.setPaymentStatus(2); // 支付成功
            o.setPaymentAmount(new BigDecimal("1000.00"));
            o.setPayeeNo("010020200513111111");
            o.setPayerNo("10000000000000003");
        });
        paymentOrderMapper.insert(paymentOrder);

        // 准备参数
        PaymentStatusQueryReqVO reqVO = new PaymentStatusQueryReqVO();
        reqVO.setOutOrderId("TEST_ORDER_002");

        // 调用
        PaymentStatusQueryRespVO result = paymentService.queryPaymentStatus(reqVO);

        // 断言
        assertNotNull(result);
        assertEquals("0", result.getReturnCode());
        assertEquals("成功", result.getReturnMsg());
        assertEquals("TEST_ORDER_002", result.getOutOrderId());
        assertEquals("ICBC_ORDER_002", result.getIcbcOrderNo());
        assertEquals("SUCCESS", result.getPaymentStatus());
        assertEquals(new BigDecimal("1000.00"), result.getPaymentAmount());
    }

} 