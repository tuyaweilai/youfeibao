package cn.iocoder.yudao.module.logistics.enums;

/**
 * 物流域的权限标识。
 *
 * <p>权限标识是本平台自己的语言，不直接等于后端接口名。它们用于 Controller 上的
 * {@code @ss.hasPermission}（运行时走 yudao 原生的菜单权限判断），并作为
 * {@link LogisticsRoleEnum} 里「哪个角色能做什么」的映射。
 *
 * <p>{@link LogisticsRoleEnum} 会依据本接口的常量幂等生成 {@code system_menu} 的权限行与
 * {@code system_role_menu} 关联（见 {@code LogisticsPermissionSyncService}），所以新增操作时：
 * 先在这里登记常量，再挂到 {@link LogisticsRoleEnum} 的相应角色，最后在 Controller 上用常量。
 *
 * <p>与 icbc 的关系：物流不依赖 icbc（ADR 0032），因此**不共用** {@code RecyclingPermission}。
 * 两边的权限串前缀不同（{@code logistics:} / {@code icbc:}），角色也是各建各的；
 * 同一个人可以同时是收货员与调度，那是租户内的角色分配问题，不是权限模型问题。
 */
public interface LogisticsPermission {

    // ========== 车辆档案 ==========

    /** 新增车辆 */
    String VEHICLE_CREATE = "logistics:vehicle:create";
    /** 修改车辆 */
    String VEHICLE_UPDATE = "logistics:vehicle:update";
    /** 删除车辆 */
    String VEHICLE_DELETE = "logistics:vehicle:delete";
    /** 查询车辆（派车选车也要这个权限） */
    String VEHICLE_QUERY = "logistics:vehicle:query";
    /** 导出车辆 */
    String VEHICLE_EXPORT = "logistics:vehicle:export";

    // ========== 司机档案 ==========

    /** 新增司机 */
    String DRIVER_CREATE = "logistics:driver:create";
    /** 修改司机 */
    String DRIVER_UPDATE = "logistics:driver:update";
    /** 删除司机 */
    String DRIVER_DELETE = "logistics:driver:delete";
    /** 查询司机（派车选人也要这个权限） */
    String DRIVER_QUERY = "logistics:driver:query";
    /** 导出司机 */
    String DRIVER_EXPORT = "logistics:driver:export";

    // ========== 本租户的物流域角色与权限初始化 ==========

    /**
     * 同步本租户的物流域角色与权限（幂等）。
     *
     * <p>权限行由开机同步全局创建，但**角色与角色-菜单是租户内的**（见 ADR 0026），需要一个
     * 租户上下文的入口：新租户开箱即得靠租户套餐，已有租户靠这个接口补。
     */
    String TENANT_ROLE_INIT = "logistics:tenant:role:init";

}
