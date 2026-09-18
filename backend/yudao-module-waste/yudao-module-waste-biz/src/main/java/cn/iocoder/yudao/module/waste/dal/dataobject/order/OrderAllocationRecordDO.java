package cn.iocoder.yudao.module.waste.dal.dataobject.order;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单过磅分摊记录 DO
 *
 * @author 芋道源码
 */
@TableName("waste_order_allocation_record")
@KeySequence("waste_order_allocation_record_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAllocationRecordDO extends BaseDO {

    /**
     * 分摊记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 关联车辆过磅记录ID
     */
    private Long vehicleWeighingId;

    /**
     * 过磅批次号（来自物流模块）
     */
    private String weighingBatchNo;

    /**
     * 车辆净重（来自物流模块）
     */
    private BigDecimal vehicleNetWeight;

    /**
     * 分摊方法 (1:按预估量比例, 2:按确认量比例, 3:人工指定, 4:平均分摊)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.AllocationMethodEnum}
     */
    private Integer allocationMethod;

    // ========== 分摊计算过程 ==========
    /**
     * 订单预估量
     */
    private BigDecimal estimatedQuantity;

    /**
     * 分摊比例
     */
    private BigDecimal allocationRatio;

    /**
     * 分摊后数量
     */
    private BigDecimal allocatedQuantity;

    // ========== 金额计算 ==========
    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 预估金额
     */
    private BigDecimal estimatedAmount;

    /**
     * 分摊后金额
     */
    private BigDecimal allocatedAmount;

    /**
     * 金额调整 = 分摊金额 - 预估金额
     */
    private BigDecimal amountAdjustment;

    // ========== 分摊详情 ==========
    /**
     * 分摊时间
     */
    private LocalDateTime allocationTime;

    /**
     * 分摊操作员
     */
    private String allocationOperator;

    /**
     * 是否人工调整
     */
    private Boolean isManualAdjustment;

    /**
     * 人工调整原因
     */
    private String adjustmentReason;

    /**
     * 分摊备注
     */
    private String remark;

} 