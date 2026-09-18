package cn.iocoder.yudao.module.icbc.service.token;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.token.IcbcPublicTokenDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.token.IcbcPublicTokenMapper;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.service.token.impl.PublicTokenServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PublicTokenServiceImpl} 的单元测试：签发、验签、单用途/限次、租户与业务绑定。
 */
@Import({PublicTokenServiceImpl.class, PublicTokenCodec.class})
@TestPropertySource(properties = "icbc.public-token.secret=test-public-token-secret-0123456789abcdef")
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class PublicTokenServiceImplTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcPublicTokenMapper publicTokenMapper;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testMintAndRedeem_invoiceDownloadResolvesTenantAndBusiness() {
        insertOrder("ORDER_T1");

        PublicTokenRespVO respVO = publicTokenService.mint(
                createReq("INVOICE_DOWNLOAD", "ORDER_T1", null));

        assertNotNull(respVO.getToken());
        assertEquals("INVOICE_DOWNLOAD", respVO.getPurpose());
        assertEquals("ORDER_T1", respVO.getBusinessKey());
        assertEquals(1, respVO.getMaxUses());

        PublicTokenPayload payload = publicTokenService.redeem(respVO.getToken(), PublicTokenPurposeEnum.INVOICE_DOWNLOAD);
        assertEquals(TENANT_ID, payload.getTenantId());
        assertEquals("ORDER_T1", payload.getBusinessKey());
    }

    @Test
    public void testRedeem_singleUseTokenRejectedOnSecondRedeem() {
        insertOrder("ORDER_T2");
        PublicTokenRespVO respVO = publicTokenService.mint(createReq("INVOICE_DOWNLOAD", "ORDER_T2", null));

        publicTokenService.redeem(respVO.getToken(), PublicTokenPurposeEnum.INVOICE_DOWNLOAD);
        assertServiceException(() -> publicTokenService.redeem(respVO.getToken(), PublicTokenPurposeEnum.INVOICE_DOWNLOAD),
                PUBLIC_TOKEN_USED_UP);
    }

    @Test
    public void testRedeem_quotaTokenAllowsLimitedReuse() {
        PayeeInfoDO payee = insertPayee();
        PublicTokenRespVO respVO = publicTokenService.mint(createReq("QUOTA_QUERY", null, payee.getId()));

        for (int i = 0; i < 20; i++) {
            publicTokenService.redeem(respVO.getToken(), PublicTokenPurposeEnum.QUOTA_QUERY);
        }
        assertServiceException(() -> publicTokenService.redeem(respVO.getToken(), PublicTokenPurposeEnum.QUOTA_QUERY),
                PUBLIC_TOKEN_USED_UP);
    }

    @Test
    public void testRedeem_tamperedTokenRejected() {
        insertOrder("ORDER_T3");
        String token = publicTokenService.mint(createReq("INVOICE_DOWNLOAD", "ORDER_T3", null)).getToken();
        // 改动 payload 首字符，签名就对不上
        char first = token.charAt(0);
        String tampered = (first == 'A' ? 'B' : 'A') + token.substring(1);

        assertServiceException(() -> publicTokenService.redeem(tampered, PublicTokenPurposeEnum.INVOICE_DOWNLOAD),
                PUBLIC_TOKEN_INVALID);
    }

    @Test
    public void testRedeem_wrongPurposeRejected() {
        insertOrder("ORDER_T4");
        String token = publicTokenService.mint(createReq("INVOICE_DOWNLOAD", "ORDER_T4", null)).getToken();

        assertServiceException(() -> publicTokenService.redeem(token, PublicTokenPurposeEnum.QUOTA_QUERY),
                PUBLIC_TOKEN_PURPOSE_MISMATCH);
    }

    @Test
    public void testRedeem_expiredTokenRejected() {
        insertOrder("ORDER_T5");
        PublicTokenRespVO respVO = publicTokenService.mint(createReq("INVOICE_DOWNLOAD", "ORDER_T5", null));
        // 直接把库里的有效期改到过去
        IcbcPublicTokenDO record = publicTokenMapper.selectList().get(0);
        record.setExpiresTime(LocalDateTime.now().minusHours(1));
        publicTokenMapper.updateById(record);

        assertServiceException(() -> publicTokenService.redeem(respVO.getToken(), PublicTokenPurposeEnum.INVOICE_DOWNLOAD),
                PUBLIC_TOKEN_EXPIRED);
    }

    @Test
    public void testMint_orderNotExists() {
        assertServiceException(() -> publicTokenService.mint(createReq("INVOICE_DOWNLOAD", "ORDER_MISSING", null)),
                INVOICE_ORDER_NOT_EXISTS);
    }

    @Test
    public void testMint_payeeNotExists() {
        assertServiceException(() -> publicTokenService.mint(createReq("QUOTA_QUERY", null, 999L)),
                PAYEE_NOT_EXISTS);
    }

    @Test
    public void testMint_missingBusinessKey() {
        assertServiceException(() -> publicTokenService.mint(createReq("INVOICE_DOWNLOAD", null, null)),
                PUBLIC_TOKEN_BUSINESS_KEY_MISSING);
    }

    @Test
    public void testMint_invalidPurpose() {
        assertServiceException(() -> publicTokenService.mint(createReq("NOT_A_PURPOSE", "ORDER_X", null)),
                PUBLIC_TOKEN_PURPOSE_INVALID);
    }

    // ==================== 造数 ====================

    private PublicTokenCreateReqVO createReq(String purpose, String partnerOrderId, Long payeeId) {
        PublicTokenCreateReqVO reqVO = new PublicTokenCreateReqVO();
        reqVO.setPurpose(purpose);
        reqVO.setPartnerOrderId(partnerOrderId);
        reqVO.setPayeeId(payeeId);
        return reqVO;
    }

    private PayeeInfoDO insertPayee() {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .name("张三").mobile("13800138000").idCardNo("110101199001011234")
                .partnerPayeeId("PARTNER_P1")
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private void insertOrder(String partnerOrderId) {
        invoiceOrderMapper.insert(InvoiceOrderDO.builder()
                .orderNo("INV_" + partnerOrderId).partnerOrderId(partnerOrderId)
                .payeeNo("PARTNER_P1").payerNo("PAYER_1")
                .totalAmount(new BigDecimal("1000.00")).invoiceType(1).businessType("SCRAP")
                .orderStatus(0).invoiceStatus(0).paymentStatus(0).taxStatus(0).build());
    }

}
