package cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 运输任务改派承接记录 DO（V4 #71）。
 *
 * <p>换车换人**不覆盖原记录**：任务行上的 {@code vehicleId / driverId} 与快照只表示
 * 「当前是谁」，每一次改派在这里追加一条「原车原人 → 新车新人 + 原因 + 谁改的」。
 * 一趟活中途换了两次车，就有两条承接记录，前后关系串得起来。
 *
 * <p>车牌与司机是**双份快照**（改派前 / 改派后）：档案改名或删档都不影响这条既成事实。
 */
@TableName("logistics_transport_task_reassign")
@KeySequence("logistics_transport_task_reassign_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsTransportTaskReassignDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 运输任务编号 */
    private Long taskId;
    /** 运输任务单号（冗余） */
    private String taskNo;

    /** 原车辆编号 */
    private Long prevVehicleId;
    /** 原车牌号快照 */
    private String prevPlateNo;
    /** 原司机编号 */
    private Long prevDriverId;
    /** 原司机姓名快照 */
    private String prevDriverName;
    /** 原司机手机号快照 */
    private String prevDriverMobile;

    /** 新车辆编号 */
    private Long vehicleId;
    /** 新车牌号快照 */
    private String plateNo;
    /** 新司机编号 */
    private Long driverId;
    /** 新司机姓名快照 */
    private String driverName;
    /** 新司机手机号快照 */
    private String driverMobile;

    /** 改派原因（必填） */
    private String reason;
    /** 改派人（系统用户编号） */
    private Long operatorId;
    /** 改派人姓名快照 */
    private String operatorName;
    /** 改派时间 */
    private LocalDateTime reassignTime;

}
