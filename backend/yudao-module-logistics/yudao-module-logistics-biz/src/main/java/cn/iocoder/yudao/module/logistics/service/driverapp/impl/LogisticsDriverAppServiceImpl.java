package cn.iocoder.yudao.module.logistics.service.driverapp.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportAbnormalReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.driverapp.LogisticsDriverAppService;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import cn.iocoder.yudao.module.logistics.service.transporthandover.LogisticsTransportHandoverService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.DRIVER_PROFILE_NOT_FOUND;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.TRANSPORT_TASK_NOT_BELONG_TO_DRIVER;

/**
 * 司机端 Service 实现（V2c #79）。
 */
@Service
@Validated
public class LogisticsDriverAppServiceImpl implements LogisticsDriverAppService {

    @Resource
    private LogisticsDriverService logisticsDriverService;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;
    @Resource
    private LogisticsTransportHandoverService logisticsTransportHandoverService;

    @Override
    public LogisticsDriverDO getCurrentDriver() {
        LogisticsDriverDO driver = logisticsDriverService.getDriverByUserId(SecurityFrameworkUtils.getLoginUserId());
        if (driver == null) {
            // 建档是管理员的事（第三方司机也由回收企业建档，ADR 0032 第 9 条），所以提示要说清找谁
            throw exception(DRIVER_PROFILE_NOT_FOUND);
        }
        return driver;
    }

    @Override
    public PageResult<LogisticsTransportTaskDO> getMyTaskPage(LogisticsTransportTaskPageReqVO pageReqVO) {
        LogisticsDriverDO driver = getCurrentDriver();
        // 强制按登录账号的司机编号过滤：入参里的 driverId 一律不信（否则司机能翻别人的活）
        pageReqVO.setDriverId(driver.getId());
        return logisticsTransportTaskService.getTaskPage(pageReqVO);
    }

    @Override
    public LogisticsTransportTaskDO getMyTask(Long taskId) {
        LogisticsDriverDO driver = getCurrentDriver();
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTask(taskId);
        if (!Objects.equals(task.getDriverId(), driver.getId())) {
            throw exception(TRANSPORT_TASK_NOT_BELONG_TO_DRIVER);
        }
        return task;
    }

    @Override
    public void acceptMyTask(Long taskId) {
        getMyTask(taskId);
        logisticsTransportTaskService.acceptTask(taskId);
    }

    @Override
    public Long reportMyNode(LogisticsTransportNodeReportReqVO reportReqVO) {
        getMyTask(reportReqVO.getTaskId());
        return logisticsTransportNodeService.reportNode(reportReqVO);
    }

    @Override
    public Long reportMyAbnormal(LogisticsTransportAbnormalReportReqVO reportReqVO) {
        getMyTask(reportReqVO.getTaskId());
        return logisticsTransportNodeService.reportAbnormal(reportReqVO);
    }

    @Override
    public Long createMyHandover(LogisticsTransportHandoverCreateReqVO reqVO) {
        // 归属校验与节点上报同一处：不是派给我的任务就报「这不是派给你的任务」
        getMyTask(reqVO.getTaskId());
        return logisticsTransportHandoverService.createHandover(reqVO);
    }

    @Override
    public List<LogisticsTransportHandoverRespVO> getMyHandoverList(Long taskId) {
        getMyTask(taskId);
        return logisticsTransportHandoverService.getHandoverListByTaskId(taskId).stream()
                .map(logisticsTransportHandoverService::toResp).toList();
    }

}
