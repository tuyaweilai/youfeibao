package cn.iocoder.yudao.module.logistics.service.transportnode;

import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 运输节点 Service（V2b #78）。
 *
 * <p>节点是**事实 + 凭证**：发生时间与上报时间分开记（弱网补录时两者会差很远），
 * 照片与位置是货物流证据的一部分。
 *
 * <p>本票只开放**起运**一类节点（其余四类与异常归 V4 #71）：现场动作在 V2b 由调度在 PC 上代录，
 * 司机端上报归 V2c #79。
 */
public interface LogisticsTransportNodeService {

    /**
     * 上报节点。幂等：同一 {@code clientRequestId} 重复提交返回既有节点编号，不落第二条。
     *
     * @param reportReqVO 上报请求
     * @return 节点编号（重复提交时返回既有节点的编号）
     */
    Long reportNode(@Valid LogisticsTransportNodeReportReqVO reportReqVO);

    /**
     * 按运输任务编号取节点，按发生时间正序。
     */
    List<LogisticsTransportNodeDO> getNodeListByTaskId(Long taskId);

    /**
     * 按运输任务单号取节点，按发生时间正序。没有任务或没有节点都返回空列表。
     */
    List<LogisticsTransportNodeDO> getNodeListByTaskNo(String taskNo);

    /**
     * 转成对外的响应 VO（补节点类型名与照片列表）。
     */
    List<LogisticsTransportNodeRespVO> toRespList(List<LogisticsTransportNodeDO> nodes);

}
