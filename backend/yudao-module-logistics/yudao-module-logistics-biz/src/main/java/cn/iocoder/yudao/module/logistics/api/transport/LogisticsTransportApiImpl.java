package cn.iocoder.yudao.module.logistics.api.transport;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportNodeRespDTO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transportnode.LogisticsTransportNodeMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportAbnormalTypeEnum;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link LogisticsTransportApi} 的实现（V2b #78 起为真实查询）。
 *
 * <p>契约（与 V1 定的一样，实现换掉但契约不动）：查不到、入参为空都返回**空列表**，
 * 不返回 null、不抛异常；按发生时间正序。缺凭证是业务状态而不是错误——自送的货本来就可能
 * 没有运输节点，ADR 0031 也不因缺轨迹而拒收。
 *
 * <p>按交接批次取凭证那个轴要等交接批次与运输任务挂上（V6 #73），本票先返回空列表。
 */
@Service
public class LogisticsTransportApiImpl implements LogisticsTransportApi {

    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportNodeMapper logisticsTransportNodeMapper;

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
    public List<LogisticsTransportNodeRespDTO> getEvidenceListByHandoverBatchId(Long handoverBatchId) {
        // TODO #73（V6 交接登记 → 回场复磅 → 收购单）：交接批次与运输任务挂接后改为按 handoverBatchId 查询
        return Collections.emptyList();
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
        dto.setPhotos(parsePhotos(node.getPhotos()));
        dto.setOperatorName(node.getOperatorName());
        if (task != null) {
            // 车牌与司机取任务上的快照：档案改名或删档都不影响凭证
            dto.setPlateNo(task.getPlateNo());
            dto.setDriverName(task.getDriverName());
        }
        return dto;
    }

    private List<String> parsePhotos(String photosJson) {
        if (StrUtil.isBlank(photosJson)) {
            return Collections.emptyList();
        }
        List<String> photos = JsonUtils.parseArray(photosJson, String.class);
        return photos == null ? Collections.emptyList() : photos;
    }

}
