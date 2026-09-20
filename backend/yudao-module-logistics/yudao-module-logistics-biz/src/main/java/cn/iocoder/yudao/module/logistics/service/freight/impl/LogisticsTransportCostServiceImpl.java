package cn.iocoder.yudao.module.logistics.service.freight.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.freight.LogisticsTransportCostDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.freight.LogisticsTransportCostMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBearerEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportCostTypeEnum;
import cn.iocoder.yudao.module.logistics.service.freight.LogisticsTransportCostService;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 运输费用（内部成本）Service 实现（V8 #75）。
 *
 * <p>校验很直白：类型与承担方都必须是登记过的枚举（避免拼错变成第三种），金额不能为负，
 * 任务必须存在。**本表金额不流向任何运费单或收购单**——它就是成本台账。
 */
@Service
@Validated
public class LogisticsTransportCostServiceImpl implements LogisticsTransportCostService {

    @Resource
    private LogisticsTransportCostMapper logisticsTransportCostMapper;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;

    @Override
    public Long createTransportCost(LogisticsTransportCostSaveReqVO createReqVO) {
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTask(createReqVO.getTaskId());
        assertCostValid(createReqVO);
        LogisticsTransportCostDO cost = BeanUtils.toBean(createReqVO, LogisticsTransportCostDO.class);
        cost.setId(null);
        cost.setTaskNo(task.getTaskNo());
        logisticsTransportCostMapper.insert(cost);
        return cost.getId();
    }

    @Override
    public void updateTransportCost(LogisticsTransportCostSaveReqVO updateReqVO) {
        getTransportCost(updateReqVO.getId());
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTask(updateReqVO.getTaskId());
        assertCostValid(updateReqVO);
        LogisticsTransportCostDO update = BeanUtils.toBean(updateReqVO, LogisticsTransportCostDO.class);
        update.setTaskNo(task.getTaskNo());
        logisticsTransportCostMapper.updateById(update);
    }

    @Override
    public void deleteTransportCost(Long id) {
        getTransportCost(id);
        logisticsTransportCostMapper.deleteById(id);
    }

    @Override
    public LogisticsTransportCostDO getTransportCost(Long id) {
        LogisticsTransportCostDO cost = id == null ? null : logisticsTransportCostMapper.selectById(id);
        if (cost == null) {
            throw exception(TRANSPORT_COST_NOT_EXISTS);
        }
        return cost;
    }

    @Override
    public PageResult<LogisticsTransportCostDO> getTransportCostPage(LogisticsTransportCostPageReqVO pageReqVO) {
        return logisticsTransportCostMapper.selectPage(pageReqVO);
    }

    @Override
    public List<LogisticsTransportCostDO> getTransportCostList(LogisticsTransportCostPageReqVO exportReqVO) {
        return logisticsTransportCostMapper.selectList(exportReqVO);
    }

    @Override
    public List<LogisticsTransportCostDO> getTransportCostListByTaskId(Long taskId) {
        return taskId == null ? List.of() : logisticsTransportCostMapper.selectListByTaskId(taskId);
    }

    @Override
    public LogisticsTransportCostRespVO toResp(LogisticsTransportCostDO cost) {
        LogisticsTransportCostRespVO resp = BeanUtils.toBean(cost, LogisticsTransportCostRespVO.class);
        resp.setCostTypeName(LogisticsTransportCostTypeEnum.nameOf(cost.getCostType()));
        resp.setBearerName(LogisticsFreightBearerEnum.nameOf(cost.getBearer()));
        return resp;
    }

    // ==================== 内部方法 ====================

    private void assertCostValid(LogisticsTransportCostSaveReqVO reqVO) {
        if (LogisticsTransportCostTypeEnum.ofType(reqVO.getCostType()).isEmpty()) {
            throw exception(TRANSPORT_COST_TYPE_UNKNOWN);
        }
        if (LogisticsFreightBearerEnum.ofBearer(reqVO.getBearer()).isEmpty()) {
            throw exception(TRANSPORT_COST_BEARER_UNKNOWN);
        }
        if (reqVO.getAmount() == null || reqVO.getAmount().signum() < 0) {
            throw exception(TRANSPORT_COST_AMOUNT_INVALID);
        }
    }

}
