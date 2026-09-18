package cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物流运输任务 DO
 *
 * @author 芋道源码
 */
@TableName("logistics_transport_task")
@KeySequence("logistics_transport_task_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportTaskDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 任务编号
     */
    private String taskNo;
    /**
     * 订单ID
     */
    private Long orderId;
    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 所属企业ID
     */
    private Long enterpriseId;
    /**
     * 车辆ID
     */
    private Long vehicleId;
    /**
     * 司机ID
     */
    private Long driverId;
    /**
     * 分配时间
     */
    private LocalDateTime assignTime;
    /**
     * 接受时间
     */
    private LocalDateTime acceptTime;
    /**
     * 取货企业ID
     */
    private Long pickupEnterpriseId;
    /**
     * 取货地址
     */
    private String pickupAddress;
    /**
     * 取货联系人姓名
     */
    private String pickupContactName;
    /**
     * 取货联系人电话
     */
    private String pickupContactPhone;
    /**
     * 预计取货时间
     */
    private LocalDateTime expectedPickupTime;
    /**
     * 实际取货时间
     */
    private LocalDateTime actualPickupTime;
    /**
     * 送货企业ID
     */
    private Long deliveryEnterpriseId;
    /**
     * 送货地址
     */
    private String deliveryAddress;
    /**
     * 送货联系人姓名
     */
    private String deliveryContactName;
    /**
     * 送货联系人电话
     */
    private String deliveryContactPhone;
    /**
     * 预计送货时间
     */
    private LocalDateTime expectedDeliveryTime;
    /**
     * 实际送货时间
     */
    private LocalDateTime actualDeliveryTime;
    /**
     * 废料代码
     */
    private String wasteCode;
    /**
     * 废料名称
     */
    private String wasteName;
    /**
     * 预计数量
     */
    private BigDecimal estimatedQuantity;
    /**
     * 实际数量
     */
    private BigDecimal actualQuantity;
    /**
     * 数量单位
     */
    private String quantityUnit;
    /**
     * 任务状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.TransportTaskStatusEnum}
     */
    private Integer taskStatus;
    /**
     * 当前位置
     */
    private String currentLocation;
    /**
     * 当前纬度
     */
    private BigDecimal currentLatitude;
    /**
     * 当前经度
     */
    private BigDecimal currentLongitude;
    /**
     * 是否异常
     */
    private Boolean isAbnormal;
    /**
     * 异常类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.TransportTaskAbnormalTypeEnum}
     */
    private Integer abnormalType;
    /**
     * 异常原因
     */
    private String abnormalReason;
    /**
     * 是否临时任务
     */
    private Boolean isTemporary;
    /**
     * 备注
     */
    private String remark;
    /**
     * 租户ID
     */
    private Long tenantId;

} 