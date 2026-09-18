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
 * 危废转移订单 DO
 *
 * @author 芋道源码
 */
@TableName("waste_transfer_order")
@KeySequence("waste_transfer_order_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferOrderDO extends BaseDO {

    /**
     * 订单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号 (系统生成, 唯一)
     */
    private String orderNo;

    // ========== 关联信息 ==========
    /**
     * 关联预约单ID
     */
    private Long appointmentId;
    /**
     * 关联报价记录ID
     */
    private Long quotationId;

    // ========== 企业信息 ==========
    /**
     * 产废企业ID
     */
    private Long producingEnterpriseId;
    /**
     * 产废门店ID
     */
    private Long producingStoreId;
    /**
     * 回收企业ID
     */
    private Long recyclingEnterpriseId;

    // ========== 物流关联（通过接口协作） ==========
    /**
     * 关联的物流运输任务ID
     */
    private Long transportTaskId;
    /**
     * 关联的车辆过磅ID
     */
    private Long vehicleWeighingId;
    /**
     * 物流状态快照
     */
    private Integer logisticsStatus;

    // ========== 废物信息 ==========
    /**
     * 危险废物代码
     */
    private String wasteCode;
    /**
     * 危险废物名称
     */
    private String wasteName;
    /**
     * 预估数量
     */
    private BigDecimal estimatedQuantity;
    /**
     * 收运员确认数量
     */
    private BigDecimal confirmedQuantity;
    /**
     * 基于过磅的分摊数量
     */
    private BigDecimal allocatedQuantity;
    /**
     * 数量单位
     */
    private String quantityUnit;
    /**
     * 包装方式
     */
    private String packagingType;

    // ========== 价格信息 ==========
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 预估总金额
     */
    private BigDecimal estimatedAmount;
    /**
     * 最终结算金额
     */
    private BigDecimal finalAmount;

    // ========== 业务状态 ==========
    /**
     * 业务状态 (0:待确认, 1:已确认, 2:待结算, 3:已结算, 4:已完成, 5:已取消)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.OrderBusinessStatusEnum}
     */
    private Integer businessStatus;

    // ========== 付款管理 ==========
    /**
     * 关联的产废企业付款配置ID
     */
    private Long paymentConfigId;
    /**
     * 实际付款方式 (1:对公结算, 2:个人结算)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.PaymentMethodTypeEnum}
     */
    private Integer paymentMethodType;
    /**
     * 付款状态 (0:未付款, 1:已付款, 2:付款失败, 3:待凭证上传, 4:凭证已上传, 5:凭证已确认)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.PaymentStatusEnum}
     */
    private Integer paymentStatus;
    /**
     * 付款完成时间
     */
    private LocalDateTime paymentCompletedTime;
    /**
     * 对公付款凭证ID
     */
    private Long paymentVoucherId;

    // ========== 收货确认 ==========
    /**
     * 收货确认时间
     */
    private LocalDateTime pickupConfirmedTime;
    /**
     * 收货确认人
     */
    private String pickupConfirmedBy;
    /**
     * 收货确认GPS位置
     */
    private String pickupConfirmedLocation;

    // ========== 分摊信息 ==========
    /**
     * 在车辆总重量中的分摊比例
     */
    private BigDecimal allocationRatio;
    /**
     * 订单分摊完成时间
     */
    private LocalDateTime allocationCompletedTime;
    /**
     * 与预估量的差异
     */
    private BigDecimal varianceFromEstimate;
    /**
     * 差异率 (%)
     */
    private BigDecimal varianceRate;

    // ========== 订单来源 ==========
    /**
     * 订单来源类型 (0:预约转订单, 1:扫街临时订单, 2:补单)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.OrderSourceTypeEnum}
     */
    private Integer sourceType;
    /**
     * 关联订单ID (用于补单场景)
     */
    private Long relatedOrderId;

    /**
     * 用户备注
     */
    private String userRemark;
    /**
     * 内部备注
     */
    private String internalRemark;

} 