package cn.iocoder.yudao.module.logistics.service.transportnode.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transportnode.LogisticsTransportNodeMapper;
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
 * 运输节点 Service 实现（V2b #78）。
 *
 * <p>两件事必须做对：**写入幂等**（弱网补传会重复提交同一条事实）与**两个时间分开**
 *（发生时间是事情真的发生的那一刻，上报时间是客户端提交上来的那一刻；补录晚到不代表业务倒序）。
 */
@Service
@Validated
public class LogisticsTransportNodeServiceImpl implements LogisticsTransportNodeService {

    /**
     * 本票开放上报的节点类型。其余四类归 V4（#71）——那时才有照片必填策略与异常标记，
     * 现在放开等于把「还没设计好的规则」也交给现场用。
     */
    private static final List<Integer> SUPPORTED_NODE_TYPES =
            Collections.singletonList(LogisticsTransportNodeTypeEnum.DEPARTED.getType());

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
        // 2. 类型与时间：本期只开放起运；发生时间必填（它是时间线的排序依据，不能靠上报时间替代）
        if (!SUPPORTED_NODE_TYPES.contains(reportReqVO.getNodeType())) {
            throw exception(TRANSPORT_NODE_TYPE_NOT_SUPPORTED_YET);
        }
        if (reportReqVO.getNodeTime() == null) {
            throw exception(TRANSPORT_NODE_TIME_REQUIRED);
        }
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTask(reportReqVO.getTaskId());

        // 3. 落节点
        LogisticsTransportNodeDO node = new LogisticsTransportNodeDO();
        node.setTaskId(task.getId());
        node.setTaskNo(task.getTaskNo());
        node.setNodeType(reportReqVO.getNodeType());
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

        // 4. 起运把任务推进到「执行中」（状态推进只经任务服务，状态机只有一处）
        if (LogisticsTransportNodeTypeEnum.DEPARTED.getType().equals(reportReqVO.getNodeType())) {
            LogisticsTransportTaskDO update = new LogisticsTransportTaskDO();
            update.setId(task.getId());
            update.setStartTime(reportReqVO.getNodeTime());
            logisticsTransportTaskService.transitStatusAndFill(
                    task, LogisticsTransportTaskStatusEnum.IN_TRANSIT, update);
        }
        return node.getId();
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
