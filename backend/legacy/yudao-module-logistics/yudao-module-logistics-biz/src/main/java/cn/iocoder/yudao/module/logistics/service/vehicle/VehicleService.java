package cn.iocoder.yudao.module.logistics.service.vehicle;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.VehicleDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 车辆信息 Service 接口
 *
 * @author 芋道源码
 */
public interface VehicleService {

    /**
     * 创建车辆信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createVehicle(@Valid VehicleCreateReqVO createReqVO);

    /**
     * 更新车辆信息
     *
     * @param updateReqVO 更新信息
     */
    void updateVehicle(@Valid VehicleUpdateReqVO updateReqVO);

    /**
     * 删除车辆信息
     *
     * @param id 编号
     */
    void deleteVehicle(Long id);

    /**
     * 获得车辆信息
     *
     * @param id 编号
     * @return 车辆信息
     */
    VehicleDO getVehicle(Long id);

    /**
     * 获得车辆信息详情
     *
     * @param id 编号
     * @return 车辆信息详情
     */
    VehicleRespVO getVehicleDetail(Long id);

    /**
     * 获得车辆信息分页
     *
     * @param pageReqVO 分页查询
     * @return 车辆信息分页
     */
    PageResult<VehicleRespVO> getVehiclePage(VehiclePageReqVO pageReqVO);

    /**
     * 获得车辆信息列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 车辆信息列表
     */
    List<VehicleDO> getVehicleList(VehiclePageReqVO exportReqVO);

    /**
     * 根据车牌号获得车辆信息
     *
     * @param plateNumber 车牌号
     * @return 车辆信息
     */
    VehicleDO getVehicleByPlateNumber(String plateNumber);

    /**
     * 根据企业ID获得车辆信息列表
     *
     * @param enterpriseId 企业ID
     * @return 车辆信息列表
     */
    List<VehicleDO> getVehicleListByEnterpriseId(Long enterpriseId);

    /**
     * 根据状态获得车辆信息列表
     *
     * @param status 车辆状态
     * @return 车辆信息列表
     */
    List<VehicleDO> getVehicleListByStatus(Integer status);

    /**
     * 更新车辆状态
     *
     * @param id 车辆ID
     * @param status 新状态
     */
    void updateVehicleStatus(Long id, Integer status);

    /**
     * 校验车辆是否存在
     *
     * @param id 车辆ID
     * @return 车辆信息
     */
    VehicleDO validateVehicleExists(Long id);

} 