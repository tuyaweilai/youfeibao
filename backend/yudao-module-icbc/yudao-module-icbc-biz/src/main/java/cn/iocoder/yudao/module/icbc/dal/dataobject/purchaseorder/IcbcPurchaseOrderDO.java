package cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购订单 DO（#46 T08，ADR 0027）。
 *
 * <p>回收企业内部的采购**执行依据**：准备向某个交易对方买哪些品类、多少量、什么价、在哪段时间、
 * 哪个场站，支持按明细分次收货。它不是交易对方下的单（那是「到站预约」）。
 *
 * <p>对手方沿用 ADR 0029 的「主体类型六态 + 双可空 id」：自然人出售者取 {@link #payeeId}，
 * 其余五类取 {@link #supplierId}，恰好一个非空。{@link #counterpartyName} 是下单时的名称快照。
 *
 * <p>可选的采购依据：{@link #contractId}（采购合同，下单时校验已审核生效）与
 * {@link #stationId}（执行场站）。状态见 {@code PurchaseOrderStatusEnum}：只有「执行中」可作为
 * 有效采购依据。超期不落库，由 {@link #endDate} 推导。
 */
@TableName("icbc_purchase_order")
@KeySequence("icbc_purchase_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcPurchaseOrderDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 采购订单号（平台生成，租户内唯一） */
    private String orderNo;

    /** 关联采购合同编号（可空；关联即代表本次采购有合同依据） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long contractId;

    /** 采购合同号快照（回查用，合同改名不影响历史订单） */
    private String contractNo;

    /** 交易对方主体类型，枚举 {@code SellerSubjectTypeEnum}（六态，判定规则是「是否属于自然人」） */
    private Integer counterpartyType;

    /** 自然人出售者档案编号（主体类型为自然人时非空） */
    // updateStrategy = ALWAYS：换对手方 / 改合同时要把另一个 id 真正清成 NULL，
    // MyBatis-Plus 默认 NOT_NULL 策略会跳过 null。所有更新路径都传整份 DO，故安全。
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long payeeId;

    /** 单位供货方编号（主体类型为非自然人时非空，指向 erp_supplier） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long supplierId;

    /** 交易对方名称快照（下单时固化） */
    private String counterpartyName;

    /** 执行场站编号（可空：不指定场站也能下单） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long stationId;

    /** 执行场站名称快照 */
    private String stationName;

    /** 执行开始日期 */
    private LocalDate startDate;

    /** 执行结束日期（早于今天即视为过期） */
    private LocalDate endDate;

    /** 状态，枚举 {@code PurchaseOrderStatusEnum} */
    private Integer status;

    /** 计划总量（由明细汇总，快照） */
    private BigDecimal totalQuantity;

    /** 计划总金额（由明细汇总，快照；供进项收票勾稽 #49 按 id 取单据金额） */
    private BigDecimal totalAmount;

    /** 暂停原因（恢复时清空：ALWAYS 策略让 null 能真正写回） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String suspendReason;

    /** 暂停时间（恢复时清空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime suspendedTime;

    /** 完成时间 */
    private LocalDateTime completedTime;

    /** 关闭人 */
    private Long closedBy;

    /** 关闭时间 */
    private LocalDateTime closedTime;

    /** 关闭原因 */
    private String closeReason;

    /** 备注 */
    private String remark;

}
