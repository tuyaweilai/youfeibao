package cn.iocoder.yudao.module.logistics.service.transportnode;

import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportAbnormalReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportAbnormalResolveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 运输节点 Service（V2b #78，V4 #71 开全五类并加异常）。
 *
 * <p>节点是**事实 + 凭证**：发生时间与上报时间分开记（弱网补录时两者会差很远），
 * 照片与位置是货物流证据的一部分。
 *
 * <p>V4 起五类节点全部可上报（到达提货点 / 交接完成 / 起运 / 到达场站 / 卸货完成），
 * 其中**交接完成与卸货完成必须有照片**。异常（车辆故障、道路封闭等）是**独立标记**，
 * 走 {@link #reportAbnormal} 落成一条没有节点类型的事实，**不改变任务状态机**。
 */
public interface LogisticsTransportNodeService {

    /**
     * 上报节点。幂等：同一 {@code clientRequestId} 重复提交返回既有节点编号，不落第二条。
     *
     * <p>「交接完成」与「卸货完成」必须有照片；「起运」会把任务推进到「执行中」。
     *
     * @param reportReqVO 上报请求
     * @return 节点编号（重复提交时返回既有节点的编号）
     */
    Long reportNode(@Valid LogisticsTransportNodeReportReqVO reportReqVO);

    /**
     * 上报运输异常。幂等：同一 {@code clientRequestId} 重复提交返回既有记录编号。
     *
     * <p>异常是**独立标记**：它只落一条事实（异常类型 + 说明 + 照片 + 位置 + 两个时间），
     * **不推进也不回退任务状态机**（#59 的 Implementation Decisions 第 17 条）。
     *
     * @param reportReqVO 异常上报请求
     * @return 异常事实编号（运输节点编号）
     */
    Long reportAbnormal(@Valid LogisticsTransportAbnormalReportReqVO reportReqVO);

    /**
     * 解决异常：记录谁、什么时候、怎么解决的。已经解决过的不能再解决（不覆盖留痕）。
     */
    void resolveAbnormal(@Valid LogisticsTransportAbnormalResolveReqVO resolveReqVO);

    /**
     * 按运输任务编号取节点（含异常事实），按发生时间正序。
     */
    List<LogisticsTransportNodeDO> getNodeListByTaskId(Long taskId);

    /**
     * 按运输任务单号取节点（含异常事实），按发生时间正序。没有任务或没有节点都返回空列表。
     */
    List<LogisticsTransportNodeDO> getNodeListByTaskNo(String taskNo);

    /**
     * 转成对外的响应 VO（补节点类型名、异常类型名与照片列表）。
     */
    List<LogisticsTransportNodeRespVO> toRespList(List<LogisticsTransportNodeDO> nodes);

}
