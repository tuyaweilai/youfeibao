package cn.iocoder.yudao.module.system.api.permission;

import java.util.Collection;

/**
 * 角色 API 接口
 *
 * @author 芋道源码
 */
public interface RoleApi {

    /**
     * 校验角色们是否有效。如下情况，视为无效：
     * 1. 角色编号不存在
     * 2. 角色被禁用
     *
     * @param ids 角色编号数组
     */
    void validRoleList(Collection<Long> ids);

    /**
     * 根据角色标识，获得角色编号
     *
     * @param code 角色标识
     * @return 角色编号；不存在时返回 {@code null}
     */
    Long getRoleIdByCode(String code);

    /**
     * 创建一个不含菜单授权的内置角色。
     *
     * <p>只用于业务模块初始化自己的固定角色（例如反向开票的收货员 / 开票员 / 财务），
     * 这类角色的权限不走菜单，而由业务模块自己的权限判断维护。
     *
     * @param code 角色标识
     * @param name 角色名称
     * @return 角色编号
     */
    Long createRole(String code, String name);

}
