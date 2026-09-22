package cn.iocoder.yudao.module.logistics.enums;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 物流域权限行在「菜单管理」里的归集方式：权限标识 → 中文名 / 父页面 / 排序。
 *
 * <p>为什么需要它：权限行（{@code system_menu} 的按钮型行）由 {@code LogisticsPermissionSyncService}
 * 依据 {@link LogisticsRoleEnum} 生成（ADR 0026 的物流侧实现），而枚举里只有权限标识。只带标识生成出来的行
 * 在菜单树里就是一片 {@code logistics:transport-cost:delete}：没有中文名，也不挂在任何页面上。
 * 本枚举把「一个权限在界面上的说法」补齐，且与权限清单一样是**代码里的单一来源**——
 * 按钮行不进 {@code logistics-menu.sql}（见该文件头部），所以名称与归属只能落在这里。
 *
 * <p>{@code parentComponentName} 是页面菜单的组件名（{@code system_menu.component_name}），不是 id：
 * 页面菜单由 {@code logistics-menu.sql} 用自增 id 建，id 不稳定，而组件名在 SQL 里写死。
 * 传 {@code null} 表示「没有对应页面」，这类权限（初始化、运维入口）留在根节点，但名字仍是中文。
 *
 * <p>新增权限时的动作：在 {@link LogisticsPermission} 登记常量 → 挂到 {@link LogisticsRoleEnum} 的某个角色
 * → **在这里补一行归集**（漏了会被 {@code LogisticsPermissionMenuEnumTest} 挡住）。
 */
public enum LogisticsPermissionMenuEnum {

    // ========== 车辆档案（component_name = LogisticsVehicle） ==========

    VEHICLE_CREATE(LogisticsPermission.VEHICLE_CREATE, "车辆新增", "LogisticsVehicle", 1),
    VEHICLE_UPDATE(LogisticsPermission.VEHICLE_UPDATE, "车辆修改", "LogisticsVehicle", 2),
    VEHICLE_DELETE(LogisticsPermission.VEHICLE_DELETE, "车辆删除", "LogisticsVehicle", 3),
    VEHICLE_QUERY(LogisticsPermission.VEHICLE_QUERY, "车辆查询", "LogisticsVehicle", 4),
    VEHICLE_EXPORT(LogisticsPermission.VEHICLE_EXPORT, "车辆导出", "LogisticsVehicle", 5),
    // 到期提醒覆盖行驶证 / 保险 / 驾驶证 / 从业资格证，按「车」的档案入口看最自然
    EXPIRY_WARNING_QUERY(LogisticsPermission.EXPIRY_WARNING_QUERY, "到期提醒查询", "LogisticsVehicle", 6),

    // ========== 司机档案（component_name = LogisticsDriver） ==========

    DRIVER_CREATE(LogisticsPermission.DRIVER_CREATE, "司机新增", "LogisticsDriver", 1),
    DRIVER_UPDATE(LogisticsPermission.DRIVER_UPDATE, "司机修改", "LogisticsDriver", 2),
    DRIVER_DELETE(LogisticsPermission.DRIVER_DELETE, "司机删除", "LogisticsDriver", 3),
    DRIVER_QUERY(LogisticsPermission.DRIVER_QUERY, "司机查询", "LogisticsDriver", 4),
    DRIVER_EXPORT(LogisticsPermission.DRIVER_EXPORT, "司机导出", "LogisticsDriver", 5),
    // 司机端（V2c #79）不进 PC 菜单（ADR 0032），但他的能力属于「司机」这个对象，挂司机档案下便于理解
    DRIVER_APP_TASK_QUERY(LogisticsPermission.DRIVER_APP_TASK_QUERY, "司机端-任务查询", "LogisticsDriver", 6),
    DRIVER_APP_TASK_ACCEPT(LogisticsPermission.DRIVER_APP_TASK_ACCEPT, "司机端-接单", "LogisticsDriver", 7),
    DRIVER_APP_NODE_REPORT(LogisticsPermission.DRIVER_APP_NODE_REPORT, "司机端-节点上报", "LogisticsDriver", 8),
    DRIVER_APP_HANDOVER_REPORT(LogisticsPermission.DRIVER_APP_HANDOVER_REPORT, "司机端-交接登记", "LogisticsDriver", 9),
    DRIVER_APP_HANDOVER_QUERY(LogisticsPermission.DRIVER_APP_HANDOVER_QUERY, "司机端-交接查询", "LogisticsDriver", 10),

    // ========== 运输任务（component_name = LogisticsTask） ==========

