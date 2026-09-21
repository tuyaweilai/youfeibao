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
 * <p><b>不记访问日志</b>（{@code requestEnable = false}）：令牌放在查询串里，而平台的
 * {@code ApiAccessLogFilter} 默认把 {@code query} 原样写进 {@code infra_api_access_log}。
 * 令牌是通往本人档案 / 签署链接的凭证，不该被记进日志表。
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
