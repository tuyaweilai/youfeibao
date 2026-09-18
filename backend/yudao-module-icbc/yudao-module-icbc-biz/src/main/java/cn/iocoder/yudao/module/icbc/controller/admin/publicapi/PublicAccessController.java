package cn.iocoder.yudao.module.icbc.controller.admin.publicapi;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicContactLeadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicQuotaRespVO;
import cn.iocoder.yudao.module.icbc.service.publicapi.PublicAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 公开端点 - 自然人免登录。
 *
 * <p>本控制器的 URL 已登记进安全白名单与 {@code yudao.tenant.ignore-urls}：没有登录态、
 * 也不带 {@code tenant-id} 请求头。租户由令牌解析出来，见 {@code PublicAccessServiceImpl}。
 */
@Tag(name = "公开端点 - 自然人免登录")
@RestController
@RequestMapping("/icbc/public")
@Validated
@Slf4j
public class PublicAccessController {

    @Resource
    private PublicAccessService publicAccessService;

    @GetMapping("/invoice/download")
    @Operation(summary = "用令牌下载发票 PDF")
    @Parameter(name = "token", description = "公开令牌", required = true)
    public void downloadInvoice(@RequestParam("token") String token, HttpServletResponse response) {
        publicAccessService.downloadInvoicePdf(token, response);
    }

    @PostMapping("/contact-lead")
    @Operation(summary = "提交收方入驻失败后的联系方式")
    public CommonResult<Boolean> submitContactLead(@Valid @RequestBody PublicContactLeadReqVO reqVO) {
        publicAccessService.submitContactLead(reqVO);
        return success(true);
    }

    @GetMapping("/quota")
    @Operation(summary = "查询自然人滚动额度余量")
    @Parameter(name = "token", description = "公开令牌", required = true)
    public CommonResult<PublicQuotaRespVO> queryQuota(@RequestParam("token") String token) {
        return success(publicAccessService.queryQuota(token));
    }

}
