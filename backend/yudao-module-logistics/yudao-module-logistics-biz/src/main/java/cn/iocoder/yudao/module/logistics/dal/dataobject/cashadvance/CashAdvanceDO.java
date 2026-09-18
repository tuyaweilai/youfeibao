package cn.iocoder.yudao.module.logistics.dal.dataobject.cashadvance;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物流现金代付记录 DO
 *
 * @author 芋道源码
 */
@TableName("logistics_cash_advance")
@KeySequence("logistics_cash_advance_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashAdvanceDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 运输任务ID
     */
    private Long taskId;
    /**
     * 订单ID
     */
    private Long orderId;
    /**
     * 司机ID
     */
    private Long driverId;
    /**
     * 司机姓名
     */
    private String driverName;
    /**
     * 支付金额
     */
    private BigDecimal paymentAmount;
    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;
    /**
     * 支付地点
     */
    private String paymentLocation;
    /**
     * 支付方式
     */
    private String paymentMethod;
    /**
     * 收款人姓名
     */
    private String payeeName;
    /**
     * 收款人电话
     */
    private String payeePhone;
    /**
     * 支付现场照片URLs(JSON数组)
     */
    private String paymentPhotos;
    /**
     * 收据照片URLs(JSON数组)
     */
    private String receiptPhotos;
    /**
     * 通知状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.CashAdvanceNotifyStatusEnum}
     */
    private Integer notifyStatus;
    /**
     * 通知时间
     */
    private LocalDateTime notifyTime;
    /**
     * 对账状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.CashAdvanceReconcileStatusEnum}
     */
    private Integer reconcileStatus;
    /**
     * 对账时间
     */
    private LocalDateTime reconcileTime;
    /**
     * 对账备注
     */
    private String reconcileRemark;
    /**
     * 对账操作员ID
     */
    private Long reconcileOperatorId;
    /**
     * 对账操作员姓名
     */
    private String reconcileOperatorName;
    /**
     * 备注
     */
    private String remark;
    /**
     * 租户ID
     */
    private Long tenantId;

} 