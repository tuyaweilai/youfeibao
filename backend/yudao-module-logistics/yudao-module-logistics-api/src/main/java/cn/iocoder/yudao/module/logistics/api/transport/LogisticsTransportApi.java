package cn.iocoder.yudao.module.logistics.api.transport;

import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportHandoverRespDTO;
import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportNodeRespDTO;

import java.util.List;

/**
 * 物流运输读取面：其它模块从这里读运输事实，**不直接读写物流的表**。
 *
 * <p>只有读，没有写：运输任务与节点的写入面留在 `yudao-module-logistics-biz`，
 * 派车与上报都发生在物流模块内部。
 *
 * <p>三个查询轴对应三件不同的事：
 * <ul>
 *   <li>按**任务编号**取节点——回答「这台车这一趟走到哪了」；</li>
 *   <li>按**交接登记编号**取凭证——回答「这批货是怎么运来的」，即税总 5 号公告第十七条要求的
 *       「运输发票或凭证」那一条货物流证据，供 icbc 的一票一档使用；</li>
 *   <li>取**待回场复磅**的交接登记——磅房按现场交接登记建交接批次（V6 #73），
 *       回场复磅才定稿（ADR 0031）。</li>
 * </ul>
 *
 * <p>契约：编号查不到时返回**空列表**（不返回 null、不抛异常）；入参为 null（例如自送的货没有任务编号）时同样返回空列表。
 * 缺凭证是业务状态，不是错误——自送的货本来就可能没有运输节点，按 ADR 0031 也不因缺轨迹而拒收。
 *
 * <p>依赖方向恒为「icbc → 物流」：物流不知道 icbc 的存在，`handoverBatchId` / `stationId` 等
 * icbc 侧编号在物流这里只是数字。
 *
 * @author 芋道源码
 */
public interface LogisticsTransportApi {

    /**
     * 按运输任务编号取该任务的运输节点，按发生时间正序。
     *
     * @param taskNo 运输任务编号（对外可见的单号）
     * @return 运输节点列表；查不到返回空列表
     */
    List<LogisticsTransportNodeRespDTO> getNodeListByTaskNo(String taskNo);

    /**
     * 按交接登记编号取该批货的运输凭证（节点及其照片、位置与两个时间），按发生时间正序。
     *
     * <p>「这批货」= 这一次交接登记所在停靠点的提货节点 + 整趟活的收尾节点（到达场站 / 卸货完成），
     * 因为卸货完成同样是这批货的运输凭证。
     *
     * @param handoverId 交接登记编号（V6 #73 起，icbc 侧通过交接批次的 logisticsHandoverId 反查）
     * @return 运输凭证列表；查不到返回空列表
     */
    List<LogisticsTransportNodeRespDTO> getEvidenceListByHandoverId(Long handoverId);

    /**
     * 取该交接登记（现场交接事实）；查不到返回 null。
     *
     * <p>icbc 侧回场复磅时用它把现场参考量、参考单价、要件状态、司机与车辆搬进交接批次。
     *
     * @param handoverId 交接登记编号
     * @return 交接登记；查不到或入参为 null 返回 null
     */
    LogisticsTransportHandoverRespDTO getHandover(Long handoverId);

    /**
     * 取**最近登记**的交接登记（供磅房按它回场复磅建批次），最新登记的在前面，**最多 100 条**。
     *
     * <p>物流**不知道哪些已经复过磅**：「已挂批次」是 icbc 侧的状态（挂接点在
     * `icbc_handover_batch.logistics_handover_id`，ADR 0032 规定物流不写 icbc 的状态），
     * 所以这一份是「最近登记的候选」，由 icbc 侧把已经建过批次的过滤掉。
     *
     * @return 最近登记的交接登记；没有返回空列表
     */
    List<LogisticsTransportHandoverRespDTO> getRecentHandoverList();

}
