package cn.iocoder.yudao.module.logistics.service.transportstop.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopCancelReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportStopDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transportnode.LogisticsTransportNodeMapper;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportStopMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportStopStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportStopTypeEnum;
import cn.iocoder.yudao.module.logistics.service.transportnode.TransportNodeConverter;
import cn.iocoder.yudao.module.logistics.service.transportnode.TransportNodeGaps;
import cn.iocoder.yudao.module.logistics.service.transportstop.LogisticsTransportStopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 运输停靠点 Service 实现（V5 #72）。
 *
 * <p>**每个停靠点独立推进**：进度、断点、节点都按 {@code stopId} 归位，互不相串；
 * 取消一个点只动这一个。这里不碰 icbc 的类（ADR 0032），出售者只存编号与姓名 / 手机号快照。
 */
@Service
@Validated
public class LogisticsTransportStopServiceImpl implements LogisticsTransportStopService {

    @Resource
    private LogisticsTransportStopMapper logisticsTransportStopMapper;
    @Resource
    private LogisticsTransportNodeMapper logisticsTransportNodeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<LogisticsTransportStopDO> createStops(Long taskId, String taskNo,
                                                      List<LogisticsTransportStopSaveReqVO> stops) {
        if (CollUtil.isEmpty(stops)) {
            return Collections.emptyList();
        }
        int stopNo = 0;
        for (LogisticsTransportStopSaveReqVO reqVO : stops) {
            stopNo++;
            insertStop(taskId, taskNo, stopNo, reqVO);
        }
        return getStopListByTaskId(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addStop(Long taskId, String taskNo, LogisticsTransportStopSaveReqVO reqVO) {
        // 停靠顺序接在最后：已取消的点也算占位，避免与既有停靠顺序撞号
        int nextStopNo = getStopListByTaskId(taskId).stream()
                .map(LogisticsTransportStopDO::getStopNo)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        return insertStop(taskId, taskNo, nextStopNo, reqVO).getId();
    }

    private LogisticsTransportStopDO insertStop(Long taskId, String taskNo, int stopNo,
                                                LogisticsTransportStopSaveReqVO reqVO) {
        LogisticsTransportStopDO stop = BeanUtils.toBean(reqVO, LogisticsTransportStopDO.class);
        stop.setId(null);
        stop.setTaskId(taskId);
        stop.setTaskNo(taskNo);
        stop.setStopNo(stopNo);
        stop.setStopType(reqVO.getStopType() != null ? reqVO.getStopType()
                : LogisticsTransportStopTypeEnum.PICKUP.getType());
        stop.setStatus(LogisticsTransportStopStatusEnum.PENDING.getStatus());
        logisticsTransportStopMapper.insert(stop);
        return stop;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelStop(LogisticsTransportStopCancelReqVO cancelReqVO) {
        if (StrUtil.isBlank(cancelReqVO.getCancelReason())) {
            throw exception(TRANSPORT_STOP_CANCEL_REASON_REQUIRED);
        }
        LogisticsTransportStopDO stop = getStop(cancelReqVO.getId());
        LogisticsTransportStopStatusEnum status = LogisticsTransportStopStatusEnum.ofStatus(stop.getStatus())
                .orElseThrow(() -> exception(TRANSPORT_STOP_STATUS_NOT_ALLOW_CANCEL));
        // 已完成（货已装车）与已取消的不能再取消：这是对既成事实的回退，不是「这个点不去了」
        if (status.isTerminal()) {
            throw exception(TRANSPORT_STOP_STATUS_NOT_ALLOW_CANCEL);
        }
        LogisticsTransportStopDO update = new LogisticsTransportStopDO();
        update.setId(stop.getId());
        update.setStatus(LogisticsTransportStopStatusEnum.CANCELLED.getStatus());
        update.setCancelReason(cancelReqVO.getCancelReason());
        update.setCancelTime(LocalDateTime.now());
        logisticsTransportStopMapper.updateById(update);
    }

    @Override
    public LogisticsTransportStopDO getStop(Long id) {
        LogisticsTransportStopDO stop = logisticsTransportStopMapper.selectById(id);
        if (stop == null) {
            throw exception(TRANSPORT_STOP_NOT_EXISTS);
        }
        return stop;
    }

    @Override
    public List<LogisticsTransportStopDO> getStopListByTaskId(Long taskId) {
        if (taskId == null) {
            return Collections.emptyList();
        }
        return logisticsTransportStopMapper.selectListByTaskId(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onNodeReported(LogisticsTransportStopDO stop, LogisticsTransportNodeTypeEnum nodeType) {
        if (stop == null) {
            return;
        }
        LogisticsTransportStopStatusEnum status = LogisticsTransportStopStatusEnum.ofStatus(stop.getStatus())
                .orElse(LogisticsTransportStopStatusEnum.PENDING);
        // 「交接完成」= 这个点的货装上车了，即完成；其余停靠点节点只表示「开始处理」
        LogisticsTransportStopStatusEnum target = nodeType == LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED
                ? LogisticsTransportStopStatusEnum.COMPLETED
                : (status == LogisticsTransportStopStatusEnum.PENDING
                        ? LogisticsTransportStopStatusEnum.IN_PROGRESS : status);
        if (target == status) {
            return;
        }
        LogisticsTransportStopDO update = new LogisticsTransportStopDO();
        update.setId(stop.getId());
        update.setStatus(target.getStatus());
        logisticsTransportStopMapper.updateById(update);
        stop.setStatus(target.getStatus());
    }

    @Override
    public List<LogisticsTransportStopRespVO> getStopRespListByTaskId(Long taskId) {
        List<LogisticsTransportStopDO> stops = getStopListByTaskId(taskId);
        if (CollUtil.isEmpty(stops)) {
            return Collections.emptyList();
        }
        // 每个停靠点的节点只看自己那些：按 stopId 分组，进度互不相串
        Map<Long, List<LogisticsTransportNodeDO>> nodesByStopId = logisticsTransportNodeMapper
                .selectListByTaskId(taskId).stream()
                .filter(node -> node.getStopId() != null)
                .collect(Collectors.groupingBy(LogisticsTransportNodeDO::getStopId));
        return stops.stream().map(stop -> toResp(stop, nodesByStopId.getOrDefault(stop.getId(),
                Collections.emptyList()))).collect(Collectors.toList());
    }

    private LogisticsTransportStopRespVO toResp(LogisticsTransportStopDO stop,
                                                List<LogisticsTransportNodeDO> nodes) {
        LogisticsTransportStopRespVO resp = BeanUtils.toBean(stop, LogisticsTransportStopRespVO.class);
        resp.setStopTypeName(LogisticsTransportStopTypeEnum.nameOf(stop.getStopType()));
        resp.setStatusName(LogisticsTransportStopStatusEnum.nameOf(stop.getStatus()));
        resp.setNodes(TransportNodeConverter.toRespList(nodes));
        resp.setMissingNodeNames(TransportNodeGaps.missingStopNodeNames(nodes));
        resp.setMissingEvidenceNames(TransportNodeGaps.missingEvidenceNames(nodes));
        return resp;
    }

    @Override
    public Map<Long, Integer> getPendingStopCounts(Collection<Long> taskIds) {
        if (CollUtil.isEmpty(taskIds)) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> counts = new HashMap<>();
        for (LogisticsTransportStopDO stop : logisticsTransportStopMapper.selectListByTaskIds(taskIds)) {
            LogisticsTransportStopStatusEnum status = LogisticsTransportStopStatusEnum.ofStatus(stop.getStatus())
                    .orElse(LogisticsTransportStopStatusEnum.PENDING);
            // 「还剩几家没提」：待处理与进行中都算还没提完；已完成 / 已取消不算
            if (!status.isTerminal()) {
                counts.merge(stop.getTaskId(), 1, Integer::sum);
            }
        }
        return counts;
    }

}
