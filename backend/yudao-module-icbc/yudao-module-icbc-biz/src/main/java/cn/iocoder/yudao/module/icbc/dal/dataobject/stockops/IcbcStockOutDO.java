package cn.iocoder.yudao.module.icbc.dal.dataobject.stockops;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 非销售出库单 DO（#54 T16，ADR 0025）。
 *
 * <p>报损 / 退货出库 / 内部领用三种，**都不挂客户**——一期不做销售出库（ADR 0004 不做正向开票），
 * 出库只是因为货真的少了、退回去了或本企业自己用了。过账才经 {@code StockApi} 写库存流水，
 * icbc 不直接碰 {@code erp_stock*}。
 */
@TableName("icbc_stock_out")
@KeySequence("icbc_stock_out_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcStockOutDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 出库单号（平台生成，唯一） */
    private String stockOutNo;

    /** 出库类型，枚举 {@link cn.iocoder.yudao.module.icbc.enums.StockOutTypeEnum}：10-报损，20-退货出库，30-内部领用 */
    private Integer outType;

    /** 出库合计（各明细数量之和） */
    private BigDecimal totalQuantity;

    /** 状态，枚举 {@link cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum}：0-待过账，1-已过账，2-已作废 */
    private Integer status;

    /** 过账时间；待过账时为空 */
    private LocalDateTime postedTime;

    /** 作废原因 */
    private String cancelReason;

    /** 作废时间 */
    private LocalDateTime cancelledTime;

    /** 备注（出库事由说明） */
    private String remark;

}
