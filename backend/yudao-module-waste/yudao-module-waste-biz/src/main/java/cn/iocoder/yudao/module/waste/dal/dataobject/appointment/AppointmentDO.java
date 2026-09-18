package cn.iocoder.yudao.module.waste.dal.dataobject.appointment;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 危废转移预约 DO
 *
 * @author 芋道源码
 */
@TableName("waste_transfer_appointment")
@KeySequence("waste_transfer_appointment_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDO extends BaseDO {

    /**
     * 预约ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 预约单号
     */
    private String appointmentNo;

    // ========== 产废企业信息 ==========
    /**
     * 产废企业ID
     */
    private Long producerEnterpriseId;
    /**
     * 产废企业名称
     */
    private String producerEnterpriseName;
    /**
     * 产废企业联系人
     */
    private String producerContactName;
    /**
     * 产废企业联系电话
     */
    private String producerContactPhone;

    // ========== 回收企业信息 ==========
    /**
     * 回收企业ID
     */
    private Long recyclerEnterpriseId;
    /**
     * 回收企业名称
     */
    private String recyclerEnterpriseName;
    /**
     * 分配方式
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.AssignmentTypeEnum}
     */
    private Integer assignmentType;
    /**
     * 分配时间
     */
    private LocalDateTime assignmentTime;
    /**
     * 分配操作人
     */
    private String assignmentOperator;

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
     * 废物类别
     */
    private String wasteCategory;
    /**
     * 预估数量
     */
    private BigDecimal estimatedQuantity;
    /**
     * 数量单位
     */
    private String quantityUnit;
    /**
     * 废物描述
     */
    private String wasteDescription;

    // ========== 地址信息 ==========
    /**
     * 取货地址
     */
    private String pickupAddress;
    /**
     * 取货地址纬度
     */
    private BigDecimal pickupLatitude;
    /**
     * 取货地址经度
     */
    private BigDecimal pickupLongitude;
    /**
     * 送货地址
     */
    private String deliveryAddress;
    /**
     * 送货地址纬度
     */
    private BigDecimal deliveryLatitude;
    /**
     * 送货地址经度
     */
    private BigDecimal deliveryLongitude;

    // ========== 时间信息 ==========
    /**
     * 期望取货时间
     */
    private LocalDateTime expectedPickupTime;
    /**
     * 期望送达时间
     */
    private LocalDateTime expectedDeliveryTime;

    // ========== 状态信息 ==========
    /**
     * 预约状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.AppointmentStatusEnum}
     */
    private Integer appointmentStatus;
    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;
    /**
     * 拒绝时间
     */
    private LocalDateTime rejectTime;
    /**
     * 拒绝原因
     */
    private String rejectReason;
    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;
    /**
     * 取消原因
     */
    private String cancelReason;

    // ========== 业务信息 ==========
    /**
     * 业务模式
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.BusinessModeEnum}
     */
    private Integer businessMode;
    /**
     * 是否紧急
     */
    private Boolean isUrgent;
    /**
     * 优先级
     */
    private Integer priorityLevel;

    // ========== 关联信息 ==========
    /**
     * 关联订单ID
     */
    private Long orderId;
    /**
     * 关联合同ID
     */
    private Long contractId;

    /**
     * 备注
     */
    private String remark;

    // ========== 业务方法 ==========
    
    /**
     * 获取预约时间
     * 优先返回期望取货时间，如果为空则返回期望送达时间
     */
    public LocalDateTime getAppointmentTime() {
        return expectedPickupTime != null ? expectedPickupTime : expectedDeliveryTime;
    }
    
    /**
     * 设置预约状态
     */
    public void setStatus(Integer status) {
        this.appointmentStatus = status;
    }

} 