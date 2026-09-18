package cn.iocoder.yudao.module.waste.dal.dataobject.order;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单价格调整记录 DO
 *
 * @author 芋道源码
 */
@TableName("waste_order_price_adjustment")
@KeySequence("waste_order_price_adjustment_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPriceAdjustmentDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 调整类型：1-价格调整，2-数量调整，3-价格和数量调整
     */
    private Integer adjustmentType;
    
    /**
     * 原价格
     */
    private BigDecimal originalPrice;
    
    /**
     * 调整后价格
     */
    private BigDecimal adjustedPrice;
    
    /**
     * 原数量
     */
    private BigDecimal originalQuantity;
    
    /**
     * 调整后数量
     */
    private BigDecimal adjustedQuantity;
    
    /**
     * 原金额
     */
    private BigDecimal originalAmount;
    
    /**
     * 调整后金额
     */
    private BigDecimal adjustedAmount;
    
    /**
     * 调整金额（正数为增加，负数为减少）
     */
    private BigDecimal adjustmentAmount;
    
    /**
     * 调整原因
     */
    private String adjustmentReason;
    
    /**
     * 状态：0-待确认，1-已确认，2-已拒绝
     */
    private Integer status;
    
    /**
     * 确认人
     */
    private String confirmedBy;
    
    /**
     * 确认时间
     */
    private LocalDateTime confirmedTime;
    
    /**
     * 关联过磅记录ID
     */
    private Long relatedWeighingId;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 备注
     */
    private String remark;

} 