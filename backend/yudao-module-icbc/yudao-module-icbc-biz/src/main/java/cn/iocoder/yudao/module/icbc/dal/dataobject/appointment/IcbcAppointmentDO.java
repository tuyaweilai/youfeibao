package cn.iocoder.yudao.module.icbc.dal.dataobject.appointment;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 到站预约 DO（#35，ADR 0020）。
 *
 * <p>自然人主动声明「我将在某个时间到某个场站卖某品类、大约多少量、车牌是多少」，
 * 用于**排队与到站登记时带出**，**不是订单**：
 * <ul>
 *   <li>不是合同、不占额度、不产生开票、不进五流；</li>
 *   <li>没有「企业接受 / 拒绝」动作，只有「到场」与「未到场」；</li>
 *   <li>预计数量只是「约」，任何统计与额度口径都不得引用本表（额度只认收购单与发票事实）。</li>
 * </ul>
 */
@TableName("icbc_appointment")
@KeySequence("icbc_appointment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcAppointmentDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 预约编号（平台生成，唯一） */
    private String appointmentNo;

    /** 自然人主体编号（平台级身份；他可能在多家企业都有预约） */
    private Long naturalPersonId;

    /** 收方（出售者）档案编号；预约时本租户尚未建档则为空 */
    private Long payeeId;

    /** 出售者姓名快照（预约时可空） */
    private String sellerName;

    // ==================== 场站 ====================

    /** 场站编号 */
    private Long stationId;

    /** 场站码快照（二维码里那个码） */
    private String stationCode;

    /** 场站名称快照 */
    private String stationName;

    // ==================== 货物与车辆 ====================

    /** 品类配置编号（到站登记时据此带出单位 / 税率 / 计税方法 / 编码） */
    private Long goodsConfigId;

    /** 品类名称快照 */
    private String categoryName;

    /** 计量单位快照 */
    private String unit;

    /** 预计数量（**可空**；界面一律以「约」标注，不参与任何计价与统计） */
    private BigDecimal expectedQuantity;

    /** 车牌号 */
    private String plateNo;

    // ==================== 到站 ====================

    /** 预计到站时间 */
    private LocalDateTime expectedArrivalTime;

    /** 状态，枚举 {@link cn.iocoder.yudao.module.icbc.enums.AppointmentStatusEnum} */
    private Integer status;

    /** 实际到场时间（收货员标记到场时写入） */
    private LocalDateTime arrivedAt;

    /** 到场后建的收购单编号（只作关联，预约本身不产生收购事实） */
    private Long acquisitionId;

    /** 取消时间（出售者本人取消） */
    private LocalDateTime cancelledAt;

    /** 取消原因 */
    private String cancelReason;

    /** 未到场说明（收货员标记时可选填写） */
    private String noShowReason;

    /** 备注 */
    private String remark;

}
