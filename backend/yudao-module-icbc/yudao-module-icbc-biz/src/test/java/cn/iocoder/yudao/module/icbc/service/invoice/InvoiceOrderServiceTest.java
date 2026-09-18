package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreOrderReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreOrderRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.OrderItemMapper;
import cn.iocoder.yudao.module.icbc.service.invoice.impl.InvoiceOrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link InvoiceOrderServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import({UnitTestConfiguration.class, InvoiceOrderServiceImpl.class})
@TestPropertySource(properties = "icbc.gateway.mode=fake")
public class InvoiceOrderServiceTest extends BaseDbUnitTest {

    @Resource
    private InvoiceOrderService invoiceOrderService;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Test
    public void testCreatePreOrder_success() {
        // 准备参数
        InvoicePreOrderReqVO reqVO = new InvoicePreOrderReqVO();
        reqVO.setOutOrderId("TEST_ORDER_001");
        reqVO.setOutVendorId("010020200513111111");
        reqVO.setOutUserId("10000000000000003");
        reqVO.setOrderAmount(new BigDecimal("1000.00"));
        reqVO.setInvoiceType("02");
        reqVO.setSpecificElements("24");
        reqVO.setBuyerInvTypeCode("04");
        reqVO.setNaturalPersonName("张三");
        reqVO.setCardType("111");
        reqVO.setCardNumber("110101199001011234");
        reqVO.setSellerAddress("北京市朝阳区");
        reqVO.setSellerTelephone("13800138000");
        reqVO.setTaxpayerNo("91110000123456789X");
        reqVO.setTaxpayerName("北京某某有限公司");
        reqVO.setDrawerName("李四");
        reqVO.setDrawerCardType("111");
        reqVO.setDrawerCardNumber("110101199001011234");
        reqVO.setAreaCode("110000");
        reqVO.setMac("00:11:22:33:44:55");
        reqVO.setTaxRate(new BigDecimal("0.13"));
        reqVO.setPayJumpUrl("https://example.com/pay/return");
        reqVO.setInvoiceNotifyUrl("https://example.com/invoice/notify");

        // 商品信息
        InvoicePreOrderReqVO.GoodsInfoVO goodsInfo = new InvoicePreOrderReqVO.GoodsInfoVO();
        goodsInfo.setGoodsSeqno("1");
        goodsInfo.setProjectName("废铁回收");
        goodsInfo.setGoodsNum(new BigDecimal("100"));
        goodsInfo.setGoodsAmt(new BigDecimal("1000.00"));
        goodsInfo.setPrice(new BigDecimal("10.00"));
        goodsInfo.setUnits("吨");
        goodsInfo.setTaxRate(new BigDecimal("0.13"));
        goodsInfo.setMergedCode("1090101010000000000");
        reqVO.setGoodsInfo(Collections.singletonList(goodsInfo));

        // 调用：经假适配层返回自然人确认页面表单
        InvoicePreOrderRespVO respVO = invoiceOrderService.createPreOrder(reqVO);

        // 断言
        assertNotNull(respVO);
        assertEquals(0, respVO.getReturnCode());
        assertEquals("TEST_ORDER_001", respVO.getPartnerOrderId());
        assertNotNull(respVO.getRedirectUrl());
        assertTrue(respVO.getRedirectUrl().contains("pre-order"));
        assertNotNull(invoiceOrderMapper.selectByPartnerOrderId("TEST_ORDER_001"));
    }

    @Test
    public void testQueryInvoiceInfo_success() {
        // 准备数据
        InvoiceOrderDO order = new InvoiceOrderDO();
        order.setOrderNo("INV123456789");
        order.setPartnerOrderId("TEST_ORDER_002");
        order.setPayeeNo("010020200513111111");
        order.setPayerNo("10000000000000003");
        order.setTotalAmount(new BigDecimal("2000.00"));
        order.setInvoiceType(1);
        order.setBusinessType("SCRAP");
        order.setOrderStatus(2);
        order.setInvoiceStatus(2);
        order.setPaymentStatus(2);
        order.setTaxStatus(2);
        order.setInvoiceNo("12345678");
        order.setInvoiceCode("144031909110");
        invoiceOrderMapper.insert(order);

        // 准备参数
        InvoiceQueryReqVO reqVO = new InvoiceQueryReqVO();
        reqVO.setOutOrderId("TEST_ORDER_002");

        // 调用
        InvoiceQueryRespVO respVO = invoiceOrderService.queryInvoiceInfo(reqVO);

        // 断言
        assertNotNull(respVO);
        assertEquals(0, respVO.getReturnCode());
        assertEquals("成功", respVO.getReturnMsg());
        assertEquals("INV123456789", respVO.getOrderNo());
        assertEquals("TEST_ORDER_002", respVO.getPartnerOrderId());
        assertEquals(Integer.valueOf(2), respVO.getOrderStatus());
        assertEquals(Integer.valueOf(2), respVO.getInvoiceStatus());
        assertEquals("12345678", respVO.getInvoiceNo());
        assertEquals("144031909110", respVO.getInvoiceCode());
    }

} 