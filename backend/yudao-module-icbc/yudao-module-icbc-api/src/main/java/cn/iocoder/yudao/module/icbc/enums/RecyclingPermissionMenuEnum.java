package cn.iocoder.yudao.module.icbc.enums;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 回收域「菜单 SQL 里没有编排」的权限行的归集方式：权限标识 → 中文名 / 父页面 / 排序。
 *
 * <p><b>为什么只有几条</b>：回收域的按钮行绝大部分由 {@code icbc-menu.sql} 显式编排（带中文名与父页面），
 * 那是本域菜单的单一来源。但权限清单会先于菜单 SQL 长出来——新加的权限若没同步进 SQL，
 * {@code RecyclingPermissionSyncService} 就会在开机时给它落一个「裸权限标识 + 挂根节点」的行，
 * 在「菜单管理」里显示成 {@code icbc:expiry-warning:query} 这样的名字。本枚举正是给这类漏网的权限补上说法，
 * 让它们在下次开机时就地归集（行 id 不变，角色授权与租户套餐不受影响）。
 *
 * <p><b>与菜单 SQL 的分工要有测试守住</b>（{@code RecyclingPermissionMenuEnumTest}）：这里登记的权限
 * 必须**不在** {@code icbc-menu.sql} 里，SQL 里没有的权限必须**全在**这里——两张表重叠或漏项都会让
 * 「同一个权限两套说法」重新长出来。
 *
 * <p>{@code parentComponentName} 是页面菜单的组件名（{@code system_menu.component_name}），
 * {@code null} 表示没有对应页面（初始化 / 接口级权限），留在根节点。
 */
public enum RecyclingPermissionMenuEnum {

    // ========== 开票就绪自检页（component_name = IcbcReadiness） ==========
    // 到期提醒与就绪自检同屏（views/icbc/readiness/index.vue 里的 ackWarning）
    EXPIRY_WARNING_QUERY(RecyclingPermission.EXPIRY_WARNING_QUERY, "到期提醒查询", "IcbcReadiness", 1),
    EXPIRY_WARNING_ACK(RecyclingPermission.EXPIRY_WARNING_ACK, "确认到期提醒", "IcbcReadiness", 2),

    // ========== 库存查询页（component_name = ErpStock） ==========
    // 口径就绪提示显示在库存查询页顶部（views/erp/stock/stock/index.vue）
    STOCK_READINESS_QUERY(RecyclingPermission.STOCK_READINESS_QUERY, "库存口径就绪查询", "ErpStock", 2),

    // ========== 没有对应页面的权限 ==========
    // 「API 日志」整棵已从回收企业套餐收回（#102，见 icbc-api-log-menu-revoke.sql），
    // 这条只剩平台运营在用；跨租户发票查询也只有接口没有页面。挂根节点即可——
    // 按钮型行不进左侧导航，不会多出菜单项。
    API_LOG_QUERY(RecyclingPermission.API_LOG_QUERY, "工行调用日志查询", null, 1),
    TENANT_ROLE_INIT(RecyclingPermission.TENANT_ROLE_INIT, "回收域权限初始化", null, 2),
    PLATFORM_INVOICE_QUERY(RecyclingPermission.PLATFORM_INVOICE_QUERY, "跨租户发票查询", null, 3);

    private final String permission;
    private final String name;
    private final String parentComponentName;
    private final int sort;

    RecyclingPermissionMenuEnum(String permission, String name, String parentComponentName, int sort) {
        this.permission = permission;
        this.name = name;
        this.parentComponentName = parentComponentName;
        this.sort = sort;
    }

    public String getPermission() {
        return permission;
    }

    public String getName() {
        return name;
    }

    /**
     * 父页面菜单的组件名；{@code null} 表示挂在根节点。
     */
    public String getParentComponentName() {
        return parentComponentName;
    }

    public int getSort() {
        return sort;
    }

    private static final Map<String, RecyclingPermissionMenuEnum> BY_PERMISSION;

    static {
        Map<String, RecyclingPermissionMenuEnum> byPermission = new LinkedHashMap<>();
        for (RecyclingPermissionMenuEnum item : values()) {
            byPermission.put(item.permission, item);
        }
        BY_PERMISSION = Collections.unmodifiableMap(byPermission);
    }

    /**
     * 按权限标识找到它的归集方式。
     *
     * @param permission 权限标识
     * @return 归集方式；没登记时返回 {@code null}
     */
    public static RecyclingPermissionMenuEnum ofPermission(String permission) {
        return BY_PERMISSION.get(permission);
    }

}
