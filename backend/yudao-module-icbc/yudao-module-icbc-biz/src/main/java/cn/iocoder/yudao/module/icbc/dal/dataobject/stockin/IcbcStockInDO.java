package cn.iocoder.yudao.module.icbc.dal.dataobject.stockin;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入库单 DO（#52 T14，ADR 0027）。
 *
 * <p>入库是收购单派生的**单向动作**：仓管在待入库列表里挑一张收购单，选仓库 / 库位 / 批次
 * 并确认实际入库量。它把「验收后的货实际堆在哪儿」记下来，并（过账后）经 {@code StockApi}
 * 写库存流水、增量余额。库存写入只有这一条路径，icbc 不直接碰 {@code erp_stock*}。
 *
 * <p>三条约定的落点：
 * <ul>
 *     <li>**一张收购单可拆多个库位 / 分多次入库**：明细落在 {@link IcbcStockInItemDO}，
 *         多次入库就是多张入库单，累计上限由 {@code StockApi} 的 {@code maxCount} 兜底；</li>
 *     <li>**只有过账的入库才增加正式库存**：{@link #status} 为待过账时不动库存（ADR 0027 /
 *         规格 #38 实现决策第 9 条），过账才写流水；</li>
 *     <li>**可入库实物量只有一个取数点**：{@link #availableQuantity} 是确认时从收购单取的快照，
 *         取数在 {@code StockInService#resolveAvailableQuantity} 一处（#53 的接收量落地后只改那里）。</li>
 * </ul>
 */
@TableName("icbc_stock_in")
@KeySequence("icbc_stock_in_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcStockInDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 入库单号（平台生成，唯一） */
    private String stockInNo;

    // ==================== 来源收购单（入库是它的派生动作） ====================

    /** 收购单编号 */
    private Long acquisitionId;

    /** 收购单号快照 */
    private String acquisitionNo;

    /** 出售者（收方）档案编号 */
    private Long payeeId;

    /** 出售者姓名快照 */
    private String sellerName;

    /** 品类配置编号 */
    private Long goodsConfigId;

    /** 品类名称快照 */
    private String categoryName;

    /** 计量单位快照 */
    private String unit;

    // ==================== 数量 ====================

    /**
     * 可入库实物量快照（确认时的值）。
     *
     * <p>取数只有一处：{@code StockInService#resolveAvailableQuantity}。当前取收购单净重
     * （实物口径，毛重 − 皮重；ADR 0028：结算重量只作计价基准，不影响库存）。
     */
    private BigDecimal availableQuantity;

    /** 本次入库合计（各明细数量之和） */
    private BigDecimal totalQuantity;

    // ==================== 状态 ====================

    /** 状态，枚举 {@link cn.iocoder.yudao.module.icbc.enums.StockInStatusEnum}：0-待过账，1-已过账，2-已作废 */
    private Integer status;

    /** 过账时间；待过账时为空 */
    private LocalDateTime postedTime;

    /** 作废原因 */
    private String cancelReason;

    /** 作废时间 */
    private LocalDateTime cancelledTime;

    /** 备注 */
    private String remark;

}
