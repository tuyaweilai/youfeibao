package cn.iocoder.yudao.module.icbc.dal.dataobject.stockops;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跨仓调拨单 DO（#54 T16，ADR 0025）。
 *
 * <p>把同一品类的货从一个仓库 / 库位 / 批次搬到另一个：源减、目标加。过账时经
 * {@code StockApi#move} 一次写两条流水（{@code MOVE_OUT} / {@code MOVE_IN}），
 * 作废时按相反方向调回。
 */
@TableName("icbc_stock_move")
@KeySequence("icbc_stock_move_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcStockMoveDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 调拨单号（平台生成，唯一） */
    private String moveNo;

    /** 调拨合计（各明细数量之和） */
    private BigDecimal totalQuantity;

    /** 状态，枚举 {@link cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum}：0-待过账，1-已过账，2-已作废 */
    private Integer status;

    /** 过账时间；待过账时为空 */
    private LocalDateTime postedTime;

    /** 作废原因 */
    private String cancelReason;

    /** 作废时间 */
    private LocalDateTime cancelledTime;

    /** 备注（调拨事由） */
    private String remark;

}
