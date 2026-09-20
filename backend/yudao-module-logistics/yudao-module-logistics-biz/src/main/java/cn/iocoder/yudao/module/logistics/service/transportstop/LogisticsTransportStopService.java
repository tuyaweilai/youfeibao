package cn.iocoder.yudao.module.logistics.service.transportstop;

import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopCancelReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportStopDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 运输停靠点 Service（V5 #72）。
 *
 * <p>停靠点是运输任务里的一个提货 / 送货地点。**每个停靠点独立推进**：它自己的节点、自己的进度、
 * 自己的断点；取消一个点不影响同一任务里的其它点（#59 的 Implementation Decisions 第 4 条）。
 *
 * <p>**一次集货不构成把几个出售者合并结算的依据**（ADR 0031）：本服务只描述运输事实，
 * 结算归属由后续的回场复磅与收购单决定，且归**派单场站**、按出售者各自成立。
 */
public interface LogisticsTransportStopService {

    /**
     * 建任务时一次带多个停靠点：按传入顺序分配停靠顺序（从 1 开始）。
     */
    List<LogisticsTransportStopDO> createStops(Long taskId, String taskNo,
                                               List<LogisticsTransportStopSaveReqVO> stops);

    /**
     * 给已有任务追加一个停靠点（停靠顺序接在最后）。
     *
     * <p>调用方（任务服务）负责校验任务状态；这里只负责分配顺序与落库。
     */
    Long addStop(Long taskId, String taskNo, @Valid LogisticsTransportStopSaveReqVO reqVO);

    /**
     * 取消一个停靠点（必填原因）。已完成或已取消的不能再取消。
     *
     * <p>**只取消这一个点**：同一任务里其它停靠点的节点、进度、凭证都不受影响。
     */
    void cancelStop(@Valid LogisticsTransportStopCancelReqVO cancelReqVO);

    /**
     * 获得停靠点；不存在时抛业务异常。
     */
    LogisticsTransportStopDO getStop(Long id);

    /**
     * 按任务取停靠点，按停靠顺序正序。没有停靠点返回空列表。
     */
    List<LogisticsTransportStopDO> getStopListByTaskId(Long taskId);

    /**
     * 节点上报后推进**该停靠点**的状态：「交接完成」→ 已完成；其余停靠点节点 → 进行中（待处理时）。
     */
    void onNodeReported(LogisticsTransportStopDO stop, LogisticsTransportNodeTypeEnum nodeType);

    /**
     * 按任务取停靠点及其**各自的**节点、进度与断点（任务详情 / 司机端用）。
     *
     * <p>返回的顺序就是停靠顺序；每个停靠点的进度只看它自己的节点，互不相串。
     */
    List<LogisticsTransportStopRespVO> getStopRespListByTaskId(Long taskId);

    /**
     * 「还剩几家没提」：任务编号 → 未完成（待处理 / 进行中）的停靠点数。
     *
     * <p>一次查批，列表页不用逐行查（N+1）。任务没有停靠点时该任务不在返回里。
     */
    Map<Long, Integer> getPendingStopCounts(Collection<Long> taskIds);

}
