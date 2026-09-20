package cn.iocoder.yudao.module.icbc.dal.dataobject.stockops;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 盘点单 DO（#54 T16，ADR 0025）。
 *
 * <p>盘点与调拨的区别：调拨搬数量，盘点把余额**对齐到一个实测值**。过账时经
 * {@code StockApi#adjustTo} 由 ERP 在同一个事务里算差额（盘盈写 {@code CHECK_MORE_IN}、
 * 盘亏写 {@code CHECK_LESS_OUT}），账实相符不写流水。
 */
@TableName("icbc_stock_check")
@KeySequence("icbc_stock_check_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcStockCheckDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 盘点单号（平台生成，唯一） */
    private String checkNo;

    /** 状态，枚举 {@link cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum}：0-待过账，1-已过账，2-已作废 */
    private Integer status;

    /** 过账时间；待过账时为空 */
    private LocalDateTime postedTime;

    /** 作废原因 */
    private String cancelReason;

    /** 作废时间 */
    private LocalDateTime cancelledTime;

    /** 备注（盘点范围 / 事由） */
    private String remark;

}
