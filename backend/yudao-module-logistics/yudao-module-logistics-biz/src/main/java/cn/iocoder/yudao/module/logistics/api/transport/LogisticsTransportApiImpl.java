package cn.iocoder.yudao.module.logistics.api.transport;

import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportNodeRespDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * {@link LogisticsTransportApi} 的实现。
 *
 * <p><b>骨架实现（#68 V1）</b>：本票只定死读取面的签名与契约，运输任务与节点的存储要等
 * #69（V2 一趟活跑通）建表、#73（V6 交接登记）接上交接口，因此这里暂时查询不出来东西。
 * 空实现是**有意的**，不是遗漏：契约要求的「查不到返回空列表」正好就是它的行为。
 *
 * <p>实现真实查询时，本类只换内部实现，签名与契约（空列表而非 null、按发生时间正序）不变。
 *
 * @author 芋道源码
 */
@Service
public class LogisticsTransportApiImpl implements LogisticsTransportApi {

    @Override
    public List<LogisticsTransportNodeRespDTO> getNodeListByTaskNo(String taskNo) {
        // TODO #69（V2 一趟活跑通）：运输任务与节点落表后改为按 taskNo 查询
        return Collections.emptyList();
    }

    @Override
    public List<LogisticsTransportNodeRespDTO> getEvidenceListByHandoverBatchId(Long handoverBatchId) {
        // TODO #73（V6 交接登记 → 回场复磅 → 收购单）：交接批次与节点挂接后改为按 handoverBatchId 查询
        return Collections.emptyList();
    }

}
