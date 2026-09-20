package cn.iocoder.yudao.module.icbc.service.tenant;

import cn.iocoder.yudao.module.icbc.service.tenant.dto.RecyclingPermissionSyncResult;

/**
 * 回收域权限同步 Service 接口。
 *
 * <p>把 {@code RecyclingRoleEnum} 这份「角色 → 权限」的唯一来源，幂等地落成 yudao 原生的
 * {@code system_menu} 权限行、{@code system_role_menu} 角色授权，并把权限菜单补进「回收企业套餐」，
 * 从而让运行时可以统一走 {@code @ss.hasPermission}。
 */
public interface RecyclingPermissionSyncService {

    /**
     * 同步全局部分（权限行 + 租户套餐），不依赖租户上下文，可在启动时调用。
     *
     * <p>租户套餐补全后，新开的回收企业租户由建租户流程自动把权限授给 {@code tenant_admin}（管理员），
     * 不需要手工配菜单。
     *
     * @return 本次实际新增的数量
     */
    RecyclingPermissionSyncResult syncGlobal();

    /**
     * 同步全局部分 + 当前租户的角色授权（幂等，可重复调用）。
     *
     * @return 本次实际新增的数量；已同步时全为 0
     */
    RecyclingPermissionSyncResult sync();

}
