package cn.iocoder.yudao.module.icbc.service.token.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.token.IcbcPublicTokenDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.token.IcbcPublicTokenMapper;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenCodec;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenPayload;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 公开令牌 Service 实现。
 *
 * <p>令牌生命周期：mint（当前租户内签发，绑定业务单）→ redeem（验签 + 查库核验有效期与次数，
 * 原子占用一次）。令牌记录里显式存了租户编号，供免登录端点把请求放回正确租户下执行。
 */
@Service
@Validated
public class PublicTokenServiceImpl implements PublicTokenService {

    /** 令牌有效期（小时）。短期令牌，够用即可。 */
    private static final int TOKEN_TTL_HOURS = 24;

    @Resource
    private PublicTokenCodec publicTokenCodec;
    @Resource
    private IcbcPublicTokenMapper publicTokenMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublicTokenRespVO mint(PublicTokenCreateReqVO reqVO) {
        PublicTokenPurposeEnum purpose = PublicTokenPurposeEnum.ofCode(reqVO.getPurpose())
                .orElseThrow(() -> exception(PUBLIC_TOKEN_PURPOSE_INVALID));
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(PUBLIC_TOKEN_INVALID); // 签发必须在某个租户上下文中
        }
        String businessKey = resolveBusinessKey(purpose, reqVO);

        LocalDateTime expiresTime = LocalDateTime.now().plusHours(TOKEN_TTL_HOURS);
        String jti = UUID.randomUUID().toString().replace("-", "");
        PublicTokenPayload payload = PublicTokenPayload.builder()
                .jti(jti)
                .purpose(purpose.getCode())
                .tenantId(tenantId)
                .businessKey(businessKey)
                .expiresAt(expiresTime.atZone(ZoneId.systemDefault()).toEpochSecond())
                .build();
        String token = publicTokenCodec.sign(payload);

        publicTokenMapper.insert(IcbcPublicTokenDO.builder()
                .jti(jti)
                .purpose(purpose.getCode())
                .tenantId(tenantId)
                .businessKey(businessKey)
                .maxUses(purpose.getMaxUses())
                .usedCount(0)
                .expiresTime(expiresTime)
                .build());

        PublicTokenRespVO respVO = new PublicTokenRespVO();
        respVO.setToken(token);
        respVO.setPurpose(purpose.getCode());
        respVO.setPurposeName(purpose.getName());
        respVO.setBusinessKey(businessKey);
        respVO.setExpiresTime(expiresTime);
        respVO.setMaxUses(purpose.getMaxUses());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublicTokenPayload redeem(String token, PublicTokenPurposeEnum expectedPurpose) {
        PublicTokenPayload payload = verify(token, expectedPurpose);
        consume(payload);
        return payload;
    }

    @Override
    public PublicTokenPayload verify(String token, PublicTokenPurposeEnum expectedPurpose) {
        PublicTokenPayload payload = publicTokenCodec.verify(token);
        if (!expectedPurpose.getCode().equals(payload.getPurpose())) {
            throw exception(PUBLIC_TOKEN_PURPOSE_MISMATCH);
        }
        IcbcPublicTokenDO record = publicTokenMapper.selectByJti(payload.getJti());
        if (record == null || !expectedPurpose.getCode().equals(record.getPurpose())) {
            throw exception(PUBLIC_TOKEN_PURPOSE_MISMATCH);
        }
        if (record.getExpiresTime() != null && record.getExpiresTime().isBefore(LocalDateTime.now())) {
            throw exception(PUBLIC_TOKEN_EXPIRED);
        }
        return payload;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consume(PublicTokenPayload payload) {
        IcbcPublicTokenDO record = publicTokenMapper.selectByJti(payload.getJti());
        if (record == null || !publicTokenMapper.consume(record.getId())) {
            throw exception(PUBLIC_TOKEN_USED_UP);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(String token) {
        // 验签只证明「这枚令牌是我们签的」；是否在库里、是否已过期由下面判定。
        // 已经过期的令牌不再重复作废，直接告诉调用方「这枚已经不在有效期内」。
        PublicTokenPayload payload = publicTokenCodec.verify(token);
        IcbcPublicTokenDO record = publicTokenMapper.selectByJti(payload.getJti());
        if (record == null) {
            throw exception(PUBLIC_TOKEN_NOT_FOUND);
        }
        // 作废 = 把有效期提前到现在：verify / redeem 都会按「已过期」拒绝，链接立刻失效。
        // 不新增「作废」列：令牌表已进脊柱建表脚本，加列成本高于收益，而有效期语义足够表达。
        IcbcPublicTokenDO update = new IcbcPublicTokenDO();
        update.setId(record.getId());
        update.setExpiresTime(LocalDateTime.now());
        publicTokenMapper.updateById(update);
    }

    private String resolveBusinessKey(PublicTokenPurposeEnum purpose, PublicTokenCreateReqVO reqVO) {
        if (purpose.getBusinessKeyType() == PublicTokenPurposeEnum.BusinessKeyType.ONBOARDING_INVITE) {
            // 自填建档：链接生成时这个人可能还没有收方档案，绑定这枚链接本身即可（#94）
            return UUID.randomUUID().toString().replace("-", "");
        }
        if (purpose.getBusinessKeyType() == PublicTokenPurposeEnum.BusinessKeyType.ORDER) {
            if (StrUtil.isBlank(reqVO.getPartnerOrderId())) {
                throw exception(PUBLIC_TOKEN_BUSINESS_KEY_MISSING);
            }
            if (invoiceOrderMapper.selectByPartnerOrderId(reqVO.getPartnerOrderId()) == null) {
                throw exception(INVOICE_ORDER_NOT_EXISTS);
            }
            return reqVO.getPartnerOrderId();
        }
        if (reqVO.getPayeeId() == null) {
            throw exception(PUBLIC_TOKEN_BUSINESS_KEY_MISSING);
        }
        if (payeeInfoMapper.selectById(reqVO.getPayeeId()) == null) {
            throw exception(PAYEE_NOT_EXISTS);
        }
        return reqVO.getPayeeId().toString();
    }

}
