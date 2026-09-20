package cn.iocoder.yudao.module.waste.dal.dataobject.payment;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对公付款凭证 DO
 *
 * @author 芋道源码
 */
@TableName("waste_company_payment_voucher")
@KeySequence("waste_company_payment_voucher_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyPaymentVoucherDO extends BaseDO {

    /**
     * 凭证ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 凭证编号 (系统生成)
     */
    private String voucherNo;

    // ========== 付款信息 ==========
    /**
     * 付款类型 (1:银行转账, 2:支票, 3:汇票, 4:其他)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.PaymentTypeEnum}
     */
    private Integer paymentType;

    /**
     * 付款金额
     */
    private BigDecimal paymentAmount;

    /**
     * 付款日期
     */
    private LocalDate paymentDate;

    // ========== 付款方信息 ==========
    /**
     * 付款银行
     */
    private String paymentBank;

    /**
     * 付款账号
     */
    private String paymentAccount;

    // ========== 收款方信息 ==========
    /**
     * 收款银行
     */
    private String payeeBank;

    /**
     * 收款账号
     */
    private String payeeAccount;

    /**
     * 收款人姓名
     */
    private String payeeName;

    // ========== 交易信息 ==========
    /**
     * 银行流水号
     */
    private String transactionNo;

    /**
     * 转账凭证图片URL
     */
    private String transferVoucherUrl;

    /**
     * 银行回单图片URL
     */
    private String bankReceiptUrl;

    // ========== 状态管理 ==========
    /**
     * 凭证状态 (0:待确认, 1:已确认, 2:有争议, 3:已作废)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.VoucherStatusEnum}
     */
    private Integer voucherStatus;

    /**
     * 产废企业确认状态
     */
    private Boolean confirmedByProducer;

    /**
     * 回收企业确认状态
     */
    private Boolean confirmedByRecycling;

    /**
     * 产废企业确认时间
     */
    private LocalDateTime producerConfirmTime;

    /**
     * 回收企业确认时间
     */
    private LocalDateTime recyclingConfirmTime;

    /**
     * 争议原因
     */
    private String disputeReason;

    // ========== 操作信息 ==========
    /**
     * 操作员ID
     */
    private Long operatorId;

    /**
     * 操作员姓名
     */
    private String operatorName;

    /**
     * 备注说明
     */
    private String remark;

} 