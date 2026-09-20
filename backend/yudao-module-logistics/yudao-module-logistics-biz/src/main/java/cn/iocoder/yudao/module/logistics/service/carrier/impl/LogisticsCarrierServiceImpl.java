package cn.iocoder.yudao.module.logistics.service.carrier.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.carrier.LogisticsCarrierMapper;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 承运商档案 Service 实现（V3 #70）。
 */
@Service
@Validated
public class LogisticsCarrierServiceImpl implements LogisticsCarrierService {

    /** 合作中 */
    private static final Integer ACTIVE = 0;

    @Resource
    private LogisticsCarrierMapper logisticsCarrierMapper;

    @Override
    public Long createCarrier(LogisticsCarrierSaveReqVO createReqVO) {
        assertNameAvailable(createReqVO.getName(), null);
        LogisticsCarrierDO carrier = BeanUtils.toBean(createReqVO, LogisticsCarrierDO.class);
        logisticsCarrierMapper.insert(carrier);
        return carrier.getId();
    }

    @Override
    public void updateCarrier(LogisticsCarrierSaveReqVO updateReqVO) {
        LogisticsCarrierDO exists = getCarrier(updateReqVO.getId());
        assertNameAvailable(updateReqVO.getName(), exists.getId());
        logisticsCarrierMapper.updateById(BeanUtils.toBean(updateReqVO, LogisticsCarrierDO.class));
    }

    @Override
    public void deleteCarrier(Long id) {
        getCarrier(id);
        logisticsCarrierMapper.deleteById(id);
    }

    @Override
    public LogisticsCarrierDO getCarrier(Long id) {
        LogisticsCarrierDO carrier = logisticsCarrierMapper.selectById(id);
        if (carrier == null) {
            throw exception(CARRIER_NOT_EXISTS);
        }
        return carrier;
    }

    @Override
    public LogisticsCarrierDO getAssignableCarrier(Long carrierId) {
        LogisticsCarrierDO carrier = getCarrier(carrierId);
        if (!ACTIVE.equals(carrier.getStatus())) {
            throw exception(CARRIER_NOT_ACTIVE);
        }
        return carrier;
    }

    @Override
    public PageResult<LogisticsCarrierDO> getCarrierPage(LogisticsCarrierPageReqVO pageReqVO) {
        return logisticsCarrierMapper.selectPage(pageReqVO);
    }

    @Override
    public List<LogisticsCarrierDO> getCarrierList(LogisticsCarrierPageReqVO exportReqVO) {
        return logisticsCarrierMapper.selectList(exportReqVO);
    }

    private void assertNameAvailable(String name, Long id) {
        LogisticsCarrierDO exists = logisticsCarrierMapper.selectByName(name);
        if (exists == null) {
            return;
        }
        if (!Objects.equals(exists.getId(), id)) {
            throw exception(CARRIER_NAME_DUPLICATE);
        }
    }

}
