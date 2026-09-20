package cn.iocoder.yudao.module.logistics.controller.admin.permission;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.service.permission.LogisticsPermissionSyncService;
import cn.iocoder.yudao.module.logistics.service.permission.dto.LogisticsPermissionSyncResult;
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
 * 管理后台 - 物流域权限同步（V2a #77）。
 *
 * <p>把 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsRoleEnum} 幂等落成菜单权限行、
 * 角色授权与租户套餐。用于初始化 / 修复**本租户**的物流域权限。
 *
 * <p><b>引导顺序（与 icbc 同构）</b>：权限行由开机 runner 全局创建，但角色与角色-菜单是租户内的。
 * 第一次调用通常由平台超管发起（新租户开箱即得靠租户套餐，已有租户靠这个接口补）。
 */
@Tag(name = "管理后台 - 物流域权限同步")
@RestController
@RequestMapping("/logistics/permission")
@Validated
public class LogisticsPermissionController {

    @Resource
    private LogisticsPermissionSyncService logisticsPermissionSyncService;

    @PostMapping("/init")
    @Operation(summary = "同步本租户的物流域角色与权限（幂等）")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TENANT_ROLE_INIT + "')")
    public CommonResult<LogisticsPermissionSyncResult> sync() {
        return success(logisticsPermissionSyncService.sync());
    }

}
