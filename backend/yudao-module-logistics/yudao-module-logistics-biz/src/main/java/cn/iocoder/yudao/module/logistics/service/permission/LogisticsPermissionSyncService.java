package cn.iocoder.yudao.module.logistics.service.permission;

import cn.iocoder.yudao.module.logistics.service.permission.dto.LogisticsPermissionSyncResult;

/**
 * 物流域权限同步 Service（V2a #77，ADR 0026 的物流侧实现）。
 *
 * <p>把 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsRoleEnum} 幂等地落成
 * {@code system_menu} 的权限行、角色与角色-菜单关联、以及租户套餐的菜单集合。
 *
 * <p><b>不依赖 icbc</b>（ADR 0032）：物流有一套自己的权限常量与角色枚举，与回收域各建各的。
 * 同一个人可以同时是收货员与调度，那是租户内的角色分配问题，不是权限模型问题。
 */
public interface LogisticsPermissionSyncService {

    /**
     * 同步**全局**部分：权限行 + 「回收企业套餐」的菜单集合。不碰租户内的角色授权
     * （那部分需要租户上下文）。
     *
     * @return 同步结果计数
     */
    LogisticsPermissionSyncResult syncGlobal();

    /**
     * 同步本租户的角色与授权：三个租户内角色各拿到枚举里定义的权限。
     *
     * @return 同步结果计数
     */
    LogisticsPermissionSyncResult sync();

}
