package cn.iocoder.yudao.module.logistics.dal.dataobject.temporaryorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物流临时订单 DO
 *
 * @author 芋道源码
 */
@TableName("logistics_temporary_order")
@KeySequence("logistics_temporary_order_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryOrderDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 运输任务ID
     */
    private Long taskId;
    /**
     * 司机ID
     */
    private Long driverId;
    /**
     * 司机姓名
     */
    private String driverName;
    /**
     * 废料类型
     */
    private String wasteType;
    /**
     * 废料名称
     */
    private String wasteName;
    /**
     * 预计数量
     */
    private BigDecimal estimatedQuantity;
    /**
     * 数量单位
     */
    private String quantityUnit;
    /**
     * 取货地址
     */
    private String pickupLocation;
    /**
     * 取货纬度
     */
    private BigDecimal pickupLatitude;
    /**
     * 取货经度
     */
    private BigDecimal pickupLongitude;
    /**
     * 取货时间
     */
    private LocalDateTime pickupTime;
    /**
     * 产废方姓名
     */
    private String producerName;
    /**
     * 产废方电话
     */
    private String producerPhone;
    /**
     * 产废方身份证号
     */
    private String producerIdCard;
    /**
     * 发现现场照片URLs(JSON数组)
     */
    private String discoveryPhotos;
    /**
     * 预估价值
     */
    private BigDecimal estimatedValue;
    /**
     * 支付状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.TemporaryOrderPaymentStatusEnum}
     */
    private Integer paymentStatus;
    /**
     * 支付金额
     */
    private BigDecimal paymentAmount;
    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;
    /**
     * 支付方式
     */
    private String paymentMethod;
    /**
     * 支付凭证URL
     */
    private String paymentVoucherUrl;
    /**
     * 是否已转为正式订单
     */
    private Boolean convertedToFormal;
    /**
     * 正式订单ID
     */
    private Long formalOrderId;
    /**
     * 转换时间
     */
    private LocalDateTime conversionTime;
    /**
     * 备注
     */
    private String remark;
    /**
     * 租户ID
     */
    private Long tenantId;

} 