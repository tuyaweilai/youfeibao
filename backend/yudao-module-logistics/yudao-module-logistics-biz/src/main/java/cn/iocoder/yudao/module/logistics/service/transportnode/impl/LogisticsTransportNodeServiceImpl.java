package cn.iocoder.yudao.module.logistics.service.transportnode.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportAbnormalReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportAbnormalResolveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportStopDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transportnode.LogisticsTransportNodeMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportAbnormalTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportStopStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
import cn.iocoder.yudao.module.logistics.service.transportnode.TransportNodeConverter;
import cn.iocoder.yudao.module.logistics.service.transportstop.LogisticsTransportStopService;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 运输节点 Service 实现（V2b #78；V4 #71 开全五类并加异常；V5 #72 节点归到停靠点）。
 *
 * <p>四件事必须做对：
 * <ol>
 *   <li><b>写入幂等</b>——弱网补传会重复提交同一条事实；</li>
 *   <li><b>两个时间分开</b>——发生时间是事情真的发生的那一刻，上报时间是客户端提交上来的那一刻；
 *       补录晚到不代表业务倒序（时间线按发生时间排）；</li>
 *   <li><b>异常是独立标记，不是状态</b>——异常只落事实，任务状态机由 {@link LogisticsTransportTaskService} 负责；</li>
 *   <li><b>集货时节点归到停靠点</b>——到达提货点 / 交接完成 / 起运必须带 {@code stopId}，
 *       进度按停靠点各自收敛，互不相串（V5）。</li>
 * </ol>
 */
@Service
@Validated
public class LogisticsTransportNodeServiceImpl implements LogisticsTransportNodeService {

