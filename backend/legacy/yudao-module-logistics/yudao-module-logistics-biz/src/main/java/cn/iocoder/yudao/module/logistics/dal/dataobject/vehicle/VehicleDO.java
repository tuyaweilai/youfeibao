package cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 车辆信息 DO
 *
 * @author 芋道源码
 */
@TableName("logistics_vehicle")
@KeySequence("logistics_vehicle_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 所属企业ID（关联系统企业表）
     */
    private Long enterpriseId;
    /**
     * 车牌号
     */
    private String plateNumber;
    /**
     * 车辆类型
     */
    private String vehicleType;
    /**
     * 载重能力(kg)
     */
    private BigDecimal capacityKg;
    /**
     * GPS设备ID
     */
    private String gpsDeviceId;
    /**
     * 车辆状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.VehicleStatusEnum}
     */
    private Integer status;
    /**
     * 行驶证到期日期
     */
    private LocalDate licenseExpiryDate;
    /**
     * 保险到期日期
     */
    private LocalDate insuranceExpiryDate;
    /**
     * 上次维护日期
     */
    private LocalDate maintenanceDate;
    /**
     * 车辆照片URLs(JSON数组)
     */
    private String vehiclePhotos;
    /**
     * 行驶证照片URLs(JSON数组)
     */
    private String licensePhotos;
    /**
     * 租户ID
     */
    private Long tenantId;

} 