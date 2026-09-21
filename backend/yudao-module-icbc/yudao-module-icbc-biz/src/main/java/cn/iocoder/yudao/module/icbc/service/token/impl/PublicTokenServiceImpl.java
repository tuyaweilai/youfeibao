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
import java.util.Objects;
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

    /**
     * 「被作废」的标记，写在令牌记录的 {@code remark} 上。
     *
     * <p>作废与否原本只靠「有效期提前到现在」表达，但这样本人打开被作废的链接会读到「已过期」——
     * 两件事对本人不一样（#94 复审 ST-5）。令牌表已进脊柱建表脚本，加列成本高于收益，
     * 而 remark 目前没人用，正好放这个标记。
     */
    private static final String REVOKED_MARK = "REVOKED";

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
        // 自填建档的两种形态：收货员给了 payeeId（已建档）就锁到那个人身上，否则绑定链接本身
        PublicTokenPurposeEnum.BusinessKeyType businessKeyType =
                purpose == PublicTokenPurposeEnum.ONBOARDING_WIZARD && reqVO.getPayeeId() != null
                        ? PublicTokenPurposeEnum.BusinessKeyType.PAYEE : purpose.getBusinessKeyType();
        String businessKey = resolveBusinessKey(purpose, reqVO);

        LocalDateTime expiresTime = LocalDateTime.now().plusHours(TOKEN_TTL_HOURS);
        String jti = UUID.randomUUID().toString().replace("-", "");
        PublicTokenPayload payload = PublicTokenPayload.builder()
                .jti(jti)
                .purpose(purpose.getCode())
                .tenantId(tenantId)
                .businessKey(businessKey)
                .businessKeyType(businessKeyType.name())
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
        // 先认「被作废」再认「已过期」：两种情况对本人是不同的提示（#94 复审 ST-5）
        if (REVOKED_MARK.equals(record.getRemark())) {
            throw exception(PUBLIC_TOKEN_REVOKED);
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
        // 验签只证明「这枚令牌是我们签的」；是否在库里、是否属于本租户、是否可作废由下面判定。
        PublicTokenPayload payload = publicTokenCodec.verify(token);
        IcbcPublicTokenDO record = publicTokenMapper.selectByJti(payload.getJti());
        // icbc_public_token 是全局表（在 ignore-tables 里），下面这些查询 / 更新**没有租户条件**，
        // 所以必须在 Java 侧收口：不属于当前租户就按「不存在」处理——不能回「无权」，那会泄露它存在（#94 评审 S-1）。
        if (record == null || !Objects.equals(record.getTenantId(), TenantContextHolder.getTenantId())) {
            throw exception(PUBLIC_TOKEN_NOT_FOUND);
        }
        // 用途收口：只有「转达给本人的邀请 / 实名链接」可作废，见 PublicTokenPurposeEnum#revocable。
        // 否则这个新端点会变成一把能掐断任何用途链接的万能钥匙（#94 评审 S-1）。
        PublicTokenPurposeEnum purpose = PublicTokenPurposeEnum.ofCode(record.getPurpose())
                .orElseThrow(() -> exception(PUBLIC_TOKEN_PURPOSE_MISMATCH));
        if (!purpose.isRevocable()) {
            throw exception(PUBLIC_TOKEN_PURPOSE_MISMATCH);
        }
        // 作废是幂等的：已经作废 / 已经过期的令牌本来就不可用，再点一次直接成功返回，不重复写库。
        if (REVOKED_MARK.equals(record.getRemark())
                || (record.getExpiresTime() != null && record.getExpiresTime().isBefore(LocalDateTime.now()))) {
            return;
        }
        // 作废 = 把有效期提前到现在 + 留一个可辨认的作废标记：
        // verify / redeem 据此回「已被作废」而不是「已过期」，本人看得懂发生了什么（#94 复审 ST-5）。
        // 不新增「作废」列：令牌表已进脊柱建表脚本，加列成本高于收益，remark 足够表达。
        IcbcPublicTokenDO update = new IcbcPublicTokenDO();
        update.setId(record.getId());
        update.setExpiresTime(LocalDateTime.now());
        update.setRemark(REVOKED_MARK);
        publicTokenMapper.updateById(update);
    }

    private String resolveBusinessKey(PublicTokenPurposeEnum purpose, PublicTokenCreateReqVO reqVO) {
        if (purpose.getBusinessKeyType() == PublicTokenPurposeEnum.BusinessKeyType.ONBOARDING_INVITE) {
            // 自填建档：给了 payeeId（已建档）就锁到那个人身上；没给（待建档）就绑定链接本身（#94 修票 ST-1）
            return reqVO.getPayeeId() != null
                    ? payeeBusinessKey(reqVO.getPayeeId())
                    : UUID.randomUUID().toString().replace("-", "");
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
        return payeeBusinessKey(reqVO.getPayeeId());
    }

    private String payeeBusinessKey(Long payeeId) {
        if (payeeId == null) {
            throw exception(PUBLIC_TOKEN_BUSINESS_KEY_MISSING);
        }
        if (payeeInfoMapper.selectById(payeeId) == null) {
            throw exception(PAYEE_NOT_EXISTS);
        }
        return payeeId.toString();
    }

}
