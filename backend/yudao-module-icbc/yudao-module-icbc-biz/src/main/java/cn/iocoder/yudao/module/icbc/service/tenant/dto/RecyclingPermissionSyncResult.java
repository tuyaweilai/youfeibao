package cn.iocoder.yudao.module.icbc.service.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 回收域权限同步结果。
 *
 * <p>每次同步返回本次实际新增的数量：重复执行（幂等）时四个计数都为 0，结果一致。
 */
@Getter
@AllArgsConstructor
public class RecyclingPermissionSyncResult {

    /**
     * 新建的 {@code system_menu} 权限行数量
     */
    private final int createdMenuCount;
    /**
     * 新建的 {@code system_role} 角色数量
     */
    private final int createdRoleCount;
    /**
     * 新增的 {@code system_role_menu} 授权数量
     */
    private final int assignedRoleMenuCount;
    /**
     * 补进租户套餐的菜单数量
     */
    private final int addedPackageMenuCount;

    /**
     * 是否本次没有任何变更（即已完全同步）。
     */
    public boolean isEmpty() {
        return createdMenuCount == 0 && createdRoleCount == 0
                && assignedRoleMenuCount == 0 && addedPackageMenuCount == 0;
    }

}
