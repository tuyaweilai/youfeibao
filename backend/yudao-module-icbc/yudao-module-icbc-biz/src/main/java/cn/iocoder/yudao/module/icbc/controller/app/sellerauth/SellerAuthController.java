package cn.iocoder.yudao.module.icbc.controller.app.sellerauth;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.config.SecurityProperties;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo.*;
import cn.iocoder.yudao.module.icbc.service.sellerauth.SellerAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 自然人出售者 - 登录与身份绑定。
 *
 * <p>挂在 {@code /app-api} 下：令牌的用户类型是会员（{@code UserTypeEnum.MEMBER}），这是 yudao 里
 * 自然人身份的既有通道。登录与验证码是公开的（{@code @PermitAll}），其余需要登录态。
 *
 * <p>为什么要显式绑身份：他扫码时我们只知道「哪家企业的哪张单」，登录后要把「这笔货的出售者」绑到
 * 当前这个登录凭证上；同一身份证已绑别的手机号时**拒绝、不合并**（ADR 0017）。
 */
@Tag(name = "自然人出售者 - 登录与身份绑定")
@RestController
@RequestMapping("/icbc/seller/auth")
@Validated
@Slf4j
public class SellerAuthController {

    @Resource
    private SellerAuthService sellerAuthService;
    @Resource
    private SecurityProperties securityProperties;

    @PostMapping("/sms-send")
    @PermitAll
    @Operation(summary = "发送登录短信验证码")
    public CommonResult<Boolean> sendSmsCode(@Valid @RequestBody SellerSmsSendReqVO reqVO) {
        sellerAuthService.sendSmsCode(reqVO.getMobile());
        return success(true);
    }

    @PostMapping("/sms-login")
    @PermitAll
    @Operation(summary = "手机号 + 短信验证码登录", description = "验证码通过后按手机号取或建登录凭证（落在平台租户）")
    public CommonResult<SellerLoginRespVO> smsLogin(@Valid @RequestBody SellerSmsLoginReqVO reqVO) {
        return success(sellerAuthService.smsLogin(reqVO));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "只注销当前登录凭证，不影响自然人主体与交易记录")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        sellerAuthService.logout(token);
        return success(true);
    }

    @GetMapping("/subjects")
    @Operation(summary = "当前登录名下的自然人主体", description = "一个登录可以挂多个主体（子女代老人操作）")
    public CommonResult<List<SellerSubjectRespVO>> listSubjects() {
        return success(sellerAuthService.listSubjects());
    }

    @PostMapping("/subjects/bind-by-mobile")
    @Operation(summary = "按当前租户 + 登录手机号自动匹配并绑定自然人主体",
            description = "他扫场站二维码进来时没有 payeeId；用登录手机号在本租户找收方档案。匹配不到返回空列表（前端给空态）")
    public CommonResult<List<SellerSubjectRespVO>> bindByLoginMobile() {
        return success(sellerAuthService.bindByLoginMobile());
    }

    @PostMapping("/subjects/bind")
    @Operation(summary = "绑定本次交易涉及的收方档案对应的自然人主体",
            description = "租户来自请求头 tenant-id（他扫码的场站所属回收企业）；手机号与身份登记不一致时拒绝，不合并")
    @Parameter(name = "payeeId", description = "本租户内的收方档案编号", required = true)
    public CommonResult<SellerSubjectRespVO> bindSubject(@Valid @RequestBody SellerSubjectBindReqVO reqVO) {
        return success(sellerAuthService.bindSubject(reqVO.getPayeeId()));
    }

    @PostMapping("/subjects/unbind")
    @Operation(summary = "解绑自然人主体", description = "只影响登录凭证，主体与交易记录永久保留")
    public CommonResult<Boolean> unbindSubject(@Valid @RequestBody SellerSubjectUnbindReqVO reqVO) {
        sellerAuthService.unbindSubject(reqVO.getNaturalPersonId());
        return success(true);
    }

}
