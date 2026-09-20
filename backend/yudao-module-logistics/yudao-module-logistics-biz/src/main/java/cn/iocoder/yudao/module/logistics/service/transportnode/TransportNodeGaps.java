package cn.iocoder.yudao.module.logistics.service.transportnode;

import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 「断点」计算（V4 #71）：一趟活还差哪几步、已有的一步缺哪张凭证。
 *
 * <p>放在这里而不是各自写在 Controller 里：PC 任务详情与司机端任务详情都要它，
 * 两处各写一遍就会出现「PC 说齐了、司机端说没齐」这种口径裂缝。
 *
 * <p>只做**展示**：断点是给人看的提示，不是门禁（门禁在 {@code LogisticsTransportNodeServiceImpl}
 * 的上报校验里）。
 */
public final class TransportNodeGaps {

    private TransportNodeGaps() {
    }

    /**
     * 还没上报的节点类型（按流程顺序）。异常事实没有节点类型，不参与判定。
     */
    public static List<String> missingNodeNames(List<LogisticsTransportNodeDO> nodes) {
        return missingNames(nodes, type -> true);
    }

    /**
     * **某一个停靠点**还没上报的节点类型（V5 #72）：只看按停靠点上报的三类
     *（到达提货点 / 交接完成 / 起运）。
     *
     * <p>集货时每个停靠点各自清算——不能拿 A 家的「到达提货点」给 B 家凑数。
     */
    public static List<String> missingStopNodeNames(List<LogisticsTransportNodeDO> stopNodes) {
        return missingNames(stopNodes, LogisticsTransportNodeTypeEnum::isStopScoped);
    }

    private static List<String> missingNames(List<LogisticsTransportNodeDO> nodes,
                                             Predicate<LogisticsTransportNodeTypeEnum> scope) {
        Set<Integer> reported = nodes.stream()
                .map(LogisticsTransportNodeDO::getNodeType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return Arrays.stream(LogisticsTransportNodeTypeEnum.values())
                .filter(scope)
                .filter(type -> !reported.contains(type.getType()))
                .map(LogisticsTransportNodeTypeEnum::getName)
                .collect(Collectors.toList());
    }

    /**
     * 已有节点但缺凭证的（「交接完成」与「卸货完成」必须有照片）。
     *
     * <p>上报校验已经拦住了新的缺照片上报，这里主要是给历史数据与跨端补录兜底。
     */
    public static List<String> missingEvidenceNames(List<LogisticsTransportNodeDO> nodes) {
        return nodes.stream()
                .filter(node -> LogisticsTransportNodeTypeEnum.ofType(node.getNodeType())
                        .map(LogisticsTransportNodeTypeEnum::isPhotoRequired).orElse(false))
                .filter(node -> Optional.ofNullable(node.getPhotos())
                        .map(photos -> photos.isBlank() || "[]".equals(photos.trim()))
                        .orElse(true))
                .map(node -> LogisticsTransportNodeTypeEnum.ofType(node.getNodeType())
                        .map(LogisticsTransportNodeTypeEnum::getName).orElse("该节点") + "缺照片")
                .collect(Collectors.toList());
    }

}
