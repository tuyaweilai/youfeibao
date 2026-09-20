package cn.iocoder.yudao.module.logistics.service.demo;

import cn.iocoder.yudao.module.logistics.controller.admin.demo.vo.LogisticsTransportTrackDemoRespVO;

/**
 * 运输轨迹**演示件** Service（V9 #76）。
 *
 * <p><b>它不是轨迹采集能力</b>：一期没有车载 GPS、也没有连续定位（CONTEXT.md「轨迹」：
 * 只有上报时的一次性位置快照），所以这里产出的是一条**模拟**折线，用途是演示与讲解。
 *
 * <p>三条硬约束（都有测试钉住）：
 * <ol>
 *   <li><b>不落库</b>：不写 {@code logistics_transport_node}，也不写任何别的表——演示数据不进证据链；</li>
 *   <li><b>不出现在读取面</b>：icbc 的一票一档走 {@code LogisticsTransportApi}，它只读真实节点，
 *       本服务产出的东西永远到不了那里；</li>
 *   <li><b>可一键关闭</b>：{@code logistics.demo.transport-track.enabled=false} 时返回 enabled=false
 *       且不带任何点，页面回到「只有真实节点与凭证」的状态。</li>
 * </ol>
 *
 * <p>模拟并非完全凭空：**锚点取真实上报过的节点经纬度**（有就用），中间的路按任务编号做确定性插值——
 * 同一个任务每次看到的是同一条线，演示时不会「每刷新一次就变一条路」。
 */
public interface LogisticsTrackDemoService {

    /**
     * 取某趟任务的演示轨迹。
     *
     * @param taskId 运输任务编号（任务不存在会抛业务异常，与其它接口一致）
     * @return 演示轨迹；未开启时 {@code enabled=false} 且点为空
     */
    LogisticsTransportTrackDemoRespVO getTransportTrackDemo(Long taskId);

}
