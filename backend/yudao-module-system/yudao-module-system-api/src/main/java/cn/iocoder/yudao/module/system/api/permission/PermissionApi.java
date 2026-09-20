package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.module.system.api.permission.dto.DeptDataPermissionRespDTO;

import java.util.Collection;
import java.util.Set;

/**
 * 权限 API 接口
 *
 * @author 芋道源码
 */
public interface PermissionApi {

    /**
     * 获得拥有多个角色的用户编号集合
     *
     * @param roleIds 角色编号集合
     * @return 用户编号集合
     */
    Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds);

    /**
     * 判断是否有权限，任一一个即可
     *
     * @param userId 用户编号
     * @param permissions 权限
     * @return 是否
     */
    boolean hasAnyPermissions(Long userId, String... permissions);

    /**
     * 判断是否有角色，任一一个即可
     *
     * @param userId 用户编号
     * @param roles 角色数组
     * @return 是否
     */
    boolean hasAnyRoles(Long userId, String... roles);

    /**
     * 给角色追加菜单授权（幂等：只新增，不删除已有授权）。
     *
     * <p>与 {@link cn.iocoder.yudao.module.system.service.permission.PermissionService#assignRoleMenu}
     * 的「全量覆盖」不同，这里用于业务模块把代码里定义的权限补给角色，不会把角色已有的菜单（如租户套餐
     * 带来的系统菜单）抹掉。
     *
     * @param roleId  角色编号
     * @param menuIds 菜单编号集合
     * @return 实际新增的授权数量
     */
    int addRoleMenus(Long roleId, Collection<Long> menuIds);

    /**
     * 获得登陆用户的部门数据权限
     *
     * @param userId 用户编号
     * @return 部门数据权限
     */
    DeptDataPermissionRespDTO getDeptDataPermission(Long userId);

}
