package cn.iocoder.yudao.module.icbc.controller.admin.esign;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignActivateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignOpenConsoleRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignTenantStatusRespVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.esign.EsignTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 回收企业：电子签章开通（#92，ADR 0036）。
 *
 * <p>章是**租户级**的：回收企业才是发起方，平台不代盖。管理员在本租户点「开通电子签」，
 * 拿一枚一次性控制台链接去完成企业认证与创建企业印章，回来确认后后台显示「已激活 + 印章就位」。
 *
 * <p>开通状态不影响建档（ADR 0036 决策 7）：未开通时签章不可用、协议落 {@code PAPER}，其余流程照常。
 */
@Tag(name = "管理后台 - 回收企业：电子签章开通")
@RestController
@RequestMapping("/icbc/esign")
@Validated
public class EsignController {

    @Resource
    private EsignTenantService esignTenantService;

    @GetMapping("/status")
    @Operation(summary = "查看本企业电子签章状态与合同额度")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ESIGN_QUERY + "')")
    public CommonResult<EsignTenantStatusRespVO> getStatus() {
        return success(esignTenantService.getStatus());
    }

    @PostMapping("/open")
    @Operation(summary = "开通电子签章：取一次性控制台链接",
            description = "在第三方控制台完成企业认证与创建企业印章；再次调用会换一枚新链接（旧链接作废）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ESIGN_MANAGE + "')")
    public CommonResult<EsignOpenConsoleRespVO> openConsole() {
        return success(esignTenantService.openConsole());
    }

    @PostMapping("/activate")
    @Operation(summary = "确认电子签章已激活",
            description = "企业认证通过且企业印章已创建后调用；缺印章编号会被拒绝——「已激活 + 印章就位」是两件事")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ESIGN_MANAGE + "')")
    public CommonResult<Boolean> activate(@Valid @RequestBody EsignActivateReqVO reqVO) {
        esignTenantService.activate(reqVO);
        return success(true);
    }

}
