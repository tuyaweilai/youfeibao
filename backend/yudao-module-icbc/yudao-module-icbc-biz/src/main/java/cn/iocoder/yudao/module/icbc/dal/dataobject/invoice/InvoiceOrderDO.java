package cn.iocoder.yudao.module.icbc.dal.dataobject.invoice;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工行反向开票订单 DO
 *
 * @author 芋道源码
 */
@TableName("icbc_invoice_order")
@KeySequence("icbc_invoice_order_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceOrderDO extends TenantBaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    
    /**
     * 订单号（我方生成）
     */
    private String orderNo;
    
    /**
     * 合作方订单ID（传给工行）
     */
    private String partnerOrderId;

    /**
     * 来源收购单编号（开票申请由已登记的收购单发起，一个收购单对应一张票）
     */
    private Long acquisitionId;

    /**
     * 收方ID
     */
    private Long payeeId;
    
    /**
     * 收方编号
     */
    private String payeeNo;
    
    /**
     * 付方ID
     */
    private Long payerId;
    
    /**
     * 付方编号
     */
    private String payerNo;
    
    /**
     * 订单总金额（元）
     */
    private BigDecimal totalAmount;
    
    /**
     * 发票类型：1-增值税普通发票，2-增值税专用发票
     */
    private Integer invoiceType;
    
    /**
     * 业务类型：AGRICULTURAL-农产品收购，SCRAP-报废产品收购
     */
    private String businessType;
    
    /**
     * 订单状态：0-待确认，1-已确认，2-已支付，3-已开票，4-已完成，9-已取消
     */
    private Integer orderStatus;
    
    /**
     * 开票状态：0-未开票，1-开票中，2-开票成功，3-开票失败
     */
    private Integer invoiceStatus;
    
    /**
     * 支付状态：0-未支付，1-支付中，2-支付成功，3-支付失败
     */
    private Integer paymentStatus;
    
    /**
     * 缴税状态：0-未缴税，1-缴税中，2-缴税成功，3-缴税失败
     */
    private Integer taxStatus;

    /**
     * 自然人确认状态：0-未确认，1-自然人确认完成，2-全部确认完成
     *
     * 枚举 {@link cn.iocoder.yudao.module.icbc.enums.InvoiceConfirmStatusEnum}
     */
    private Integer confirmStatus;

    /**
     * 预开票状态：0-初始，1-预开票中，2-预开票成功，3-预开票失败，4-预开票取消
     *
     * 枚举 {@link cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum}
     */
    private Integer preInvoiceStatus;

    /**
     * 预下单发起时间（预下单成功、等待自然人确认的时刻）
     */
    private LocalDateTime preOrderTime;
    
    /**
     * 发票号码
     */
    private String invoiceNo;
    
    /**
     * 发票代码
     */
    private String invoiceCode;
    
    /**
     * 开票日期
     */
    private LocalDateTime invoiceDate;
    
    /**
     * 发票金额
     */
    private BigDecimal invoiceAmount;
    
    /**
     * 税额
     */
    private BigDecimal taxAmount;
    
    /**
     * 发票文件URL
     */
    private String invoiceFileUrl;
    
    /**
     * 备注
     */
    private String remark;

} 