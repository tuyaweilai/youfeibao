package cn.iocoder.yudao.module.logistics.enums;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 物流域的租户内角色，以及每个角色能做什么。
 *
 * <p><b>本枚举是权限归属的唯一来源</b>：{@code logistics} 侧 Controller 的
 * {@code @ss.hasPermission} 运行时走 yudao 原生的菜单权限判断，而菜单行（{@code system_menu}）
 * 与角色-菜单关联（{@code system_role_menu}）由 {@code LogisticsPermissionSyncService}
 * 依据本枚举幂等生成。新增权限时只需在 {@link LogisticsPermission} 登记、把它挂到这里的某个角色，
 * 再在 Controller 上写 {@code @ss.hasPermission}；不用另行维护菜单种子。
 *
 * <p>三个角色都在回收企业这一租户内，**没有跨租户角色**——物流不含平台运营视角
 * （与 icbc 的 {@code RecyclingRoleEnum} 不同，那边有一个平台运营角色）。
 *
 * <p>角色的 {@code code} 是 {@code system_role.code}。管理员直接复用 yudao 建租户时自动生成的
 * {@code tenant_admin}，这样新租户开出来就天然是管理员，不必额外造角色。
 *
 * <p><b>司机不进 PC 菜单</b>（ADR 0032）：他只用司机端，因此他的权限面在司机端接口上按动作收窄，
 * 而不是靠菜单可见性。
 */
public enum LogisticsRoleEnum {

    /**
     * 管理员：回收企业负责人，本租户内物流作业的最终责任人。
     */
    ADMIN("tenant_admin", "管理员", Set.of(
            LogisticsPermission.VEHICLE_CREATE, LogisticsPermission.VEHICLE_UPDATE,
            LogisticsPermission.VEHICLE_DELETE, LogisticsPermission.VEHICLE_QUERY,
            LogisticsPermission.VEHICLE_EXPORT,
            LogisticsPermission.DRIVER_CREATE, LogisticsPermission.DRIVER_UPDATE,
            LogisticsPermission.DRIVER_DELETE, LogisticsPermission.DRIVER_QUERY,
            LogisticsPermission.DRIVER_EXPORT,
            LogisticsPermission.TENANT_ROLE_INIT)),

    /**
     * 调度：安排车辆与运输任务的人。派车要能从档案里选到车与人，因此有车辆与司机的查询权；
     * 建档与删档归管理员。
     */
    DISPATCHER("logistics_dispatcher", "调度", Set.of(
            LogisticsPermission.VEHICLE_QUERY,
            LogisticsPermission.DRIVER_QUERY)),

    /**
     * 司机：把货从提货点运到场站并上报运输节点的人（自有司机与承运商司机同构）。
     *
     * <p>本票（V2a）还没有运输任务，所以这里**暂时没有权限**——司机角色先被建出来（租户初始化时
     * 就会拿到它），他的任务读取与节点上报权限随运输任务票（V2b #78）登记进来。
     */
    DRIVER("logistics_driver", "司机", Set.of());

    private final String code;
    private final String name;
    private final Set<String> permissions;

    LogisticsRoleEnum(String code, String name, Set<String> permissions) {
        this.code = code;
        this.name = name;
        this.permissions = permissions;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public static Optional<LogisticsRoleEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(role -> role.code.equals(code)).findFirst();
    }

    /**
     * 该角色是否拥有某个权限。
     */
    public static boolean roleHasPermission(String roleCode, String permission) {
        return ofCode(roleCode).map(role -> role.permissions.contains(permission)).orElse(false);
    }

    /**
     * 全部登记过的权限（去重，保持登记顺序）。同步服务按它建菜单权限行。
     */
    public static Set<String> allPermissions() {
        Set<String> permissions = new LinkedHashSet<>();
        for (LogisticsRoleEnum role : values()) {
            permissions.addAll(role.permissions);
        }
        return permissions;
    }

}
