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
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transportnode.LogisticsTransportNodeMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportAbnormalTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 运输节点 Service 实现（V2b #78；V4 #71 开全五类并加异常）。
 *
 * <p>三件事必须做对：
 * <ol>
 *   <li><b>写入幂等</b>——弱网补传会重复提交同一条事实；</li>
 *   <li><b>两个时间分开</b>——发生时间是事情真的发生的那一刻，上报时间是客户端提交上来的那一刻；
 *       补录晚到不代表业务倒序（时间线按发生时间排）；</li>
 *   <li><b>异常是独立标记，不是状态</b>——异常只落事实，任务状态机由 {@link LogisticsTransportTaskService} 负责。</li>
 * </ol>
 */
@Service
@Validated
public class LogisticsTransportNodeServiceImpl implements LogisticsTransportNodeService {

    @Resource
    private LogisticsTransportNodeMapper logisticsTransportNodeMapper;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;

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

        // 4. 落节点
        LogisticsTransportNodeDO node = new LogisticsTransportNodeDO();
        node.setTaskId(task.getId());
        node.setTaskNo(task.getTaskNo());
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

        // 5. 起运把任务推进到「执行中」（状态推进只经任务服务，状态机只有一处）
        if (LogisticsTransportNodeTypeEnum.DEPARTED == nodeType) {
            LogisticsTransportTaskDO update = new LogisticsTransportTaskDO();
            update.setId(task.getId());
            update.setStartTime(reportReqVO.getNodeTime());
            logisticsTransportTaskService.transitStatusAndFill(
                    task, LogisticsTransportTaskStatusEnum.IN_TRANSIT, update);
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

        // 3. 落一条**没有节点类型**的事实：异常是独立标记，不是「走到哪一步」
        LogisticsTransportNodeDO node = new LogisticsTransportNodeDO();
        node.setTaskId(task.getId());
        node.setTaskNo(task.getTaskNo());
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
        // 4. 刻意**不**调 transitStatus：异常不改变任务状态机
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
        if (CollUtil.isEmpty(nodes)) {
            return Collections.emptyList();
        }
        return nodes.stream().map(this::toResp).collect(Collectors.toList());
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

    private LogisticsTransportNodeRespVO toResp(LogisticsTransportNodeDO node) {
        LogisticsTransportNodeRespVO resp = new LogisticsTransportNodeRespVO();
        resp.setId(node.getId());
        resp.setTaskId(node.getTaskId());
        resp.setTaskNo(node.getTaskNo());
        resp.setNodeType(node.getNodeType());
        resp.setNodeTypeName(LogisticsTransportNodeTypeEnum.ofType(node.getNodeType())
                .map(LogisticsTransportNodeTypeEnum::getName).orElse(null));
        resp.setNodeTime(node.getNodeTime());
        resp.setReportTime(node.getReportTime());
        resp.setLocation(node.getLocation());
        resp.setLatitude(node.getLatitude());
        resp.setLongitude(node.getLongitude());
        resp.setPhotos(fromPhotosJson(node.getPhotos()));
        resp.setOperatorId(node.getOperatorId());
        resp.setOperatorName(node.getOperatorName());
        resp.setAbnormalType(node.getAbnormalType());
        resp.setAbnormalTypeName(LogisticsTransportAbnormalTypeEnum.nameOf(node.getAbnormalType()));
        resp.setAbnormalReason(node.getAbnormalReason());
        resp.setAbnormalResolved(node.getAbnormalResolved());
        resp.setAbnormalResolvedAt(node.getAbnormalResolvedAt());
        resp.setAbnormalResolvedName(node.getAbnormalResolvedName());
        resp.setAbnormalResolvedRemark(node.getAbnormalResolvedRemark());
        resp.setRemark(node.getRemark());
        resp.setCreateTime(node.getCreateTime());
        return resp;
    }

    private String toPhotosJson(List<String> photos) {
        return CollUtil.isEmpty(photos) ? null : JsonUtils.toJsonString(photos);
    }

    private List<String> fromPhotosJson(String photosJson) {
        if (StrUtil.isBlank(photosJson)) {
            return Collections.emptyList();
        }
        List<String> photos = JsonUtils.parseArray(photosJson, String.class);
        return photos == null ? Collections.emptyList() : photos;
    }

    /**
     * 上报人姓名：登录态里有昵称就用它，没有（例如系统任务）留空——不编造一个名字进凭证。
     */
    private String currentOperatorName() {
        return SecurityFrameworkUtils.getLoginUserNickname();
    }

}
