package cn.iocoder.yudao.module.logistics.dal.dataobject.driver;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * 司机资质信息 DO
 *
 * @author 芋道源码
 */
@TableName("logistics_driver_qualification")
@KeySequence("logistics_driver_qualification_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverQualificationDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 关联的用户ID
     */
    private Long userId;
    /**
     * 所属企业ID（关联系统企业表）
     */
    private Long enterpriseId;
    /**
     * 司机编号
     */
    private String driverCode;
    /**
     * 驾驶证号码
     */
    private String drivingLicenseNo;
    /**
     * 驾驶证类型
     */
    private String drivingLicenseType;
    /**
     * 驾驶证到期日期
     */
    private LocalDate drivingLicenseExpiryDate;
    /**
     * 从业资格证号码
     */
    private String qualificationCertNo;
    /**
     * 从业资格证到期日期
     */
    private LocalDate qualificationCertExpiryDate;
    /**
     * 危险品运输资质证号
     */
    private String hazardousTransportCertNo;
    /**
     * 危险品运输资质证到期日期
     */
    private LocalDate hazardousTransportCertExpiryDate;
    /**
     * 准驾车型
     */
    private String vehicleTypePermitted;
    /**
     * 驾龄(年)
     */
    private Integer yearsOfExperience;
    /**
     * 司机状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.DriverStatusEnum}
     */
    private Integer status;
    /**
     * 入职日期
     */
    private LocalDate joinDate;
    /**
     * 离职日期
     */
    private LocalDate leaveDate;
    /**
     * 驾驶证照片URLs(JSON数组)
     */
    private String driverLicensePhotos;
    /**
     * 从业资格证照片URLs(JSON数组)
     */
    private String qualificationCertPhotos;
    /**
     * 危险品运输资质证照片URLs(JSON数组)
     */
    private String hazardousCertPhotos;
    /**
     * 最近一次培训日期
     */
    private LocalDate lastTrainingDate;
    /**
     * 下次培训日期
     */
    private LocalDate nextTrainingDate;
    /**
     * 备注
     */
    private String remark;
    /**
     * 租户ID
     */
    private Long tenantId;

} 