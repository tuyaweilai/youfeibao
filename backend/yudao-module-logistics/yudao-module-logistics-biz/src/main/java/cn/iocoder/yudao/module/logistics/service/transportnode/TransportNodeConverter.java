package cn.iocoder.yudao.module.logistics.service.transportnode;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportAbnormalTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运输节点 DO → Response VO 的转换（V5 #72 抽出）。
 *
 * <p>抽出来是因为两个读取面都要它：任务详情（整趟活的节点）与停靠点详情（某一个停靠点的节点）。
 * 两处各写一份就会漂移成「同一个节点在两页显示不一样」。
 */
public final class TransportNodeConverter {

    private TransportNodeConverter() {
    }

    public static List<LogisticsTransportNodeRespVO> toRespList(List<LogisticsTransportNodeDO> nodes) {
        if (CollUtil.isEmpty(nodes)) {
            return Collections.emptyList();
        }
        return nodes.stream().map(TransportNodeConverter::toResp).collect(Collectors.toList());
    }

    public static LogisticsTransportNodeRespVO toResp(LogisticsTransportNodeDO node) {
        LogisticsTransportNodeRespVO resp = new LogisticsTransportNodeRespVO();
        resp.setId(node.getId());
        resp.setTaskId(node.getTaskId());
        resp.setTaskNo(node.getTaskNo());
        resp.setStopId(node.getStopId());
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

    public static List<String> fromPhotosJson(String photosJson) {
        if (StrUtil.isBlank(photosJson)) {
            return Collections.emptyList();
        }
        List<String> photos = JsonUtils.parseArray(photosJson, String.class);
        return photos == null ? Collections.emptyList() : photos;
    }

}
