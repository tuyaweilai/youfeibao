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
     * 合作方订单ID（等于收购单号；一个业务单号同一时刻只有一笔在途支付）
     */
    private String partnerOrderId;

    /**
     * 来源收购单编号（资金流证据挂回的那笔收购）
     */
    private Long acquisitionId;

    /**
     * 来源开票订单编号（预开票成功的那张票）
     */
    private Long invoiceOrderId;

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
     * 支付状态，枚举 {@link cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum}
     * 0-待支付，1-支付中，2-支付成功，3-支付失败，4-订单关闭，5-已冲正，
     * 6-已退汇，7-他行已扣款本行未入账，8-已支付待签收，9-部分成功
     */
    private Integer paymentStatus;

    /**
     * 工行原始支付状态码（payStatus）：-1/00/01/02/03/04/05/06/07/12/25
     */
    private String payStatus;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 支付流水号
     */
    private String paymentSerialNo;

    /**
     * 实际到账金额（部分成功时小于应付金额）
     */
    private BigDecimal actuallyReceivedAmount;

    /**
     * 转账回单号（归档的资金流凭证编号）
     */
    private String receiptNo;

    /**
     * 转账回单归档时间
     */
    private LocalDateTime receiptTime;

    /**
     * 自然人自行确认收到的时间（「我收到了」；**不改银行状态**，ADR 0021）
     */
    private LocalDateTime sellerReceivedConfirmedAt;

    /**
     * 自然人自行确认收到时的 IP
     */
    private String sellerReceivedConfirmIp;

    /**
     * 转账回单文件地址（工行回单 PDF / 截图，可空）
     */
    private String receiptFileUrl;

    /**
     * 重新发起次数：异常状态（失败 / 冲正 / 退汇 / 部分成功）后重新发起的累计次数
     */
    private Integer retryCount;

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