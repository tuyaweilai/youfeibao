package cn.iocoder.yudao.module.logistics.service.driver;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.*;
import cn.iocoder.yudao.module.logistics.convert.driver.DriverQualificationConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.DriverQualificationDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.driver.DriverQualificationMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 司机资质信息 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class DriverQualificationServiceImpl implements DriverQualificationService {

    @Resource
    private DriverQualificationMapper driverQualificationMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public Long createDriverQualification(DriverQualificationCreateReqVO createReqVO) {
        // 校验司机编号唯一性
        validateDriverCodeUnique(null, createReqVO.getDriverCode());
        // 校验驾驶证号码唯一性
        validateDrivingLicenseNoUnique(null, createReqVO.getDrivingLicenseNo());
        // 校验用户是否存在
        validateUserExists(createReqVO.getUserId());

        // 插入
        DriverQualificationDO driverQualification = DriverQualificationConvert.INSTANCE.convert(createReqVO);
        driverQualificationMapper.insert(driverQualification);
        // 返回
        return driverQualification.getId();
    }

    @Override
    public void updateDriverQualification(DriverQualificationUpdateReqVO updateReqVO) {
        // 校验存在
        validateDriverQualificationExists(updateReqVO.getId());
        // 校验司机编号唯一性
        validateDriverCodeUnique(updateReqVO.getId(), updateReqVO.getDriverCode());
        // 校验驾驶证号码唯一性
        validateDrivingLicenseNoUnique(updateReqVO.getId(), updateReqVO.getDrivingLicenseNo());
        // 校验用户是否存在
        validateUserExists(updateReqVO.getUserId());

        // 更新
        DriverQualificationDO updateObj = DriverQualificationConvert.INSTANCE.convert(updateReqVO);
        driverQualificationMapper.updateById(updateObj);
    }

    @Override
    public void deleteDriverQualification(Long id) {
        // 校验存在
        validateDriverQualificationExists(id);
        // 删除
        driverQualificationMapper.deleteById(id);
    }

    private void validateDriverQualificationExists(Long id) {
        if (driverQualificationMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(DRIVER_NOT_EXISTS);
        }
    }

    private void validateDriverCodeUnique(Long id, String driverCode) {
        if (StrUtil.isBlank(driverCode)) {
            return;
        }
        DriverQualificationDO driverQualification = driverQualificationMapper.selectByDriverCode(driverCode);
        if (driverQualification == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的司机
        if (id == null) {
            throw ServiceExceptionUtil.exception(DRIVER_CODE_EXISTS);
        }
        if (!driverQualification.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(DRIVER_CODE_EXISTS);
        }
    }

    private void validateDrivingLicenseNoUnique(Long id, String drivingLicenseNo) {
        if (StrUtil.isBlank(drivingLicenseNo)) {
            return;
        }
        DriverQualificationDO driverQualification = driverQualificationMapper.selectByDrivingLicenseNo(drivingLicenseNo);
        if (driverQualification == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的司机
        if (id == null) {
            throw ServiceExceptionUtil.exception(DRIVER_LICENSE_NO_EXISTS);
        }
        if (!driverQualification.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(DRIVER_LICENSE_NO_EXISTS);
        }
    }

    private void validateUserExists(Long userId) {
        if (userId == null) {
            return;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            throw ServiceExceptionUtil.exception(DRIVER_NOT_EXISTS);
        }
    }

    @Override
    public DriverQualificationDO getDriverQualification(Long id) {
        return driverQualificationMapper.selectById(id);
    }

    @Override
    public DriverQualificationRespVO getDriverQualificationDetail(Long id) {
        DriverQualificationDO driverQualification = getDriverQualification(id);
        if (driverQualification == null) {
            return null;
        }

        // 转换基本信息
        DriverQualificationRespVO respVO = DriverQualificationConvert.INSTANCE.convert(driverQualification);

        // 填充用户信息
        if (driverQualification.getUserId() != null) {
            AdminUserRespDTO user = adminUserApi.getUser(driverQualification.getUserId());
            if (user != null) {
                respVO.setUserName(user.getNickname());
                respVO.setUserMobile(user.getMobile());
            }
        }

        // TODO: 填充企业信息（需要企业模块API）
        // if (driverQualification.getEnterpriseId() != null) {
        //     EnterpriseRespDTO enterprise = enterpriseApi.getEnterprise(driverQualification.getEnterpriseId());
        //     if (enterprise != null) {
        //         respVO.setEnterpriseName(enterprise.getName());
        //     }
        // }

        return respVO;
    }

    @Override
    public PageResult<DriverQualificationRespVO> getDriverQualificationPage(DriverQualificationPageReqVO pageReqVO) {
        PageResult<DriverQualificationDO> pageResult = driverQualificationMapper.selectPage(pageReqVO);
        
        // 转换基本信息
        PageResult<DriverQualificationRespVO> result = DriverQualificationConvert.INSTANCE.convertPage(pageResult);
        
        // 批量填充用户信息
        result.getList().forEach(respVO -> {
            if (respVO.getUserId() != null) {
                AdminUserRespDTO user = adminUserApi.getUser(respVO.getUserId());
                if (user != null) {
                    respVO.setUserName(user.getNickname());
                    respVO.setUserMobile(user.getMobile());
                }
            }
            // TODO: 填充企业信息
        });
        
        return result;
    }

    @Override
    public List<DriverQualificationDO> getDriverQualificationList(DriverQualificationPageReqVO exportReqVO) {
        return driverQualificationMapper.selectList(exportReqVO);
    }

    @Override
    public DriverQualificationDO getDriverQualificationByUserId(Long userId) {
        return driverQualificationMapper.selectByUserId(userId);
    }

    @Override
    public DriverQualificationDO getDriverQualificationByDriverCode(String driverCode) {
        return driverQualificationMapper.selectByDriverCode(driverCode);
    }

    @Override
    public DriverQualificationDO getDriverQualificationByDrivingLicenseNo(String drivingLicenseNo) {
        return driverQualificationMapper.selectByDrivingLicenseNo(drivingLicenseNo);
    }

    @Override
    public List<DriverQualificationDO> getDriverQualificationListByEnterpriseId(Long enterpriseId) {
        return driverQualificationMapper.selectListByEnterpriseId(enterpriseId);
    }

    @Override
    public List<DriverQualificationDO> getDriverQualificationListByStatus(Integer status) {
        return driverQualificationMapper.selectListByStatus(status);
    }

    @Override
    public void updateDriverQualificationStatus(Long id, Integer status) {
        // 校验存在
        validateDriverQualificationExists(id);
        
        // 更新状态
        DriverQualificationDO updateObj = new DriverQualificationDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        driverQualificationMapper.updateById(updateObj);
    }

}