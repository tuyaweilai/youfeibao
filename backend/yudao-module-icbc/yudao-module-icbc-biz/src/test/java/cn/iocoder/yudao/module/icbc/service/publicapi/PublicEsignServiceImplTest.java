package cn.iocoder.yudao.module.icbc.service.publicapi;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicAgreementSignRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.service.publicapi.impl.PublicEsignServiceImpl;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenCodec;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import cn.iocoder.yudao.module.icbc.service.token.impl.PublicTokenServiceImpl;
import cn.iocoder.yudao.module.icbc.service.esign.FrameworkAgreementEsignService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.PUBLIC_TOKEN_PURPOSE_MISMATCH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PublicEsignServiceImpl} 的单元测试（#95）。
 *
 * <p>公开端点没有登录态与租户头：令牌解析出收方与租户后，显式切到该租户下执行
 * （与其它公开端点同一套做法）。这里断言「租户切对了、收方键对上了、链接原样返回」。
 */
@Import({PublicEsignServiceImpl.class, PublicTokenServiceImpl.class, PublicTokenCodec.class})
@TestPropertySource(properties = {
        "icbc.public-token.secret=test-public-token-secret-0123456789abcdef"})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class PublicEsignServiceImplTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    @Resource
    private PublicEsignService publicEsignService;
    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @MockBean
    private FrameworkAgreementEsignService frameworkAgreementEsignService;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testCreateSignUrl_resolvesPayeeAndSwitchesTenant() {
        PayeeInfoDO payee = insertPayee("张三", "110101199001011234");
        String token = mint("ONBOARDING", payee.getId());
        when(frameworkAgreementEsignService.createSignUrl(anyLong())).thenAnswer(invocation -> {
            // 公开端点没有租户上下文，实现必须自己切到令牌解析出的租户
            assertEquals(TENANT_ID, TenantContextHolder.getTenantId());
            assertEquals(payee.getId(), invocation.getArgument(0));
            return "https://esign/sign?one-time";
        });

        PublicAgreementSignRespVO resp = publicEsignService.createSignUrl(token);

        assertEquals("https://esign/sign?one-time", resp.getSignUrl());
        verify(frameworkAgreementEsignService).createSignUrl(payee.getId());
    }

    @Test
    public void testCreateSignUrl_wrongPurposeRejected() {
        PayeeInfoDO payee = insertPayee("李四", "110101199002022345");
        String quotaToken = mint("QUOTA_QUERY", payee.getId());

        assertServiceException(() -> publicEsignService.createSignUrl(quotaToken), PUBLIC_TOKEN_PURPOSE_MISMATCH);
    }

    // ==================== 助手 ====================

    private String mint(String purpose, Long payeeId) {
        PublicTokenCreateReqVO reqVO = new PublicTokenCreateReqVO();
        reqVO.setPurpose(purpose);
        reqVO.setPayeeId(payeeId);
        return publicTokenService.mint(reqVO).getToken();
    }

    private PayeeInfoDO insertPayee(String name, String idCardNo) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .name(name).mobile("13800138000").idCardNo(idCardNo)
                .partnerPayeeId("PARTNER_" + idCardNo)
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

}
