package cn.iocoder.yudao.module.system.api.permission;

import java.util.Collection;
import java.util.Set;

/**
 * 菜单 API 接口
 *
 * <p>业务模块（例如反向开票）把「代码里写的权限标识」落成 {@code system_menu} 的按钮型权限行，
 * 从而让运行时可以用 yudao 原生的 {@code @ss.hasPermission} 判权，而不是各模块自建角色策略。
 *
 * <p>权限行还要能被人看懂：{@code system_menu} 是「菜单管理」页面的数据源，一行只有权限标识、
 * 没有中文名与父页面时，菜单树里就是一片 {@code logistics:transport-cost:delete} 这样的裸标识。
 * 因此这里提供 {@link #getMenuIdByComponentName(String)} 与
 * {@link #ensurePermissionMenu(String, String, Long, Integer)}：让业务模块用自己的语言
 * （中文名 + 挂在哪个页面的组件名）把权限行归集到页面下。
 *
 * @author 芋道源码
 */
public interface MenuApi {

    /**
     * 按权限标识获得菜单编号。
     *
     * <p>同一个权限标识可能对应多个菜单行（例如同一权限挂在多个页面按钮上），返回其中一个即可；
     * 权限判断只看「角色是否拥有任一该权限的菜单」，所以一个足够。
     *
     * @param permission 权限标识
     * @return 菜单编号；不存在时返回 {@code null}
     */
    Long getMenuIdByPermission(String permission);

    /**
     * 按组件名获得菜单编号。
     *
     * <p>业务模块用它找到「权限要挂到哪个页面」：页面菜单的 id 不是稳定值（自增），而组件名是菜单种子里写死的。
     * 组件名在走 {@code MenuService} 的校验时全局唯一，但菜单 SQL 直接插库可以绕过校验（实测
     * {@code ErpStock} / {@code ErpSupplier} 各两行：已停用的 ERP 那一棵与 icbc 页面各一行），
     * 所以这里取**自身与所有祖先都启用**的那一个（停用只在树根上标，看 status 看不出来）。
     *
     * @param componentName 组件名（例如 {@code LogisticsVehicle}）
     * @return 菜单编号；不存在时返回 {@code null}
     */
    Long getMenuIdByComponentName(String componentName);

    /**
     * 确保一个按钮型权限菜单存在，并把它归集到给定的名称、父菜单与排序。
     *
     * <p>三种情况：
     * <ol>
     *   <li>权限标识还没有菜单行 → 按参数建行（{@code parentId} 传 {@code null} 表示挂在根节点）；</li>
     *   <li>已有菜单行、但仍是「未归集」的形态（{@code name} 等于权限标识且挂在根节点）→ 就地改成
     *       给定的名称与父菜单，**行 id 不变**，因此角色-菜单授权与租户套餐不受影响；</li>
     *   <li>已有菜单行且已经归集过（或由菜单 SQL / 后台人工编排）→ **一律不动**。</li>
     * </ol>
     *
     * <p>第 3 条是刻意的：{@code icbc-menu.sql} 里同一个权限会挂在多个按钮上（如
     * {@code icbc:purchase-order:manage} 有 8 行），菜单名称也是人工推敲过的；同步过程只能
     * **修正自己落下的裸标识行**，不能反过来把 SQL 或后台编排的结果改坏。代价是归集过一次的行不再
     * 跟随参数变化——要换位置由该域自己的菜单 SQL 重建（那本来也是菜单结构变更的入口）。
     *
     * @param name       菜单名称（中文名，例如「车辆新增」）
     * @param permission 权限标识
     * @param parentId   父菜单编号；{@code null} 表示根节点
     * @param sort       排序；{@code null} 视为 0
     * @return 菜单编号
     */
    Long ensurePermissionMenu(String name, String permission, Long parentId, Integer sort);

    /**
     * 收集这些菜单的**全部祖先菜单**编号（不含入参自身）。
     *
     * <p>角色授权时必须把父链一起挂上：{@code /system/auth/get-permission-info} 会调
     * {@code MenuService#filterDisableMenus}，它沿父链判断「是否自身与所有祖先是启用」。父菜单不在
     * 角色已授权的菜单集合里时，按钮型权限行会被判为「禁用」而从权限列表里掉出去——表现为
     * 后端 {@code @ss.hasPermission} 放行、前端却提示没有权限。
     *
     * <p>已到根节点（{@code parentId = 0}）或链上有环时停止；菜单不存在则跳过。
     *
     * @param menuIds 菜单编号集合
     * @return 祖先菜单编号（去重）；入参为空时返回空集合
     */
    Set<Long> getAncestorMenuIds(Collection<Long> menuIds);

}
