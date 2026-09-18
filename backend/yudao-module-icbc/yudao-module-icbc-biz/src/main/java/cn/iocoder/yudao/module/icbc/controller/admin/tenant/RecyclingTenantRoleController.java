package cn.iocoder.yudao.module.icbc.controller.admin.tenant;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.service.tenant.RecyclingTenantRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 回收企业租户初始化。
 */
@Tag(name = "管理后台 - 回收企业租户初始化")
@RestController
@RequestMapping("/icbc/tenant/role")
@Validated
public class RecyclingTenantRoleController {

    @Resource
    private RecyclingTenantRoleService recyclingTenantRoleService;

    @PostMapping("/init")
    @Operation(summary = "补齐本租户的反向开票内置角色")
    @PreAuthorize("@icbc.hasPermission('icbc:tenant:role:init')")
    public CommonResult<Boolean> initTenantRoles() {
        recyclingTenantRoleService.initTenantRoles();
        return success(true);
    }

}
