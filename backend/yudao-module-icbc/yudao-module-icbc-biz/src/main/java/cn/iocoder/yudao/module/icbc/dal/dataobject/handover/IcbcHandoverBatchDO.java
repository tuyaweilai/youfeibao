package cn.iocoder.yudao.module.icbc.dal.dataobject.handover;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 交接批次 DO（#50 T12）。
 *
 * <p>一个交易对方的一次**物理交接**形成的货，是「这批货经历了什么」的追溯单位（见 CONTEXT
 * 「交接批次」）。它与「结算单」是两种粒度：交接批次按物理交接事件划分，结算单按确认对象
 * （同出售者 + 同场站）划分。
 *
 * <p>三件容易做错的事，这里刻意不做：
 * <ul>
 *   <li>**不按「车牌 + 日期」去重**：同一车同一天两次送货就是两个批次，磅单与收购单必须各归各；</li>
 *   <li>**不要求预约与采购订单**：临时上门的散户不被流程挡住，两者都只是可选的关联；</li>
 *   <li>**不把计量放在批次上**：本表只记「谁、什么时候、在哪儿、怎么来的、谁开的车」，
 *       毛重 / 皮重属于 {@link IcbcWeighingDO}，且只有被选定的那一次参与计量。</li>
 * </ul>
 */
@TableName("icbc_handover_batch")
@KeySequence("icbc_handover_batch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcHandoverBatchDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 批次号（平台生成，唯一） */
    private String batchNo;

    // ==================== 交易对方 ====================

    /** 出售者（收方）档案编号 */
    private Long payeeId;

    /** 出售者姓名快照（主体名称变更后历史批次不受影响） */
    private String sellerName;

    /** 出售者联系方式快照 */
    private String sellerMobile;

    // ==================== 地点：场站或上门地址 ====================

    /** 场站编号（到场收货时填；上门回收时为空） */
    private Long stationId;

    /** 场站名称快照 */
    private String stationName;

    /** 上门地址（上门回收时填；到场收货时为空） */
    private String visitAddress;

    // ==================== 交接信息 ====================

    /** 交接时间（默认登记时刻） */
    private LocalDateTime occurTime;

    /** 来源方式，枚举 {@link cn.iocoder.yudao.module.icbc.enums.HandoverSourceTypeEnum} */
    private String sourceType;

    /** 司机姓名（运输信息，不参与确认与收款） */
    private String driverName;

    /** 司机手机号（运输信息） */
    private String driverMobile;

    /** 车牌号（同一车同一天两次送货是两个批次，靠它与磅次区分） */
    private String plateNo;

    // ==================== 可选关联（都不是建批次的必要条件） ====================

    /** 关联的到站预约编号（可空；预约不是订单，见 ADR 0020） */
    private Long appointmentId;

    /** 关联的采购订单编号（可空；零散收购标为「直接收购」） */
    private Long purchaseOrderId;

    /** 备注 */
    private String remark;

}
