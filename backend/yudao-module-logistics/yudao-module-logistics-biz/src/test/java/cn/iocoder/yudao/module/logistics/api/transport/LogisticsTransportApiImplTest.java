package cn.iocoder.yudao.module.logistics.api.transport;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportNodeRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LogisticsTransportApiImpl} 的单元测试，同时也是物流模块**测试底座**的冒烟测试。
 *
 * <p>锁死的是读取面的契约，而不是实现：查不到返回**空列表**（不是 null、不抛异常）、按发生时间正序。
 * icbc 的追溯页会直接消费这个契约——缺凭证是业务状态（自送的货本来就可能没有运输节点），不是错误。
 *
 * <p>本票只有骨架实现，所以断言的都是「空」这一侧；等 #69 / #73 落表之后同一组断言仍然要成立。
 */
@Import({LogisticsTransportApiImpl.class, UnitTestConfiguration.class})
public class LogisticsTransportApiImplTest extends BaseDbUnitTest {

    @Resource
    private LogisticsTransportApi logisticsTransportApi;

    @Test
    public void testGetNodeListByTaskNo_unknown_returnsEmptyListNotNull() {
        List<LogisticsTransportNodeRespDTO> nodes = logisticsTransportApi.getNodeListByTaskNo("NOT-EXISTS-TASK-NO");

        assertNotNull(nodes, "契约要求返回空列表而不是 null");
        assertTrue(nodes.isEmpty());
    }

    @Test
    public void testGetEvidenceListByHandoverBatchId_unknown_returnsEmptyListNotNull() {
        List<LogisticsTransportNodeRespDTO> evidences = logisticsTransportApi.getEvidenceListByHandoverBatchId(-1L);

        assertNotNull(evidences, "契约要求返回空列表而不是 null");
        assertTrue(evidences.isEmpty());
    }

    @Test
    public void testGetNodeListByTaskNo_nullTaskNo_doesNotThrow() {
        // 自送或没有派车的场景，调用方可能什么都没有；契约是「没有就是空」，不是报错
        List<LogisticsTransportNodeRespDTO> nodes = logisticsTransportApi.getNodeListByTaskNo(null);

        assertNotNull(nodes);
        assertTrue(nodes.isEmpty());
    }

}
