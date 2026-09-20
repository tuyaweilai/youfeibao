package cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 车辆档案 DO（V2a #77）。
 *
 * <p>车辆是「执行运输任务的车」，以车牌为对外标识。本票只做最小档案（车牌、类型、载重、状态）；
 * 行驶证与保险到期日、照片、GPS 设备、以及「证件过期不得派出」的门禁归 V3（#70）。
 *
 * <p>车辆是**租户内**的档案：自有的车与承运商的车都建在回收企业名下，承运商只作为司机来源标记
 * （见 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum}）。
 */
@TableName("logistics_vehicle")
@KeySequence("logistics_vehicle_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsVehicleDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 车牌号（租户内唯一，对外标识） */
    private String plateNo;

    /** 车辆类型（如 厢式货车 / 平板 / 自卸） */
    private String vehicleType;

    /** 载重能力（吨） */
    private BigDecimal capacityTon;

    /**
     * 车辆状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum}
     */
    private Integer status;

    /** 备注 */
    private String remark;

}
