package cn.iocoder.yudao.module.icbc.service.token;

import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.SELLER_APP_ENTRY_NOT_CONFIGURED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link SellerAppLinkBuilder} 的单元测试（#82）。
 *
 * <p>两件事必须钉住：入口地址只从一个地方取（seller-app-url，缺失回退场站入口），
 * 以及「缺配置」在两种调用姿势下的不同结果——可跳过的返回 null，办不成事的当场报错。
 * 后者是 #82 要修的毛病：以前缺配置会静默带空值去求工行。
 */
public class SellerAppLinkBuilderTest {

    private static final String SECRET_APP = "https://seller.example.com";

    private final PublicTokenService publicTokenService = mock(PublicTokenService.class);

    @Test
    public void testBuildPayeeLink_usesSellerAppUrlAndAppendsTokenAndPurpose() {
        when(publicTokenService.mint(any())).thenReturn(token("TOKEN_1", LocalDateTime.of(2026, 9, 22, 10, 0)));
        SellerAppLinkBuilder builder = new SellerAppLinkBuilder(SECRET_APP + "/", "", publicTokenService);

        SellerAppLink link = builder.buildPayeeLink(7L, PublicTokenPurposeEnum.ONBOARDING);

        // 末尾斜杠只留一个；令牌与用途都进 query（令牌是 base64url，不用再编码）
        assertEquals("https://seller.example.com/#/?token=TOKEN_1&purpose=ONBOARDING", link.getLink());
        assertEquals("TOKEN_1", link.getToken());
        assertEquals(LocalDateTime.of(2026, 9, 22, 10, 0), link.getExpiresTime());
        ArgumentCaptor<PublicTokenCreateReqVO> captor = ArgumentCaptor.forClass(PublicTokenCreateReqVO.class);
        verify(publicTokenService).mint(captor.capture());
        assertEquals("ONBOARDING", captor.getValue().getPurpose());
        assertEquals(7L, captor.getValue().getPayeeId());
    }

    @Test
    public void testBuildPayeeLink_fallsBackToStationEntryUrl() {
        when(publicTokenService.mint(any())).thenReturn(token("TOKEN_2", null));
        SellerAppLinkBuilder builder = new SellerAppLinkBuilder("", "https://station.example.com", publicTokenService);

        SellerAppLink link = builder.buildPayeeLink(8L, PublicTokenPurposeEnum.SELLER_NOTICE);

        assertTrue(link.getLink().startsWith("https://station.example.com/#/?token=TOKEN_2"));
        assertTrue(builder.isConfigured());
    }

    @Test
    public void testBuildLink_returnsNullWhenNoEntryConfigured() {
        SellerAppLinkBuilder builder = new SellerAppLinkBuilder("  ", "", publicTokenService);

        assertFalse(builder.isConfigured());
        assertNull(builder.buildPayeeLink(7L, PublicTokenPurposeEnum.ONBOARDING));
        // 没入口地址就不该去签发令牌
        verifyNoInteractions(publicTokenService);
    }

    @Test
    public void testRequirePayeeLink_throwsClearErrorWhenNoEntryConfigured() {
        SellerAppLinkBuilder builder = new SellerAppLinkBuilder("", "", publicTokenService);

        // 缺配置 = 办不成：明确报错，而不是让调用方带着空 URL 去求工行
        assertServiceException(() -> builder.requirePayeeLink(7L, PublicTokenPurposeEnum.ONBOARDING),
                SELLER_APP_ENTRY_NOT_CONFIGURED);
    }

    private PublicTokenRespVO token(String value, LocalDateTime expiresTime) {
        PublicTokenRespVO respVO = new PublicTokenRespVO();
        respVO.setToken(value);
        respVO.setExpiresTime(expiresTime);
        return respVO;
    }

}
