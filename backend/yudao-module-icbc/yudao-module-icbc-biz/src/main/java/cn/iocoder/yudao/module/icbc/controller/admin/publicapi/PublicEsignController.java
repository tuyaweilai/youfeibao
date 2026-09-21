package cn.iocoder.yudao.module.icbc.controller.admin.publicapi;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicAgreementSignRespVO;
import cn.iocoder.yudao.module.icbc.service.publicapi.PublicEsignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 公开端点 - 合同组签署（#95，ADR 0036）。
 *
 * <p>自然人在自己的页面上点「去签署」：拿建档令牌换一枚现生成现用的签署链接。
 * URL 落在既有的 {@code /admin-api/icbc/public/**} 白名单里（免登录、不带 tenant-id，
 * 租户由令牌解析）。用 POST 而不是 GET：生成链接会消耗令牌次数并让第三方现签一枚链接，
 * 不能被浏览器 / 网关预取。
 *
 * <p><b>不记访问日志</b>（{@code requestEnable = false}）：令牌放在查询串里，显式关掉请求参数记录，
 * 不让令牌有落进 {@code infra_api_access_log} 的机会。框架当前的 {@code ApiAccessLogFilter} 恰好
 * 会把查询串里的 {@code token} 脱敏、且 {@code setRequestUrl} 不含查询串，所以这不是当下唯一的
 * 防线；但接口契约不该依赖框架内部的两个巧合（脱敏键集合 + 是否带查询串），这里照样显式声明，
 * 将来框架调整也不会把令牌捅到日志表里。
 */
@Tag(name = "公开端点 - 合同组签署")
@RestController
@RequestMapping("/icbc/public/agreement")
@Validated
@Slf4j
public class PublicEsignController {

    @Resource
    private PublicEsignService publicEsignService;

    @PostMapping("/sign-url")
    @Operation(summary = "用建档令牌现生成合同组签署链接（现生成现用，不存不复用）")
    @ApiAccessLog(requestEnable = false)
    @Parameter(name = "token", description = "公开令牌", required = true)
    public CommonResult<PublicAgreementSignRespVO> signUrl(@RequestParam("token") String token) {
        return success(publicEsignService.createSignUrl(token));
    }

}
