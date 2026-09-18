package cn.iocoder.yudao.module.icbc.controller.admin.entauth;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthInitReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthRespVO;
import cn.iocoder.yudao.module.icbc.service.entauth.IcbcEnterpriseAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 工行企业授权
 */
@Tag(name = "管理后台 - 工行企业授权")
@RestController
@RequestMapping("/icbc/enterprise-auth")
@Validated
public class IcbcEnterpriseAuthController {

    @Resource
    private IcbcEnterpriseAuthService enterpriseAuthService;

    @PostMapping("/init")
    @Operation(summary = "发起企业授权，返回工行授权页面表单 HTML")
    @PreAuthorize("@icbc.hasPermission('icbc:enterprise-auth:init')")
    public CommonResult<String> init(@Valid @RequestBody IcbcEnterpriseAuthInitReqVO reqVO) {
        return success(enterpriseAuthService.initEnterpriseAuth(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得企业授权记录分页")
    @PreAuthorize("@icbc.hasPermission('icbc:enterprise-auth:query')")
    public CommonResult<PageResult<IcbcEnterpriseAuthRespVO>> page(@Valid IcbcEnterpriseAuthPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(enterpriseAuthService.getEnterpriseAuthPage(pageReqVO),
                IcbcEnterpriseAuthRespVO.class));
    }

    @PutMapping("/update-status")
    @Operation(summary = "人工回填授权结果")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "authStatus", description = "0-未授权，1-已授权，2-已失效", required = true)
    @PreAuthorize("@icbc.hasPermission('icbc:enterprise-auth:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                              @RequestParam("authStatus") Integer authStatus) {
        enterpriseAuthService.updateAuthStatus(id, authStatus);
        return success(true);
    }

}
