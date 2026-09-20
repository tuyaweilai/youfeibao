package cn.iocoder.yudao.module.logistics.api.transport;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportHandoverRespDTO;
import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportNodeRespDTO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporthandover.LogisticsTransportHandoverDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transportnode.LogisticsTransportNodeMapper;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporthandover.LogisticsTransportHandoverMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsHandoverDocumentStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportAbnormalTypeEnum;
import cn.iocoder.yudao.module.logistics.service.transportnode.TransportNodeConverter;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link LogisticsTransportApi} 的实现（V2b #78 起为真实查询；V6 #73 补上交接登记两个轴）。
 *
 * <p>契约（与 V1 定的一样，实现换掉但契约不动）：查不到、入参为空都返回**空列表**，
 * 不返回 null、不抛异常；节点按发生时间正序。缺凭证是业务状态而不是错误——自送的货本来就可能
 * 没有运输节点，ADR 0031 也不因缺轨迹而拒收。
 */
@Service
public class LogisticsTransportApiImpl implements LogisticsTransportApi {

    /**
     * 「最近登记的交接登记」最多返回多少条。
     *
     * <p>它是一份待处理队列（磅房按它回场复磅），不是查询接口——需要检索请走物流模块自己的页面。
     */
    private static final int RECENT_HANDOVER_LIMIT = 100;

    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportNodeMapper logisticsTransportNodeMapper;
    @Resource
    private LogisticsTransportHandoverMapper logisticsTransportHandoverMapper;

    @Override
    public List<LogisticsTransportNodeRespDTO> getNodeListByTaskNo(String taskNo) {
        if (StrUtil.isBlank(taskNo)) {
            return Collections.emptyList();
        }
        List<LogisticsTransportNodeDO> nodes = logisticsTransportNodeMapper.selectListByTaskNo(taskNo);
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTaskByTaskNo(taskNo);
        return nodes.stream().map(node -> toDTO(node, task)).collect(Collectors.toList());
    }

    @Override
    public List<LogisticsTransportNodeRespDTO> getEvidenceListByHandoverId(Long handoverId) {
        LogisticsTransportHandoverDO handover = handoverId == null
                ? null : logisticsTransportHandoverMapper.selectById(handoverId);
        if (handover == null || handover.getTaskId() == null) {
            return Collections.emptyList();
        }
        // 「这批货」= 这一次交接所在停靠点的提货节点 + 整趟活的收尾节点（到达场站 / 卸货完成）：
        // 卸货完成同样是这批货的运输凭证，不属于任何单个停靠点，所以要一起带上
        List<LogisticsTransportNodeDO> nodes = logisticsTransportNodeMapper.selectListByTaskId(handover.getTaskId())
                .stream()
                .filter(node -> node.getStopId() == null || Objects.equals(node.getStopId(), handover.getStopId()))
                .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTask(handover.getTaskId());
        return nodes.stream().map(node -> toDTO(node, task)).collect(Collectors.toList());
    }

    @Override
    public LogisticsTransportHandoverRespDTO getHandover(Long handoverId) {
        LogisticsTransportHandoverDO handover = handoverId == null
                ? null : logisticsTransportHandoverMapper.selectById(handoverId);
        return handover == null ? null : toHandoverDTO(handover);
    }

    @Override
    public List<LogisticsTransportHandoverRespDTO> getRecentHandoverList() {
        return logisticsTransportHandoverMapper.selectRecentList(RECENT_HANDOVER_LIMIT).stream()
                .map(this::toHandoverDTO)
                .collect(Collectors.toList());
    }

    private LogisticsTransportHandoverRespDTO toHandoverDTO(LogisticsTransportHandoverDO handover) {
        LogisticsTransportHandoverRespDTO dto =
                BeanUtils.toBean(handover, LogisticsTransportHandoverRespDTO.class);
        dto.setPhotos(TransportNodeConverter.fromPhotosJson(handover.getPhotos()));
        dto.setDocumentStatusName(LogisticsHandoverDocumentStatusEnum.nameOf(handover.getDocumentStatus()));
        return dto;
    }

    private LogisticsTransportNodeRespDTO toDTO(LogisticsTransportNodeDO node, LogisticsTransportTaskDO task) {
        LogisticsTransportNodeRespDTO dto = new LogisticsTransportNodeRespDTO();
        dto.setId(node.getId());
        dto.setTransportTaskId(node.getTaskId());
        dto.setTaskNo(node.getTaskNo());
        dto.setStopId(node.getStopId());
        dto.setNodeType(node.getNodeType());
        dto.setAbnormalType(node.getAbnormalType());
        dto.setAbnormalTypeName(LogisticsTransportAbnormalTypeEnum.nameOf(node.getAbnormalType()));
        dto.setNodeTime(node.getNodeTime());
        dto.setReportTime(node.getReportTime());
        dto.setLocation(node.getLocation());
        dto.setLatitude(node.getLatitude());
        dto.setLongitude(node.getLongitude());
        dto.setPhotos(TransportNodeConverter.fromPhotosJson(node.getPhotos()));
        dto.setOperatorName(node.getOperatorName());
        if (task != null) {
            // 车牌与司机取任务上的快照：档案改名或删档都不影响凭证
            dto.setPlateNo(task.getPlateNo());
            dto.setDriverName(task.getDriverName());
        }
        return dto;
    }

}
