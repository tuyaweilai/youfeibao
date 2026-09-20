package cn.iocoder.yudao.module.logistics.service.transportnode;

import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TransportNodeGaps} 的单元测试（V4 #71）。
 *
 * <p>断点是给人看的提示：任务详情要能说清「还差哪几步」与「哪一步有记录但缺凭证」。
 */
public class TransportNodeGapsTest {

    @Test
    public void testMissingNodeNames_ignoresAbnormalEvents() {
        List<LogisticsTransportNodeDO> nodes = Arrays.asList(
                node(LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, null),
                node(LogisticsTransportNodeTypeEnum.DEPARTED, null),
                // 异常事实没有节点类型：它不该被当成「补上了一步」，也不该让断点判定崩掉
                node(null, "abnormal"));

        assertEquals(Arrays.asList("交接完成", "到达场站", "卸货完成"),
                TransportNodeGaps.missingNodeNames(nodes));
    }

    @Test
    public void testMissingNodeNames_allReported_isEmpty() {
        List<LogisticsTransportNodeDO> nodes = Arrays.asList(
                node(LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, null),
                node(LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED, "[\"p1\"]"),
                node(LogisticsTransportNodeTypeEnum.DEPARTED, null),
                node(LogisticsTransportNodeTypeEnum.ARRIVED_STATION, null),
                node(LogisticsTransportNodeTypeEnum.UNLOADED, "[\"p2\"]"));

        assertTrue(TransportNodeGaps.missingNodeNames(nodes).isEmpty());
        assertTrue(TransportNodeGaps.missingEvidenceNames(nodes).isEmpty());
    }

    @Test
    public void testMissingEvidenceNames_photoRequiredTypesWithoutPhotos() {
        List<LogisticsTransportNodeDO> nodes = Arrays.asList(
                node(LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED, null),
                node(LogisticsTransportNodeTypeEnum.UNLOADED, "[]"),
                // 这三类不要求照片，不该被报成缺凭证
                node(LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP, null),
                node(LogisticsTransportNodeTypeEnum.DEPARTED, null),
                node(LogisticsTransportNodeTypeEnum.ARRIVED_STATION, null));

        assertEquals(Arrays.asList("交接完成缺照片", "卸货完成缺照片"),
                TransportNodeGaps.missingEvidenceNames(nodes));
    }

    @Test
    public void testMissingNodeNames_emptyInput_listsAllFive() {
        assertEquals(5, TransportNodeGaps.missingNodeNames(Collections.emptyList()).size());
        assertTrue(TransportNodeGaps.missingEvidenceNames(Collections.emptyList()).isEmpty());
    }

    private static LogisticsTransportNodeDO node(LogisticsTransportNodeTypeEnum type, String photos) {
        return LogisticsTransportNodeDO.builder()
                .nodeType(type == null ? null : type.getType())
                .photos(photos)
                .build();
    }

}
