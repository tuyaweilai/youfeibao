package cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 运输任务 DO（V2b #78）。
 *
 * <p>一期「一个任务 = 一车 + 一司机 + 一次执行」（ADR 0032 第 5 条）：一个出发地、一个提货点、
 * 一个时间窗。多停靠点集货归 V5（#72），独立「车次」实体一期不建。
 *
 * <p>**车牌与司机是快照**：档案改名或删档都不影响历史单据（一票一档的追溯不能被追溯对象自己变掉）。
 * 同时保留 id 引用，让「这台车跑了多少趟」算得出来。
 *
 * <p>任务可以挂在采购安排上（采购订单编号 + 单号快照），也可以**什么都不挂**——司机直接上门收购
 * 是常态（与「直接收购」同一逻辑，ADR 0027）。
 */
@TableName("logistics_transport_task")
@KeySequence("logistics_transport_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsTransportTaskDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 运输任务单号（租户内唯一，对外可见） */
    private String taskNo;

    /**
     * 任务状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum}
     */
    private Integer status;

    /** 车辆编号（派车后非空） */
    private Long vehicleId;
    /** 车牌号快照 */
    private String plateNo;

    /** 司机编号（派车后非空） */
    private Long driverId;
    /** 司机姓名快照 */
    private String driverName;
    /** 司机手机号快照 */
    private String driverMobile;

    /** 出发地（通常是场站或车队所在地） */
    private String departureAddress;

    /** 提货点地址 */
    private String pickupAddress;
    /** 提货点联系人 */
    private String pickupContactName;
    /** 提货点联系电话 */
    private String pickupContactPhone;

    /** 时间窗开始 */
    private LocalDateTime expectedStartTime;
    /** 时间窗结束 */
    private LocalDateTime expectedEndTime;

    /** 关联采购订单编号（可空：什么都不挂也能派车） */
    private Long purchaseOrderId;
    /** 采购订单号快照 */
    private String purchaseOrderNo;

    /** 派车时间 */
    private LocalDateTime assignTime;
    /** 接单时间 */
    private LocalDateTime acceptTime;
    /** 起运时间（上报「起运」节点时落） */
    private LocalDateTime startTime;
    /** 完成时间 */
    private LocalDateTime completeTime;
    /** 取消时间 */
    private LocalDateTime cancelTime;
    /** 取消原因（取消必填） */
    private String cancelReason;

    /** 备注 */
    private String remark;

}