    @Resource
    private LogisticsTransportNodeMapper logisticsTransportNodeMapper;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportStopService logisticsTransportStopService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportNode(LogisticsTransportNodeReportReqVO reportReqVO) {
        // 1. 幂等：同一客户端请求号只落一条（离线补传的核心保证）
        LogisticsTransportNodeDO existing =
                logisticsTransportNodeMapper.selectByClientRequestId(reportReqVO.getClientRequestId());
        if (existing != null) {
            return existing.getId();
        }
        // 2. 类型与时间：五类节点全部可上报；发生时间必填（它是时间线的排序依据，不能靠上报时间替代）
        if (reportReqVO.getNodeType() == null) {
            throw exception(TRANSPORT_NODE_TYPE_REQUIRED);
        }
        LogisticsTransportNodeTypeEnum nodeType = LogisticsTransportNodeTypeEnum.ofType(reportReqVO.getNodeType())
                .orElseThrow(() -> exception(TRANSPORT_NODE_TYPE_UNKNOWN));
        if (reportReqVO.getNodeTime() == null) {
            throw exception(TRANSPORT_NODE_TIME_REQUIRED);
        }
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTask(reportReqVO.getTaskId());
        assertTaskReportable(task);
        // 3. 照片必填策略：交接完成与卸货完成是货物流的关键凭证（ADR 0031 / 税总 5 号公告第十七条）
        if (nodeType.isPhotoRequired() && CollUtil.isEmpty(reportReqVO.getPhotos())) {
            throw exception(TRANSPORT_NODE_PHOTO_REQUIRED);
        }
        // 4. 停靠点：集货时提货相关节点必须归到某一个停靠点，整趟收尾的两类不许带停靠点
        LogisticsTransportStopDO stop = resolveStop(task, nodeType, reportReqVO.getStopId());

        // 5. 落节点
        LogisticsTransportNodeDO node = new LogisticsTransportNodeDO();
        node.setTaskId(task.getId());
        node.setTaskNo(task.getTaskNo());
        node.setStopId(stop == null ? null : stop.getId());
        node.setNodeType(nodeType.getType());
        node.setNodeTime(reportReqVO.getNodeTime());
        node.setReportTime(LocalDateTime.now());
        node.setLocation(reportReqVO.getLocation());
        node.setLatitude(reportReqVO.getLatitude());
        node.setLongitude(reportReqVO.getLongitude());
        node.setPhotos(toPhotosJson(reportReqVO.getPhotos()));
        node.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        node.setOperatorName(currentOperatorName());
        node.setClientRequestId(reportReqVO.getClientRequestId());
        node.setRemark(reportReqVO.getRemark());
        logisticsTransportNodeMapper.insert(node);

        // 6. 起运把任务推进到「执行中」（状态推进只经任务服务，状态机只有一处）
        if (LogisticsTransportNodeTypeEnum.DEPARTED == nodeType) {
            LogisticsTransportTaskDO update = new LogisticsTransportTaskDO();
            update.setId(task.getId());
            update.setStartTime(reportReqVO.getNodeTime());
            logisticsTransportTaskService.transitStatusAndFill(
                    task, LogisticsTransportTaskStatusEnum.IN_TRANSIT, update);
        }
        // 7. 推进该停靠点自己的进度（只动这一个点）
        if (stop != null) {
            logisticsTransportStopService.onNodeReported(stop, nodeType);
        }
        return node.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportAbnormal(LogisticsTransportAbnormalReportReqVO reportReqVO) {
        // 1. 幂等：与节点共用同一套客户端请求号
        LogisticsTransportNodeDO existing =
                logisticsTransportNodeMapper.selectByClientRequestId(reportReqVO.getClientRequestId());
        if (existing != null) {
            return existing.getId();
        }
        // 2. 校验：类型固定枚举、说明必填、发生时间必填
        LogisticsTransportAbnormalTypeEnum abnormalType =
                LogisticsTransportAbnormalTypeEnum.ofType(reportReqVO.getAbnormalType())
                        .orElseThrow(() -> exception(TRANSPORT_ABNORMAL_TYPE_UNKNOWN));
        if (StrUtil.isBlank(reportReqVO.getAbnormalReason())) {
            throw exception(TRANSPORT_ABNORMAL_REASON_REQUIRED);
        }
        if (reportReqVO.getNodeTime() == null) {
            throw exception(TRANSPORT_NODE_TIME_REQUIRED);
        }
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTask(reportReqVO.getTaskId());
        assertTaskReportable(task);
        // 异常可以挂在某一个停靠点上（对方不在），也可以是路上的事（不带停靠点）
        LogisticsTransportStopDO stop = reportReqVO.getStopId() == null ? null
                : requireStopOfTask(task, reportReqVO.getStopId());

        // 3. 落一条**没有节点类型**的事实：异常是独立标记，不是「走到哪一步」
        LogisticsTransportNodeDO node = new LogisticsTransportNodeDO();
        node.setTaskId(task.getId());
        node.setTaskNo(task.getTaskNo());
        node.setStopId(stop == null ? null : stop.getId());
        node.setNodeType(null);
        node.setNodeTime(reportReqVO.getNodeTime());
        node.setReportTime(LocalDateTime.now());
        node.setLocation(reportReqVO.getLocation());
        node.setLatitude(reportReqVO.getLatitude());
        node.setLongitude(reportReqVO.getLongitude());
        node.setPhotos(toPhotosJson(reportReqVO.getPhotos()));
        node.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        node.setOperatorName(currentOperatorName());
        node.setAbnormalType(abnormalType.getType());
        node.setAbnormalReason(reportReqVO.getAbnormalReason());
        node.setAbnormalResolved(false);
        node.setClientRequestId(reportReqVO.getClientRequestId());
        node.setRemark(reportReqVO.getRemark());
        logisticsTransportNodeMapper.insert(node);
        // 4. 刻意**不**调 transitStatus，也不改停靠点状态：异常是独立标记
        return node.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveAbnormal(LogisticsTransportAbnormalResolveReqVO resolveReqVO) {
        LogisticsTransportNodeDO node = logisticsTransportNodeMapper.selectById(resolveReqVO.getId());
        if (node == null || node.getAbnormalType() == null) {
            throw exception(TRANSPORT_ABNORMAL_NOT_EXISTS);
        }
        if (Boolean.TRUE.equals(node.getAbnormalResolved())) {
            // 不覆盖已有的解决留痕：谁在什么时候怎么解决的是一旦写下就不该变的事实
            throw exception(TRANSPORT_ABNORMAL_ALREADY_RESOLVED);
        }
        LogisticsTransportNodeDO update = new LogisticsTransportNodeDO();
        update.setId(node.getId());
        update.setAbnormalResolved(true);
        update.setAbnormalResolvedAt(LocalDateTime.now());
        update.setAbnormalResolvedBy(SecurityFrameworkUtils.getLoginUserId());
        update.setAbnormalResolvedName(currentOperatorName());
        update.setAbnormalResolvedRemark(resolveReqVO.getResolveRemark());
        logisticsTransportNodeMapper.updateById(update);
    }

    @Override
    public List<LogisticsTransportNodeDO> getNodeListByTaskId(Long taskId) {
        if (taskId == null) {
            return Collections.emptyList();
        }
        return logisticsTransportNodeMapper.selectListByTaskId(taskId);
    }

    @Override
    public List<LogisticsTransportNodeDO> getNodeListByTaskNo(String taskNo) {
        if (StrUtil.isBlank(taskNo)) {
            return Collections.emptyList();
        }
        return logisticsTransportNodeMapper.selectListByTaskNo(taskNo);
    }

    @Override
    public List<LogisticsTransportNodeRespVO> toRespList(List<LogisticsTransportNodeDO> nodes) {
        return TransportNodeConverter.toRespList(nodes);
    }

    /**
     * 停靠点规则（V5）：
     * <ul>
     *   <li>提货相关的三类节点（到达提货点 / 交接完成 / 起运）在**有停靠点的任务**上必须带 {@code stopId}；
     *       没有停靠点的历史任务允许为空（单点、按老口径）。</li>
     *   <li>整趟收尾的两类（到达场站 / 卸货完成）不许带 {@code stopId}。</li>
     *   <li>带到已取消的停靠点上报直接拒。</li>
     * </ul>
     */
    private LogisticsTransportStopDO resolveStop(LogisticsTransportTaskDO task,
                                                 LogisticsTransportNodeTypeEnum nodeType, Long stopId) {
        if (!nodeType.isStopScoped()) {
            if (stopId != null) {
                throw exception(TRANSPORT_STOP_NOT_ALLOWED_FOR_NODE);
            }
            return null;
        }
        if (stopId == null) {
            // 老任务（建在 V5 之前）没有停靠点，按单点口径放行；有停靠点的任务必须指明去的是哪一家
            boolean hasStops = !logisticsTransportStopService.getStopListByTaskId(task.getId()).isEmpty();
            if (hasStops) {
                throw exception(TRANSPORT_STOP_REQUIRED_FOR_NODE);
            }
            return null;
        }
        return requireStopOfTask(task, stopId);
    }

    private LogisticsTransportStopDO requireStopOfTask(LogisticsTransportTaskDO task, Long stopId) {
        LogisticsTransportStopDO stop = logisticsTransportStopService.getStop(stopId);
        if (!Objects.equals(stop.getTaskId(), task.getId())) {
            throw exception(TRANSPORT_STOP_NOT_BELONG_TO_TASK);
        }
        if (LogisticsTransportStopStatusEnum.CANCELLED.getStatus().equals(stop.getStatus())) {
            throw exception(TRANSPORT_STOP_CANCELLED_NOT_REPORTABLE);
        }
        return stop;
    }

    /**
     * 待分配意味着还没有车与人（谈不上运输过程）；已取消意味着这趟活已经了结。
     *
     * <p>**已完成仍可补录**：「补录晚到不导致业务倒序」——弱网下事后补报事实是常态，
     * 把证据拦在门外比多一条晚到的记录更糟。（「起运」在已完成的任务上仍会被状态机拦，那不是重开一趟活。）
     */
    private void assertTaskReportable(LogisticsTransportTaskDO task) {
        LogisticsTransportTaskStatusEnum status = LogisticsTransportTaskStatusEnum.ofStatus(task.getStatus())
                .orElseThrow(() -> exception(TRANSPORT_NODE_TASK_NOT_REPORTABLE));
        if (status == LogisticsTransportTaskStatusEnum.PENDING
                || status == LogisticsTransportTaskStatusEnum.CANCELLED) {
            throw exception(TRANSPORT_NODE_TASK_NOT_REPORTABLE);
        }
    }

    private String toPhotosJson(List<String> photos) {
        return CollUtil.isEmpty(photos) ? null : JsonUtils.toJsonString(photos);
    }

    /**
     * 上报人姓名：登录态里有昵称就用它，没有（例如系统任务）留空——不编造一个名字进凭证。
     */
    private String currentOperatorName() {
        return SecurityFrameworkUtils.getLoginUserNickname();
    }

}
