package cn.iocoder.yudao.module.system.api.permission;

/**
 * 菜单 API 接口
 *
 * <p>业务模块（例如反向开票）把「代码里写的权限标识」落成 {@code system_menu} 的按钮型权限行，
 * 从而让运行时可以用 yudao 原生的 {@code @ss.hasPermission} 判权，而不是各模块自建角色策略。
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
     * 创建一个按钮型的权限菜单（父菜单为根节点、启用状态）。
     *
     * <p>只在权限标识还没有对应菜单时调用，用于给新登记的权限兜底建行。
     *
     * @param name       菜单名称
     * @param permission 权限标识
     * @return 菜单编号
     */
    Long createPermissionMenu(String name, String permission);

}
