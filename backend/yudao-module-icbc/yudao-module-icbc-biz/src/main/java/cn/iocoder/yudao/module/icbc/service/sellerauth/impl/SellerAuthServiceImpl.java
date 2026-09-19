package cn.iocoder.yudao.module.icbc.service.sellerauth.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.TerminalEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo.SellerLoginRespVO;
import cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo.SellerSmsLoginReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo.SellerSubjectRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.impl.NaturalPersonServiceImpl;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.icbc.service.sellerauth.SellerAuthService;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import cn.iocoder.yudao.module.system.api.oauth2.OAuth2TokenApi;
import cn.iocoder.yudao.module.system.api.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import cn.iocoder.yudao.module.system.api.oauth2.dto.OAuth2AccessTokenRespDTO;
import cn.iocoder.yudao.module.system.api.sms.SmsCodeApi;
import cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.iocoder.yudao.module.system.enums.oauth2.OAuth2ClientConstants;
import cn.iocoder.yudao.module.system.enums.sms.SmsSceneEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.concurrent.Callable;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 自然人出售者登录 Service 实现。
 *
 * <p>三件事刻意分开（见 ADR 0017）：
 * <ol>
 *   <li><b>登录凭证</b>（会员用户）落在**平台租户**：一个手机号一个凭证，不随回收企业重复建；</li>
 *   <li><b>身份锚点</b>（自然人主体）记身份证，与凭证多对多；</li>
 *   <li><b>收方档案</b>（企业 × 自然人）留在租户内，绑定时按请求头租户 + 手机号一致性校验。</li>
 * </ol>
 */
@Service
@Validated
@Slf4j
public class SellerAuthServiceImpl implements SellerAuthService {

    /**
     * 登录凭证所属的平台租户。0 = 平台级，与 {@code icbc_natural_person} 这些全局表同一口径。
     */
    @Value("${icbc.seller.platform-tenant-id:0}")
    private Long platformTenantId;

    @Resource
    private MemberUserApi memberUserApi;
    @Resource
    private OAuth2TokenApi oauth2TokenApi;
    @Resource
    private SmsCodeApi smsCodeApi;
    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PayeeInfoService payeeInfoService;

    @Override
    public void sendSmsCode(String mobile) {
        String ip = getClientIP();
        SmsCodeSendReqDTO reqDTO = new SmsCodeSendReqDTO();
        reqDTO.setMobile(mobile);
        reqDTO.setScene(SmsSceneEnum.MEMBER_LOGIN.getScene());
        reqDTO.setCreateIp(ip);
        inPlatformTenant(() -> {
            smsCodeApi.sendSmsCode(reqDTO);
            return null;
        });
    }

    @Override
    public SellerLoginRespVO smsLogin(@Valid SellerSmsLoginReqVO reqVO) {
        String ip = getClientIP();
        MemberUserRespDTO user = inPlatformTenant(() -> {
            // 先校验验证码：错码直接抛，不建任何凭证
            SmsCodeUseReqDTO useReqDTO = new SmsCodeUseReqDTO();
            useReqDTO.setMobile(reqVO.getMobile());
            useReqDTO.setCode(reqVO.getCode());
            useReqDTO.setScene(SmsSceneEnum.MEMBER_LOGIN.getScene());
            useReqDTO.setUsedIp(ip);
            smsCodeApi.useSmsCode(useReqDTO);
            return memberUserApi.createUserIfAbsent(reqVO.getMobile(), ip, TerminalEnum.H5.getTerminal());
        });

        OAuth2AccessTokenCreateReqDTO tokenReqDTO = new OAuth2AccessTokenCreateReqDTO();
        tokenReqDTO.setUserId(user.getId());
        tokenReqDTO.setUserType(UserTypeEnum.MEMBER.getValue());
        tokenReqDTO.setClientId(OAuth2ClientConstants.CLIENT_ID_DEFAULT);
        OAuth2AccessTokenRespDTO token = oauth2TokenApi.createAccessToken(tokenReqDTO);

        SellerLoginRespVO resp = new SellerLoginRespVO();
        resp.setAccessToken(token.getAccessToken());
        resp.setRefreshToken(token.getRefreshToken());
        resp.setExpiresTime(token.getExpiresTime());
        resp.setSubjects(toSubjectList(naturalPersonService.getNaturalPersonListByMemberUserId(user.getId())));
        return resp;
    }

