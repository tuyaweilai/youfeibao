package cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运输停靠点 DO（V5 #72）。
 *
 * <p>运输任务里的一个**提货或送货地点**。一个任务可有多个、对应不同出售者（集货）；
 * **每个停靠点各自交接、各自复磅、各自结算**——一次集货不构成把几个出售者合并结算的依据
 * （ADR 0031、#59 的 Implementation Decisions 第 4 条）。整车复磅只能核对总运输量。
 *
 * <p>物流不引用 icbc 的类（ADR 0032）：出售者只存**编号 + 姓名 / 手机号快照**，
 * 编号是 icbc 侧的 {@code icbc_payee_info.id}，物流不知道它长什么样。
 *
 * <p>进度由该停靠点自己的运输节点推动（见 {@code LogisticsTransportStopStatusEnum}）：
 * 到达提货点/交接完成/起运三类节点带 {@code stopId}；到达场站/卸货完成是整趟活的收尾，不带。
 */
@TableName("logistics_transport_stop")
@KeySequence("logistics_transport_stop_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsTransportStopDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 运输任务编号 */
    private Long taskId;
    /** 运输任务单号（冗余，便于按单号查询与展示） */
    private String taskNo;

    /** 停靠顺序（从 1 开始，同一任务内唯一） */
    private Integer stopNo;

    /**
     * 停靠点类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsTransportStopTypeEnum}
     */
    private Integer stopType;

    /** 出售者编号（icbc 侧编号，可空：临时散户现场才建档） */
    private Long payeeId;
    /** 出售者姓名快照 */
    private String payeeName;
    /** 出售者手机号快照 */
    private String payeeMobile;

    /** 地址 */
    private String address;
    /** 联系人 */
    private String contactName;
    /** 联系电话 */
    private String contactPhone;

    /** 货物名称（计划提示，不是品类权威：权威品类在交接登记与收购单上） */
    private String cargoName;
    /** 约量（计划提示） */
    private BigDecimal estimatedQuantity;
    /** 约量单位 */
    private String quantityUnit;

    /** 预计到站时间 */
    private LocalDateTime expectedArrivalTime;

    /**
     * 停靠点状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsTransportStopStatusEnum}
     */
    private Integer status;

    /** 取消原因（取消必填） */
    private String cancelReason;
    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 备注 */
    private String remark;

}
