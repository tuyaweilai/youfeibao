package cn.iocoder.yudao.module.icbc.service.token;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 「指回本平台某个公开页面」的链接组装处（#106）。
 *
 * <p>与 {@link SellerAppLinkBuilder} 的区别：那个拼的是**自然人端前端**的入口
 * （{@code /#/?token=…}），这个拼的是**后端自己输出 HTML 的公开页面**
 * （{@code /admin-api/icbc/public/…}）。工行确认页面的表单 HTML 由后端生成，所以链接要指回后端。
 *
 * <p>基地址取 {@code icbc.public-base-url}（形如 {@code https://host/admin-api}）；
 * 没配时退到当前请求的 origin + {@code icbc.public-api-prefix}（默认 {@code /admin-api}），
 * 这样本地联调不用先配一个公网地址也能把流程走通。
 */
@Component
public class PublicPageLinkBuilder {

    /** 公开页面的基地址（含 API 前缀），如 https://platform.example.com/admin-api */
    private final String publicBaseUrl;
    /** 未配基地址时的退化前缀 */
    private final String apiPrefix;
    private final PublicTokenService publicTokenService;

    public PublicPageLinkBuilder(@Value("${icbc.public-base-url:}") String publicBaseUrl,
                                 @Value("${icbc.public-api-prefix:/admin-api}") String apiPrefix,
                                 PublicTokenService publicTokenService) {
        this.publicBaseUrl = publicBaseUrl;
        this.apiPrefix = apiPrefix;
        this.publicTokenService = publicTokenService;
    }

    /**
     * 签发一枚绑定「合作方订单号」的令牌，并拼出公开页面链接。
     *
     * @param path            公开页面路径（如 {@code /icbc/public/invoice/confirm-page}）
     * @param partnerOrderId  合作方订单号（业务键）
     * @param purpose         令牌用途
     * @return 链接；基地址既没配也取不到当前请求时返回 {@code null}，由调用方说明原因
     */
    public String buildOrderPageLink(String path, String partnerOrderId, PublicTokenPurposeEnum purpose) {
        String base = resolveBase();
        if (base == null || StrUtil.isBlank(partnerOrderId)) {
            return null;
        }
        PublicTokenCreateReqVO reqVO = new PublicTokenCreateReqVO();
        reqVO.setPurpose(purpose.getCode());
        reqVO.setPartnerOrderId(partnerOrderId);
        PublicTokenRespVO token = publicTokenService.mint(reqVO);
        return base.replaceAll("/+$", "") + path + "?token=" + token.getToken();
    }

    /**
     * 基地址：配置优先；没配则用当前请求的 origin + API 前缀（本地联调可用）。
     */
    public String resolveBase() {
        if (StrUtil.isNotBlank(publicBaseUrl)) {
            return publicBaseUrl.trim();
        }
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request == null) {
                return null;
            }
            String origin = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() > 0 ? ":" + request.getServerPort() : "");
            String prefix = StrUtil.blankToDefault(apiPrefix, "").trim();
            return origin + (prefix.startsWith("/") ? prefix : "/" + prefix);
        } catch (RuntimeException e) {
            // 无请求上下文（定时任务 / 单测）：没有基地址就是没有，不编一个
            return null;
        }
    }

    /**
     * 基地址是否已配置（不含「从请求退化」这一路）。
     */
    public boolean isConfigured() {
        return StrUtil.isNotBlank(publicBaseUrl);
    }

}