    @Override
    public void logout(String token) {
        if (StrUtil.isNotBlank(token)) {
            oauth2TokenApi.removeAccessToken(token);
        }
    }

    @Override
    public List<SellerSubjectRespVO> listSubjects() {
        return toSubjectList(naturalPersonService.getNaturalPersonListByMemberUserId(loginMemberUserId()));
    }

    @Override
    public SellerSubjectRespVO bindSubject(Long payeeId) {
        Long memberUserId = loginMemberUserId();
        // 收方档案是租户级的，租户来自请求头（他扫码的那个场站所属回收企业）。
        // 缺了它必须直接拒绝：宁可报错，也不能在没有租户条件的情况下去读别人的档案。
        if (TenantContextHolder.getTenantId() == null) {
            throw exception(SELLER_STATION_TENANT_REQUIRED);
        }
        // 凭证在**平台租户**，而当前请求带的是扫码那家企业的租户；必须回到平台租户去读，
        // 否则查不到凭证、登录手机号为空，下面的「手机号与身份登记不一致就拒绝」会被静默跳过。
        MemberUserRespDTO credential = inPlatformTenant(() -> memberUserApi.getUser(memberUserId));
        String loginMobile = credential != null ? credential.getMobile() : null;

        PayeeInfoDO payee = payeeId == null ? null : payeeInfoService.getPayeeInfo(payeeId);
        if (payee == null) {
            throw exception(PAYEE_NOT_EXISTS);
        }
        IcbcNaturalPersonDO person = payeeInfoService.ensureNaturalPerson(payee);
        // 同一身份已登记别的手机号：拒绝、不合并，让他用原手机号登录或联系客服
        if (StrUtil.isNotBlank(person.getMobile()) && StrUtil.isNotBlank(loginMobile)
                && !person.getMobile().equals(loginMobile)) {
            throw exception(NATURAL_PERSON_IDENTITY_TAKEN);
        }
        naturalPersonService.bindLogin(person.getId(), memberUserId,
                NaturalPersonServiceImpl.SOURCE_REGISTER, null);
        return toSubject(person);
    }

    @Override
    public void unbindSubject(Long naturalPersonId) {
        // 只解绑凭证：身份与交易记录不删（ADR 0017）
        naturalPersonService.unbindLogin(naturalPersonId, loginMemberUserId());
    }

    @Override
    public void assertBound(Long naturalPersonId) {
        // 一个登录名下可能挂着多个主体（子女代老人操作），所以每次都要求显式指定并校验
        if (!naturalPersonService.isBoundToLogin(naturalPersonId, loginMemberUserId())) {
            throw exception(NATURAL_PERSON_NOT_BOUND_TO_LOGIN);
        }
    }

    // ==================== 内部方法 ====================

    private Long loginMemberUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(NATURAL_PERSON_NOT_BOUND_TO_LOGIN);
        }
        return userId;
    }

    private List<SellerSubjectRespVO> toSubjectList(List<IcbcNaturalPersonDO> persons) {
        return persons.stream().map(this::toSubject).toList();
    }

    private SellerSubjectRespVO toSubject(IcbcNaturalPersonDO person) {
        SellerSubjectRespVO resp = new SellerSubjectRespVO();
        resp.setNaturalPersonId(person.getId());
        resp.setName(person.getName());
        resp.setIdCardNo(MaskUtils.maskIdCard(person.getIdCardNo()));
        resp.setMobile(MaskUtils.maskMobile(person.getMobile()));
        resp.setRealNameStatus(person.getRealNameStatus());
        PayeeRealNameStatusEnum realName = PayeeRealNameStatusEnum.of(person.getRealNameStatus());
        resp.setRealNameStatusName(realName != null ? realName.getName() : null);
        return resp;
    }

    /**
     * 在平台租户下执行：登录凭证与验证码都属于平台级，不能跟着请求头里的回收企业租户走。
     */
    private <T> T inPlatformTenant(Callable<T> callable) {
        return TenantUtils.execute(platformTenantId, callable);
    }

}
