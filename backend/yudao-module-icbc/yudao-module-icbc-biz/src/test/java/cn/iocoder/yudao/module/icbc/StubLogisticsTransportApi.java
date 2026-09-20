package cn.iocoder.yudao.module.icbc;

import cn.iocoder.yudao.module.logistics.api.transport.LogisticsTransportApi;
import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportHandoverRespDTO;
import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportNodeRespDTO;

import java.util.Collections;
import java.util.List;

/**
 * icbc 单元测试用的物流读取面空实现（V6 #73）。
 *
 * <p>icbc 的测试上下文只 `@ComponentScan` 本模块，跨模块的 API 在上下文里没有 bean。icbc 侧的
 * `HandoverBatchServiceImpl` 与 `TraceQueryServiceImpl` 都注入了 {@link LogisticsTransportApi}，
 * 所以测试上下文必须提供一个——否则**每个** icbc 测试类都会在上下文启动时报
 * `NoSuchBeanDefinitionException`。
 *
 * <p>这里给的是**空实现**而不是 Mockito mock：它同时表达了真实的业务语义——「自送的货没有现场交接、
 * 没有运输节点」是常态。需要断言的用例再各自 `@MockBean` 覆盖它。
 *
 * <p>与 #59 Testing Decisions 一致：跨模块 API 在单测里不跨模块调用。
 */
public class StubLogisticsTransportApi implements LogisticsTransportApi {

    @Override
    public List<LogisticsTransportNodeRespDTO> getNodeListByTaskNo(String taskNo) {
        return Collections.emptyList();
    }

    @Override
    public List<LogisticsTransportNodeRespDTO> getEvidenceListByHandoverId(Long handoverId) {
        return Collections.emptyList();
    }

    @Override
    public LogisticsTransportHandoverRespDTO getHandover(Long handoverId) {
        return null;
    }

    @Override
    public List<LogisticsTransportHandoverRespDTO> getRecentHandoverList() {
        return Collections.emptyList();
    }

}
