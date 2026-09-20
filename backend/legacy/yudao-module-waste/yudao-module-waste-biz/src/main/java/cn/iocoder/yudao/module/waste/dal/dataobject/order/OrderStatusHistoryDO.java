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
 * 订单状态变更历史 DO
 *
 * @author 芋道源码
 */
@TableName("waste_order_status_history")
@KeySequence("waste_order_status_history_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryDO extends BaseDO {

    /**
     * 状态历史ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    // ========== 状态变更信息 ==========
    /**
     * 变更前状态
     */
    private Integer statusFrom;

    /**
     * 变更后状态
     */
    private Integer statusTo;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 变更类型 (1:正常流转, 2:异常处理, 3:人工干预, 4:系统回滚)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.StatusChangeTypeEnum}
     */
    private Integer changeType;

    /**
     * 状态变更原因
     */
    private String changeReason;

    // ========== 操作者信息 ==========
    /**
     * 操作者类型 (1:产废企业, 2:回收企业, 3:物流企业, 4:系统自动, 5:平台管理员)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.OperatorTypeEnum}
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
     * 操作者IP地址
     */
    private String operatorIp;

    // ========== 时间信息 ==========
    /**
     * 状态变更时间
     */
    private LocalDateTime changeTime;

    /**
     * 在前一状态的停留时长(秒)
     */
    private Integer durationSeconds;

    /**
     * 是否关键里程碑
     */
    private Boolean milestoneFlag;

    // ========== 扩展的业务数据 ==========
    /**
     * 付款方式 (状态为已确认收货时)
     */
    private Integer paymentMethod;

    /**
     * 现金金额 (现金付款时)
     */
    private BigDecimal cashAmount;

    /**
     * 线上金额 (线上付款时)
     */
    private BigDecimal onlineAmount;

    /**
     * 关联的车辆过磅ID
     */
    private Long vehicleWeighingId;

    /**
     * 分摊比例 (分摊完成时)
     */
    private BigDecimal allocationRatio;

    /**
     * 业务相关数据 (JSON格式)
     */
    private String businessData;

    /**
     * GPS位置信息
     */
    private String locationInfo;

    /**
     * 备注说明
     */
    private String remark;

} 