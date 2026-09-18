package cn.iocoder.yudao.module.icbc.dal.dataobject.payment;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工行付方支付订单 DO
 *
 * @author 芋道源码
 */
@TableName("icbc_payment_order")
@KeySequence("icbc_payment_order_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 支付订单号
     */
    private String orderNo;

    /**
     * 合作方订单ID
     */
    private String partnerOrderId;

    /**
     * 工行订单号
     */
    private String icbcOrderNo;

    /**
     * 收方编号
     */
    private String payeeNo;

    /**
     * 付方编号
     */
    private String payerNo;

    /**
     * 支付金额
     */
    private BigDecimal paymentAmount;

    /**
     * 支付状态
     * 0-待支付，1-支付中，2-支付成功，3-支付失败，4-已取消
     */
    private Integer paymentStatus;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 支付流水号
     */
    private String paymentSerialNo;

    /**
     * 机构编码（场景支付时必输）
     */
    private String verifiedCode;

    /**
     * U盾ID（场景支付时必输）
     */
    private String ukeyId;

    /**
     * 支付页面重定向URL
     */
    private String redirectUrl;

    /**
     * 消息通讯唯一编号
     */
    private String msgId;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 备注
     */
    private String remark;

} 