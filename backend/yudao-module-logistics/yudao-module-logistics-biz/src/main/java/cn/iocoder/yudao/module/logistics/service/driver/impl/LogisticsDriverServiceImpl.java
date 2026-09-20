package cn.iocoder.yudao.module.logistics.service.driver.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.driver.LogisticsDriverMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierService;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 司机档案 Service 实现（V2a #77）。
 */
@Service
@Validated
public class LogisticsDriverServiceImpl implements LogisticsDriverService {

    @Resource
    private LogisticsDriverMapper logisticsDriverMapper;
    @Resource
    private LogisticsCarrierService logisticsCarrierService;

    @Override
    public Long createDriver(LogisticsDriverSaveReqVO createReqVO) {
        assertCarrierUsable(createReqVO);
        assertUserIdAvailable(createReqVO.getUserId(), null);
        LogisticsDriverDO driver = BeanUtils.toBean(createReqVO, LogisticsDriverDO.class);
        logisticsDriverMapper.insert(driver);
        return driver.getId();
    }

    @Override
    public void updateDriver(LogisticsDriverSaveReqVO updateReqVO) {
        assertCarrierUsable(updateReqVO);
        LogisticsDriverDO exists = getDriver(updateReqVO.getId());
        assertUserIdAvailable(updateReqVO.getUserId(), exists.getId());
        LogisticsDriverDO update = BeanUtils.toBean(updateReqVO, LogisticsDriverDO.class);
        logisticsDriverMapper.updateById(update);
    }

    @Override
    public void deleteDriver(Long id) {
        getDriver(id);
        logisticsDriverMapper.deleteById(id);
    }

    @Override
    public LogisticsDriverDO getDriver(Long id) {
        LogisticsDriverDO driver = logisticsDriverMapper.selectById(id);
        if (driver == null) {
            throw exception(DRIVER_NOT_EXISTS);
        }
        return driver;
    }

    @Override
    public LogisticsDriverDO getDriverByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return logisticsDriverMapper.selectByUserId(userId);
    }

    @Override
    public LogisticsDriverDO getAssignableDriver(Long driverId) {
        LogisticsDriverDO driver = getAssignableDriverAllowingExpiredDocuments(driverId);
        if (isDocumentExpired(driver)) {
            throw exception(DRIVER_DOCUMENT_EXPIRED);
        }
        return driver;
    }

    @Override
    public LogisticsDriverDO getAssignableDriverAllowingExpiredDocuments(Long driverId) {
        LogisticsDriverDO driver = getDriver(driverId);
        if (!LogisticsDriverStatusEnum.ACTIVE.getStatus().equals(driver.getStatus())) {
            throw exception(TRANSPORT_TASK_DRIVER_NOT_ACTIVE);
        }
        return driver;
    }

    @Override
    public boolean isDocumentExpired(LogisticsDriverDO driver) {
        java.time.LocalDate today = java.time.LocalDate.now();
        return isBeforeToday(driver.getDrivingLicenseExpiryDate(), today)
                || isBeforeToday(driver.getQualificationCertExpiryDate(), today);
    }

    @Override
    public PageResult<LogisticsDriverDO> getDriverPage(LogisticsDriverPageReqVO pageReqVO) {
        return logisticsDriverMapper.selectPage(pageReqVO);
    }

    @Override
    public java.util.List<LogisticsDriverDO> getDriverList(LogisticsDriverPageReqVO exportReqVO) {
        return logisticsDriverMapper.selectList(exportReqVO);
    }

    /**
     * 来源是承运商时必须落到一个**合作中**的承运商档案上：否则将来「运费付给谁」没有对象。
     */
    private void assertCarrierUsable(LogisticsDriverSaveReqVO reqVO) {
        if (!LogisticsDriverSourceEnum.CARRIER.getSource().equals(reqVO.getSource())) {
            return;
        }
        if (reqVO.getCarrierId() == null) {
            throw exception(DRIVER_CARRIER_REQUIRED);
        }
        logisticsCarrierService.getAssignableCarrier(reqVO.getCarrierId());
    }

    /** 到期日早于今天即过期；没填到期日视为「未登记」，不拦 */
    private boolean isBeforeToday(java.time.LocalDate expiryDate, java.time.LocalDate today) {
        return expiryDate != null && expiryDate.isBefore(today);
    }

    private void assertUserIdAvailable(Long userId, Long id) {
        LogisticsDriverDO exists = logisticsDriverMapper.selectByUserId(userId);
        if (exists == null) {
            return;
        }
        if (!Objects.equals(exists.getId(), id)) {
            throw exception(DRIVER_USER_DUPLICATE);
        }
    }

}
