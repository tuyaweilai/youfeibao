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

    // ========== 运输过程：改派与异常（V4 #71） ==========

    /**
     * 改派运输任务（换车换人）。
     *
     * <p>与首次派车分开一条权限：改派发生在货已经在路上之后，是比派车更重的一个动作
     *（要写承接记录、要放回原车）。
     */
    String TRANSPORT_TASK_REASSIGN = "logistics:transport-task:reassign";

    /**
     * 解决运输异常（记录谁 / 什么时候 / 怎么解决的）。
     *
     * <p>**上报异常不需要单独权限**：它就是一次节点上报（车辆的异常也走同一个司机端入口），
     * 复用 {@link #TRANSPORT_NODE_REPORT}；解决是调度侧的动作，才需要单独一位。
     */
    String TRANSPORT_NODE_ABNORMAL_RESOLVE = "logistics:transport-node:abnormal:resolve";

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

    // ========== 承运合同（V8 #75） ==========

    /** 新增承运合同 */
    String CARRIER_CONTRACT_CREATE = "logistics:carrier-contract:create";
    /** 修改承运合同 */
    String CARRIER_CONTRACT_UPDATE = "logistics:carrier-contract:update";
    /** 删除承运合同 */
    String CARRIER_CONTRACT_DELETE = "logistics:carrier-contract:delete";
    /** 查询承运合同（汇集运费选合同也要这个权限） */
    String CARRIER_CONTRACT_QUERY = "logistics:carrier-contract:query";
    /** 导出承运合同 */
    String CARRIER_CONTRACT_EXPORT = "logistics:carrier-contract:export";

    // ========== 承运商运费与对账（V8 #75） ==========

    /**
     * 汇集运费（按趟次建运费单）。
     *
     * <p>**自有车不产生承运商运费**：这一趟不是承运商的车就建不出运费单，这是硬校验不是提示。
     */
    String FREIGHT_CREATE = "logistics:freight:create";
    /** 修改运费单（改计费量 / 实际应付 / 差异原因） */
    String FREIGHT_UPDATE = "logistics:freight:update";
    /** 查询运费单与对账汇总 */
    String FREIGHT_QUERY = "logistics:freight:query";
    /** 确认应付（把应有的应付确认为实际应付，差异必须留原因） */
    String FREIGHT_CONFIRM = "logistics:freight:confirm";
    /**
     * 登记外部付款凭证（**不接对公付款通道**，ADR 0006）。
     *
     * <p>与确认应付分开一条权限：确认应付是「欠多少」，登记凭证是「付了、凭什么是这笔」。
     */
    String FREIGHT_PAY = "logistics:freight:pay";
    /** 导出运费对账 */
    String FREIGHT_EXPORT = "logistics:freight:export";

    // ========== 运输费用：自有车的路桥 / 燃油等内部成本（V8 #75） ==========

    /** 登记运输费用 */
    String TRANSPORT_COST_CREATE = "logistics:transport-cost:create";
    /** 修改运输费用 */
    String TRANSPORT_COST_UPDATE = "logistics:transport-cost:update";
    /** 删除运输费用 */
    String TRANSPORT_COST_DELETE = "logistics:transport-cost:delete";
    /** 查询运输费用 */
    String TRANSPORT_COST_QUERY = "logistics:transport-cost:query";

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

    // ========== 轨迹演示件（V9 #76） ==========

    /**
     * 查看运输轨迹**演示**（模拟数据）。
     *
     * <p>单独一条权限而不是混进运输任务查询：演示件要能**一键关掉**，权限粒度与配置开关一致，
     * 谁都不必为了演示能力放开真实数据的读取面。
     */
    String DEMO_TRACK_QUERY = "logistics:demo:track:query";

    // ========== 司机端（V2c #79） ==========
    //
    // 司机在用司机端时只拿这几个权限，且**只看得到派给自己的任务**——「是谁的任务」这层归属校验
    // 在 LogisticsDriverAppService 里按登录账号对应的司机档案强制加条件，不是靠权限位。

    /** 司机端：查看派给自己的任务与时间线 */
    String DRIVER_APP_TASK_QUERY = "logistics:driver-app:task:query";
    /** 司机端：接单 */
    String DRIVER_APP_TASK_ACCEPT = "logistics:driver-app:task:accept";
    /** 司机端：上报运输节点（含位置快照与照片） */
    String DRIVER_APP_NODE_REPORT = "logistics:driver-app:node:report";
    /** 司机端：登记交接（品类 / 参考量 / 参考单价 / 凭证照片，现场不产生金额） */
    String DRIVER_APP_HANDOVER_REPORT = "logistics:driver-app:handover:report";
    /** 司机端：查看自己登记过的交接 */
    String DRIVER_APP_HANDOVER_QUERY = "logistics:driver-app:handover:query";

    // ========== 交接登记（V6 #73） ==========

    /**
     * 查询交接登记（PC：磅房 / 调度看现场参考量与照片凭证）。
     *
     * <p>现场登记的主入口在司机端（见 {@link #DRIVER_APP_HANDOVER_REPORT}）；这条给 PC 查处与对账用。
     */
    String TRANSPORT_HANDOVER_QUERY = "logistics:transport-handover:query";
    /**
     * 登记 / 补录交接（PC 代录：司机没带手机、或事后补录）。
     */
    String TRANSPORT_HANDOVER_MANAGE = "logistics:transport-handover:manage";

    // ========== 本租户的物流域角色与权限初始化 ==========

    /**
     * 同步本租户的物流域角色与权限（幂等）。
     *
     * <p>权限行由开机同步全局创建，但**角色与角色-菜单是租户内的**（见 ADR 0026），需要一个
     * 租户上下文的入口：新租户开箱即得靠租户套餐，已有租户靠这个接口补。
     */
    String TENANT_ROLE_INIT = "logistics:tenant:role:init";

}
