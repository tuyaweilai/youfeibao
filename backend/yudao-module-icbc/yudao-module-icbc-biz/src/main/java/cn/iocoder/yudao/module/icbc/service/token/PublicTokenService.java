package cn.iocoder.yudao.module.icbc.service.token;

import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;

/**
 * 公开令牌 Service：签发与校验（含单用途 / 限次 / 有效期）。
 */
public interface PublicTokenService {

    /**
     * 在当前租户下为某个业务单签发一枚公开令牌。
     *
     * @param reqVO 用途与绑定业务
     * @return 令牌信息
     */
    PublicTokenRespVO mint(PublicTokenCreateReqVO reqVO);

    /**
     * 校验并占用一枚令牌。签名无效 / 过期 / 用途不符 / 次数用尽都会抛业务异常。
     *
     * @param token 令牌
     * @param expectedPurpose 期望用途
     * @return 令牌载荷（含租户与业务键）
     */
    PublicTokenPayload redeem(String token, PublicTokenPurposeEnum expectedPurpose);

    /**
     * 只校验，不占用次数。用于「先确认后续动作可行，再扣次数」的场景（如发票下载）。
     *
     * @param token 令牌
     * @param expectedPurpose 期望用途
     * @return 令牌载荷
     */
    PublicTokenPayload verify(String token, PublicTokenPurposeEnum expectedPurpose);

    /**
     * 原子占用一枚已校验的令牌一次。
     *
     * @param payload 令牌载荷
     */
    void consume(PublicTokenPayload payload);

}
