package cn.iocoder.yudao.module.waste.dal.dataobject.payment;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 付款状态变更历史 DO
 *
 * @author 芋道源码
 */
@TableName("waste_payment_status_history")
@KeySequence("waste_payment_status_history_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusHistoryDO extends BaseDO {

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
     * 原状态
     */
    private Integer statusFrom;
    
    /**
     * 目标状态
     */
    private Integer statusTo;
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 变更原因
     */
    private String changeReason;
    
    /**
     * 操作者类型：1-系统，2-产废企业，3-回收企业，4-平台管理员
     */
    private Integer operatorType;
    
    /**
     * 操作者ID
     */
    private Long operatorId;
    
    /**
     * 操作者姓名
     */
    private String operatorName;
    
    /**
     * 变更时间
     */
    private LocalDateTime changeTime;
    
    /**
     * 付款金额
     */
    private BigDecimal paymentAmount;
    
    /**
     * 付款方式：1-银行转账，2-现金，3-支票，4-其他
     */
    private Integer paymentMethod;
    
    /**
     * 凭证ID
     */
    private Long voucherId;
    
    /**
     * 业务数据（JSON格式）
     */
    private String businessData;

} 