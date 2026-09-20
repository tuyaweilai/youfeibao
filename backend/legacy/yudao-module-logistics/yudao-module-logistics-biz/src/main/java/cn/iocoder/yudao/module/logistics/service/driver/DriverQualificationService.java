package cn.iocoder.yudao.module.logistics.service.driver;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.DriverQualificationDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 司机资质信息 Service 接口
 *
 * @author 芋道源码
 */
public interface DriverQualificationService {

    /**
     * 创建司机资质信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDriverQualification(@Valid DriverQualificationCreateReqVO createReqVO);

    /**
     * 更新司机资质信息
     *
     * @param updateReqVO 更新信息
     */
    void updateDriverQualification(@Valid DriverQualificationUpdateReqVO updateReqVO);

    /**
     * 删除司机资质信息
     *
     * @param id 编号
     */
    void deleteDriverQualification(Long id);

    /**
     * 获得司机资质信息
     *
     * @param id 编号
     * @return 司机资质信息
     */
    DriverQualificationDO getDriverQualification(Long id);

    /**
     * 获得司机资质信息详情
     *
     * @param id 编号
     * @return 司机资质信息详情
     */
    DriverQualificationRespVO getDriverQualificationDetail(Long id);

    /**
     * 获得司机资质信息分页
     *
     * @param pageReqVO 分页查询
     * @return 司机资质信息分页
     */
    PageResult<DriverQualificationRespVO> getDriverQualificationPage(DriverQualificationPageReqVO pageReqVO);

    /**
     * 获得司机资质信息列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 司机资质信息列表
     */
    List<DriverQualificationDO> getDriverQualificationList(DriverQualificationPageReqVO exportReqVO);

    /**
     * 根据用户ID获得司机资质信息
     *
     * @param userId 用户ID
     * @return 司机资质信息
     */
    DriverQualificationDO getDriverQualificationByUserId(Long userId);

    /**
     * 根据司机编号获得司机资质信息
     *
     * @param driverCode 司机编号
     * @return 司机资质信息
     */
    DriverQualificationDO getDriverQualificationByDriverCode(String driverCode);

    /**
     * 根据驾驶证号码获得司机资质信息
     *
     * @param drivingLicenseNo 驾驶证号码
     * @return 司机资质信息
     */
    DriverQualificationDO getDriverQualificationByDrivingLicenseNo(String drivingLicenseNo);

    /**
     * 根据企业ID获得司机资质信息列表
     *
     * @param enterpriseId 企业ID
     * @return 司机资质信息列表
     */
    List<DriverQualificationDO> getDriverQualificationListByEnterpriseId(Long enterpriseId);

    /**
     * 根据状态获得司机资质信息列表
     *
     * @param status 状态
     * @return 司机资质信息列表
     */
    List<DriverQualificationDO> getDriverQualificationListByStatus(Integer status);

    /**
     * 更新司机状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updateDriverQualificationStatus(Long id, Integer status);

} 