    TRANSPORT_TASK_CREATE(LogisticsPermission.TRANSPORT_TASK_CREATE, "任务新建", "LogisticsTask", 1),
    TRANSPORT_TASK_UPDATE(LogisticsPermission.TRANSPORT_TASK_UPDATE, "任务修改", "LogisticsTask", 2),
    TRANSPORT_TASK_QUERY(LogisticsPermission.TRANSPORT_TASK_QUERY, "任务查询", "LogisticsTask", 3),
    TRANSPORT_TASK_ASSIGN(LogisticsPermission.TRANSPORT_TASK_ASSIGN, "派车", "LogisticsTask", 4),
    TRANSPORT_TASK_REASSIGN(LogisticsPermission.TRANSPORT_TASK_REASSIGN, "改派", "LogisticsTask", 5),
    TRANSPORT_TASK_CANCEL(LogisticsPermission.TRANSPORT_TASK_CANCEL, "任务取消", "LogisticsTask", 6),
    TRANSPORT_TASK_OVERRIDE(LogisticsPermission.TRANSPORT_TASK_OVERRIDE, "证件过期授权放行", "LogisticsTask", 7),
    TRANSPORT_NODE_REPORT(LogisticsPermission.TRANSPORT_NODE_REPORT, "节点上报", "LogisticsTask", 8),
    TRANSPORT_NODE_QUERY(LogisticsPermission.TRANSPORT_NODE_QUERY, "节点查询", "LogisticsTask", 9),
    TRANSPORT_NODE_ABNORMAL_RESOLVE(LogisticsPermission.TRANSPORT_NODE_ABNORMAL_RESOLVE, "异常解决", "LogisticsTask", 10),
    // 交接登记（V6 #73）在 PC 上没有独立页面：现场在司机端登记，PC 在运输任务里查与补录
    TRANSPORT_HANDOVER_QUERY(LogisticsPermission.TRANSPORT_HANDOVER_QUERY, "交接查询", "LogisticsTask", 11),
    TRANSPORT_HANDOVER_MANAGE(LogisticsPermission.TRANSPORT_HANDOVER_MANAGE, "交接登记与补录", "LogisticsTask", 12),
    DEMO_TRACK_QUERY(LogisticsPermission.DEMO_TRACK_QUERY, "轨迹演示查询", "LogisticsTask", 13),

    // ========== 承运商档案（component_name = LogisticsCarrier） ==========

    CARRIER_CREATE(LogisticsPermission.CARRIER_CREATE, "承运商新增", "LogisticsCarrier", 1),
    CARRIER_UPDATE(LogisticsPermission.CARRIER_UPDATE, "承运商修改", "LogisticsCarrier", 2),
    CARRIER_DELETE(LogisticsPermission.CARRIER_DELETE, "承运商删除", "LogisticsCarrier", 3),
    CARRIER_QUERY(LogisticsPermission.CARRIER_QUERY, "承运商查询", "LogisticsCarrier", 4),
    CARRIER_EXPORT(LogisticsPermission.CARRIER_EXPORT, "承运商导出", "LogisticsCarrier", 5),

    // ========== 承运合同（component_name = LogisticsCarrierContract） ==========

    CARRIER_CONTRACT_CREATE(LogisticsPermission.CARRIER_CONTRACT_CREATE, "合同新增", "LogisticsCarrierContract", 1),
    CARRIER_CONTRACT_UPDATE(LogisticsPermission.CARRIER_CONTRACT_UPDATE, "合同修改", "LogisticsCarrierContract", 2),
    CARRIER_CONTRACT_DELETE(LogisticsPermission.CARRIER_CONTRACT_DELETE, "合同删除", "LogisticsCarrierContract", 3),
    CARRIER_CONTRACT_QUERY(LogisticsPermission.CARRIER_CONTRACT_QUERY, "合同查询", "LogisticsCarrierContract", 4),
    CARRIER_CONTRACT_EXPORT(LogisticsPermission.CARRIER_CONTRACT_EXPORT, "合同导出", "LogisticsCarrierContract", 5),

    // ========== 运费对账（component_name = LogisticsFreight） ==========
    // 运费对账页有两个页签：承运商运费单（freight:*）与自有车运输费用（transport-cost:*）

    FREIGHT_CREATE(LogisticsPermission.FREIGHT_CREATE, "汇集运费", "LogisticsFreight", 1),
    FREIGHT_UPDATE(LogisticsPermission.FREIGHT_UPDATE, "运费单修改", "LogisticsFreight", 2),
    FREIGHT_QUERY(LogisticsPermission.FREIGHT_QUERY, "运费单查询", "LogisticsFreight", 3),
    FREIGHT_CONFIRM(LogisticsPermission.FREIGHT_CONFIRM, "确认应付", "LogisticsFreight", 4),
    FREIGHT_PAY(LogisticsPermission.FREIGHT_PAY, "登记付款凭证", "LogisticsFreight", 5),
    FREIGHT_EXPORT(LogisticsPermission.FREIGHT_EXPORT, "运费导出", "LogisticsFreight", 6),
    TRANSPORT_COST_CREATE(LogisticsPermission.TRANSPORT_COST_CREATE, "运输费用登记", "LogisticsFreight", 7),
    TRANSPORT_COST_UPDATE(LogisticsPermission.TRANSPORT_COST_UPDATE, "运输费用修改", "LogisticsFreight", 8),
    TRANSPORT_COST_DELETE(LogisticsPermission.TRANSPORT_COST_DELETE, "运输费用删除", "LogisticsFreight", 9),
    TRANSPORT_COST_QUERY(LogisticsPermission.TRANSPORT_COST_QUERY, "运输费用查询", "LogisticsFreight", 10),

    // ========== 没有对应页面的权限 ==========
    // 租户内权限初始化的入口，是运维动作不是页面能力，因此留在根节点（按钮型行不会出现在左侧导航里）
    TENANT_ROLE_INIT(LogisticsPermission.TENANT_ROLE_INIT, "物流权限初始化", null, 1);

    private final String permission;
    private final String name;
    private final String parentComponentName;
    private final int sort;

    LogisticsPermissionMenuEnum(String permission, String name, String parentComponentName, int sort) {
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

    private static final Map<String, LogisticsPermissionMenuEnum> BY_PERMISSION;

    static {
        Map<String, LogisticsPermissionMenuEnum> byPermission = new LinkedHashMap<>();
        for (LogisticsPermissionMenuEnum item : values()) {
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
    public static LogisticsPermissionMenuEnum ofPermission(String permission) {
        return BY_PERMISSION.get(permission);
    }

}
