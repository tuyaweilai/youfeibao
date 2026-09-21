package cn.iocoder.yudao.module.icbc.service.token;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.SELLER_APP_ENTRY_NOT_CONFIGURED;

/**
 * 自然人端入口链接的唯一组装处：入口地址 + 一次性令牌。
 *
 * <p>「让自然人打开我们某个页面」有好几处（触达通知 #36、实名完成后的回跳 #82、发票下载 …），
 * 入口地址的取法与链接形状只该有一份。入口地址取 {@code icbc.notify.seller-app-url}，
 * 未配置时回退场站入口 {@code icbc.station.entry-url}。
 *
 * <p>两种调用姿势，区别只在「缺配置算不算致命」：
 * <ul>
 *   <li>{@link #buildLink} / {@link #buildPayeeLink} 返回 {@code null}——用于本就可以跳过的场景
 *       （触达通知没入口就落一条记录）；</li>
 *   <li>{@link #requirePayeeLink} 抛业务异常——用于缺了就办不成的场景（实名回跳缺了就回不到我们这里，
 *       不能让请求带着空值去求工行，见 #82）。</li>
 * </ul>
 */
@Component
public class SellerAppLinkBuilder {

    /** 自然人端入口（拼一次性令牌链接的首选地址） */
    private final String sellerAppUrl;
    /** 场站入口（seller-app-url 未配置时的回退） */
    private final String stationEntryUrl;
    private final PublicTokenService publicTokenService;

    public SellerAppLinkBuilder(@Value("${icbc.notify.seller-app-url:}") String sellerAppUrl,
                                @Value("${icbc.station.entry-url:}") String stationEntryUrl,
                                PublicTokenService publicTokenService) {
        this.sellerAppUrl = sellerAppUrl;
        this.stationEntryUrl = stationEntryUrl;
        this.publicTokenService = publicTokenService;
    }

    /**
     * 自然人端入口地址；两处都没配时返回 {@code null}。
     */
    public String entryUrl() {
        if (StrUtil.isNotBlank(sellerAppUrl)) {
            return sellerAppUrl.trim();
        }
        return StrUtil.isBlank(stationEntryUrl) ? null : stationEntryUrl.trim();
    }

    /**
     * 入口地址是否已配置。
     */
    public boolean isConfigured() {
        return entryUrl() != null;
    }

    /**
     * 签发令牌并拼链接；入口地址未配置时返回 {@code null}，由调用方决定是跳过还是报错。
     */
    public SellerAppLink buildLink(PublicTokenCreateReqVO reqVO) {
        String base = entryUrl();
        if (base == null) {
            return null;
        }
        PublicTokenRespVO token = publicTokenService.mint(reqVO);
        return SellerAppLink.builder()
                .token(token.getToken())
                .expiresTime(token.getExpiresTime())
                .link(base.replaceAll("/+$", "") + "/#/?token=" + token.getToken()
                        + "&purpose=" + reqVO.getPurpose())
                .build();
    }

    /**
     * 绑定收方的链接；入口地址未配置时返回 {@code null}。
     */
    public SellerAppLink buildPayeeLink(Long payeeId, PublicTokenPurposeEnum purpose) {
        if (payeeId == null) {
            return null;
        }
        PublicTokenCreateReqVO reqVO = new PublicTokenCreateReqVO();
        reqVO.setPurpose(purpose.getCode());
        reqVO.setPayeeId(payeeId);
        return buildLink(reqVO);
    }

    /**
     * 同上，但入口地址未配置时抛明确错误：宁可当场失败，也不把空值带给工行 / 落一条点不开的记录。
     */
    public SellerAppLink requirePayeeLink(Long payeeId, PublicTokenPurposeEnum purpose) {
        SellerAppLink link = buildPayeeLink(payeeId, purpose);
        if (link == null) {
            throw exception(SELLER_APP_ENTRY_NOT_CONFIGURED);
        }
        return link;
    }

}
