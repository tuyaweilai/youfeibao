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

    // ========== 运输任务与节点（V2b #78） ==========

    /** 新建运输任务（派车） */
    String TRANSPORT_TASK_CREATE = "logistics:transport-task:create";
    /** 修改运输任务（改派、改时间窗等） */
    String TRANSPORT_TASK_UPDATE = "logistics:transport-task:update";
    /** 查询运输任务与时间线 */
    String TRANSPORT_TASK_QUERY = "logistics:transport-task:query";
    /** 派车（给待分配的任务安排车与司机） */
    String TRANSPORT_TASK_ASSIGN = "logistics:transport-task:assign";
    /** 取消运输任务（必填原因） */
    String TRANSPORT_TASK_CANCEL = "logistics:transport-task:cancel";
    /** 上报运输节点（司机端，也用于调度在 PC 上代录） */
    String TRANSPORT_NODE_REPORT = "logistics:transport-node:report";
    /** 查询运输节点与凭证 */
    String TRANSPORT_NODE_QUERY = "logistics:transport-node:query";

    // ========== 承运商档案（V3 #70） ==========

    /** 新增承运商 */
    String CARRIER_CREATE = "logistics:carrier:create";
    /** 修改承运商 */
    String CARRIER_UPDATE = "logistics:carrier:update";
    /** 删除承运商 */
    String CARRIER_DELETE = "logistics:carrier:delete";
    /** 查询承运商 */
    String CARRIER_QUERY = "logistics:carrier:query";
    /** 导出承运商 */
    String CARRIER_EXPORT = "logistics:carrier:export";

    // ========== 派车门禁的授权放行（V3 #70） ==========

    /**
     * 证件过期时授权放行（**只挂管理员**）。
     *
     * <p>放行的是「软门禁」：证件过期（现实里常见「正在换证」）。车辆维修中、司机离职这类
     * **硬门禁不可授权绕过**——那不是流程不便，是无证运营。
     */
    String TRANSPORT_TASK_OVERRIDE = "logistics:transport-task:override";

    /** 查看到期提醒（行驶证 / 保险 / 驾驶证 / 从业资格证） */
    String EXPIRY_WARNING_QUERY = "logistics:expiry-warning:query";

    // ========== 司机端（V2c #79） ==========
    //
    // 司机在用司机端时只拿这三个权限，且**只看得到派给自己的任务**——「是谁的任务」这层归属校验
    // 在 LogisticsDriverAppService 里按登录账号对应的司机档案强制加条件，不是靠权限位。

    /** 司机端：查看派给自己的任务与时间线 */
    String DRIVER_APP_TASK_QUERY = "logistics:driver-app:task:query";
    /** 司机端：接单 */
    String DRIVER_APP_TASK_ACCEPT = "logistics:driver-app:task:accept";
    /** 司机端：上报运输节点（含位置快照与照片） */
    String DRIVER_APP_NODE_REPORT = "logistics:driver-app:node:report";

    // ========== 本租户的物流域角色与权限初始化 ==========

    /**
     * 同步本租户的物流域角色与权限（幂等）。
     *
     * <p>权限行由开机同步全局创建，但**角色与角色-菜单是租户内的**（见 ADR 0026），需要一个
     * 租户上下文的入口：新租户开箱即得靠租户套餐，已有租户靠这个接口补。
     */
    String TENANT_ROLE_INIT = "logistics:tenant:role:init";

}
