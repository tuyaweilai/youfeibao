package cn.iocoder.yudao.module.icbc.service.token;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
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
import cn.iocoder.yudao.test.icbc.IcbcTenantTestConfiguration;
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
 *
 * <p>{@link IcbcTenantTestConfiguration} 打开多租户拦截器：作废是「在全局表上做鉴权端写入」，
 * 与其它方法不同——{@code icbc_public_token} 在 {@code ignore-tables} 里，SQL 没有租户条件，
 * 租户收口必须在 Java 侧做（#94 评审 S-1）。
 */
@Import({PublicTokenServiceImpl.class, PublicTokenCodec.class, IcbcTenantTestConfiguration.class})
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
    @Resource
    private PublicTokenCodec publicTokenCodec;

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

    @Test
    public void testMint_onboardingWizardInvite_needsNoExistingPayee() {
        // 待建档：链接生成时这个人还没有收方档案，绑定链接本身，业务键类型是 ONBOARDING_INVITE（#94）
        PublicTokenRespVO respVO = publicTokenService.mint(createReq("ONBOARDING_WIZARD", null, null));

        assertNotNull(respVO.getToken());
        assertNotNull(respVO.getBusinessKey(), "邀请令牌的业务键由后端生成");
        assertEquals(1, respVO.getMaxUses(), "一枚链接只建一份档案：成功落库占唯一一次");
        PublicTokenPayload payload = publicTokenService.verify(respVO.getToken(), PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        assertEquals(TENANT_ID, payload.getTenantId());
        assertEquals(PublicTokenPurposeEnum.BusinessKeyType.ONBOARDING_INVITE.name(), payload.getBusinessKeyType(),
                "待建档链接绑的是链接本身");
    }

    @Test
    public void testMint_onboardingWizardWithPayee_bindsPayeeId() {
        // 已建档：收货员给了 payeeId，链接锁到那个人身上（#94 修票 ST-1 结构根因）
        PayeeInfoDO payee = insertPayee();

        PublicTokenRespVO respVO = publicTokenService.mint(createReq("ONBOARDING_WIZARD", null, payee.getId()));

        assertEquals(payee.getId().toString(), respVO.getBusinessKey(), "业务键就是收方 ID");
        PublicTokenPayload payload = publicTokenService.verify(respVO.getToken(), PublicTokenPurposeEnum.ONBOARDING_WIZARD);
        assertEquals(PublicTokenPurposeEnum.BusinessKeyType.PAYEE.name(), payload.getBusinessKeyType());
    }

    @Test
    public void testMint_onboardingWizardWithMissingPayee_rejected() {
        assertServiceException(() -> publicTokenService.mint(createReq("ONBOARDING_WIZARD", null, 999L)),
                PAYEE_NOT_EXISTS);
    }

    @Test
    public void testRevoke_tokenRejectedAfterwards() {
        PublicTokenRespVO respVO = publicTokenService.mint(createReq("ONBOARDING_WIZARD", null, null));

        publicTokenService.revoke(respVO.getToken());

        // 被作废的链接本人打开：提示「已被作废」，不是「已过期」（#94 复审 ST-5）
        assertServiceException(() -> publicTokenService.verify(respVO.getToken(), PublicTokenPurposeEnum.ONBOARDING_WIZARD),
                PUBLIC_TOKEN_REVOKED);
    }

    @Test
    public void testRevoke_isIdempotent() {
        PublicTokenRespVO respVO = publicTokenService.mint(createReq("ONBOARDING_WIZARD", null, null));

        publicTokenService.revoke(respVO.getToken());
        // 再点一次不该报错（已不可用，没什么可作废的）
        assertDoesNotThrow(() -> publicTokenService.revoke(respVO.getToken()));
        assertServiceException(() -> publicTokenService.verify(respVO.getToken(), PublicTokenPurposeEnum.ONBOARDING_WIZARD),
                PUBLIC_TOKEN_REVOKED);
    }

    @Test
    public void testRevoke_crossTenantTokenTreatedAsNotFound_andRowUnchanged() {
        // 另一家回收企业（tenantId=2）签发的邀请令牌：本租户拿到字符串也不能作废它。
        // 这条用例必须真红：icbc_public_token 在 ignore-tables 里，selectByJti / updateById 都没有租户条件，
        // 没有 Java 侧收口时 revoke 会成功、下面两处断言（NotFound + 行不变）全崩。
        PublicTokenRespVO minted = TenantUtils.execute(2L,
                () -> publicTokenService.mint(createReq("ONBOARDING_WIZARD", null, null)));
        String jti = publicTokenCodec.verify(minted.getToken()).getJti();
        LocalDateTime before = publicTokenMapper.selectByJti(jti).getExpiresTime();

        TenantContextHolder.setTenantId(TENANT_ID); // 当前上下文是租户 1
        assertServiceException(() -> publicTokenService.revoke(minted.getToken()), PUBLIC_TOKEN_NOT_FOUND);

        assertEquals(before, publicTokenMapper.selectByJti(jti).getExpiresTime(),
                "别家企业的令牌行一个字都不能改");
    }

    @Test
    public void testRevoke_nonRevocablePurposeRejected_andRowUnchanged() {
        insertOrder("ORDER_T7");
        PublicTokenRespVO minted = publicTokenService.mint(createReq("INVOICE_DOWNLOAD", "ORDER_T7", null));
        String jti = publicTokenCodec.verify(minted.getToken()).getJti();
        LocalDateTime before = publicTokenMapper.selectByJti(jti).getExpiresTime();

        // 本租户、签名有效，但这个用途不支持作废：新端点不能变成掐断任意链接的万能钥匙
        assertServiceException(() -> publicTokenService.revoke(minted.getToken()), PUBLIC_TOKEN_PURPOSE_MISMATCH);

        assertEquals(before, publicTokenMapper.selectByJti(jti).getExpiresTime(), "被拒后行不能被改动");
    }

    @Test
    public void testRevoke_recordMissingGivesReadableError() {
        PublicTokenRespVO respVO = publicTokenService.mint(createReq("ONBOARDING_WIZARD", null, null));
        // 签名有效但库记录已不存在（清理 / 误删）：不能静默成功
        publicTokenMapper.deleteById(publicTokenMapper.selectList().get(0).getId());

        assertServiceException(() -> publicTokenService.revoke(respVO.getToken()), PUBLIC_TOKEN_NOT_FOUND);
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
