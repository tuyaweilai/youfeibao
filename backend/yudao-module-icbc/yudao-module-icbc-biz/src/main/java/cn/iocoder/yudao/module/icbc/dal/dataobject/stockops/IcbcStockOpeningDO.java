package cn.iocoder.yudao.module.icbc.dal.dataobject.stockops;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 期初记录 DO（#54 T16）。
 *
 * <p>「当前库存」之前必须有期初：把启用平台之前就躺在仓库里的货录进来（导入），否则余额只等于
 * 平台内收货的累计入库，不能当在库量读（规格 #38 user story 37）。一行 = 一个
 * 「品类 + 仓库 + 库位 + 批次」的期初数量，导入即过账（经 {@code StockApi} 写
 * {@code OPENING_IN} 流水）。
 *
 * <p>**同一维度只允许一条生效期初**：重复导入会撞 {@code STOCK_OPENING_DIMENSION_DUPLICATED}；
 * 录错了先作废（冲销流水）再导，或用盘点调整修正。
 */
@TableName("icbc_stock_opening")
@KeySequence("icbc_stock_opening_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcStockOpeningDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 导入批次号（一次导入一个批次号，便于回溯与整体作废） */
    private String openingNo;

    /** 品类编号（icbc_goods_config.id） */
    private Long goodsConfigId;

    /** 仓库编号（erp_warehouse.id） */
    private Long warehouseId;

    /** 库位编号；0 = 未指定 */
    private Long locationId;

    /** 批次编号；0 = 未指定 */
    private Long batchId;

    /** 期初数量（正数；只有真的没货才不导这一行） */
    private BigDecimal quantity;

    /** 状态，枚举 {@link cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum}：1-已过账，2-已作废 */
    private Integer status;

    /** 过账时间 */
    private LocalDateTime postedTime;

    /** 作废原因 */
    private String cancelReason;

    /** 作废时间 */
    private LocalDateTime cancelledTime;

    /** 备注 */
    private String remark;

}
