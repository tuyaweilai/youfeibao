package cn.iocoder.yudao.module.icbc.controller.admin.entauth;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthInitReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthUpdateReqVO;
import cn.iocoder.yudao.module.icbc.service.entauth.IcbcEnterpriseAuthService;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import io.swagger.v3.oas.annotations.Operation;
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
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ENTERPRISE_AUTH_INIT + "')")
    public CommonResult<String> init(@Valid @RequestBody IcbcEnterpriseAuthInitReqVO reqVO) {
        return success(enterpriseAuthService.initEnterpriseAuth(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得企业授权记录分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ENTERPRISE_AUTH_QUERY + "')")
    public CommonResult<PageResult<IcbcEnterpriseAuthRespVO>> page(@Valid IcbcEnterpriseAuthPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(enterpriseAuthService.getEnterpriseAuthPage(pageReqVO),
                IcbcEnterpriseAuthRespVO.class));
    }

    @PutMapping("/update-result")
    @Operation(summary = "人工回填授权结果与授权有效期")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ENTERPRISE_AUTH_UPDATE + "')")
    public CommonResult<Boolean> updateResult(@Valid @RequestBody IcbcEnterpriseAuthUpdateReqVO reqVO) {
        enterpriseAuthService.updateAuthResult(reqVO);
        return success(true);
    }

}
