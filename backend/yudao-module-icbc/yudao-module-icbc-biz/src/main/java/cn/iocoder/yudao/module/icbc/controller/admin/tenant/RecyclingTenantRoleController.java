package cn.iocoder.yudao.module.icbc.controller.admin.tenant;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.tenant.RecyclingPermissionSyncService;
import cn.iocoder.yudao.module.icbc.service.tenant.dto.RecyclingPermissionSyncResult;
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
 * 管理后台 - 回收域权限同步。
 *
 * <p>把 {@code RecyclingRoleEnum} 幂等落成菜单权限行、角色授权与租户套餐。超管可调用，
 * 用于初始化 / 修复本租户的回收域权限。
 */
@Tag(name = "管理后台 - 回收域权限同步")
@RestController
@RequestMapping("/icbc/tenant/role")
@Validated
public class RecyclingTenantRoleController {

    @Resource
    private RecyclingPermissionSyncService recyclingPermissionSyncService;

    @PostMapping("/init")
    @Operation(summary = "同步本租户的回收域角色与权限（幂等）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.TENANT_ROLE_INIT + "')")
    public CommonResult<RecyclingPermissionSyncResult> sync() {
        return success(recyclingPermissionSyncService.sync());
    }

}
