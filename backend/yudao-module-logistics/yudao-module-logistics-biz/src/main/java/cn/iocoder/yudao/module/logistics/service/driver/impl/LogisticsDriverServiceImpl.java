package cn.iocoder.yudao.module.logistics.service.driver.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.driver.LogisticsDriverMapper;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.DRIVER_NOT_EXISTS;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.DRIVER_USER_DUPLICATE;

/**
 * 司机档案 Service 实现（V2a #77）。
 */
@Service
@Validated
public class LogisticsDriverServiceImpl implements LogisticsDriverService {

    @Resource
    private LogisticsDriverMapper logisticsDriverMapper;

    @Override
    public Long createDriver(LogisticsDriverSaveReqVO createReqVO) {
        assertUserIdAvailable(createReqVO.getUserId(), null);
        LogisticsDriverDO driver = BeanUtils.toBean(createReqVO, LogisticsDriverDO.class);
        logisticsDriverMapper.insert(driver);
        return driver.getId();
    }

    @Override
    public void updateDriver(LogisticsDriverSaveReqVO updateReqVO) {
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
    public PageResult<LogisticsDriverDO> getDriverPage(LogisticsDriverPageReqVO pageReqVO) {
        return logisticsDriverMapper.selectPage(pageReqVO);
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
