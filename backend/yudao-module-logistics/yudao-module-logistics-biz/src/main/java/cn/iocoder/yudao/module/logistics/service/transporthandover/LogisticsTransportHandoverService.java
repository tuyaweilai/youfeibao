package cn.iocoder.yudao.module.logistics.service.transporthandover;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporthandover.LogisticsTransportHandoverDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 交接登记 Service（V6 #73，见 ADR 0030 / 0031 与 CONTEXT「交接登记」）。
 *
 * <p>司机在提货点就一个停靠点登记交接事实：品类、参考量、参考单价与凭证照片。
 * **现场不产生金额、不产生收购单**——收购单在回场复磅后由 icbc 侧按本登记生成。
 *
 * <p>与「交接确认」是两件事：交接登记是**登记事实**（谈好的品类与参考量价），
 * 节点里的「交接完成」是**运输事实**（货装上车了）。两者都不等于出售者的结算确认
 *（ADR 0030：三个动作分开命名、分开留痕）。
 */
public interface LogisticsTransportHandoverService {

    /**
     * 登记交接（按任务 + 停靠点）。
     *
     * <p>幂等：带 {@code clientRequestId} 时重复提交返回既有登记；同一个停靠点重复登记会被拦住
     *（补录走既有那条，不新增）。
     *
     * @return 交接登记编号
     */
    Long createHandover(@Valid LogisticsTransportHandoverCreateReqVO reqVO);

    /**
     * 获得交接登记；不存在时抛业务异常。
     */
    LogisticsTransportHandoverDO getHandover(Long id);

    /**
     * 按停靠点取交接登记；没有时返回 null（「这一家还没登记」是业务状态，不是错误）。
     */
    LogisticsTransportHandoverDO getHandoverByStopId(Long stopId);

    /**
     * 按任务取全部交接登记（集货时一家一条），按停靠点正序。
     */
    List<LogisticsTransportHandoverDO> getHandoverListByTaskId(Long taskId);

    PageResult<LogisticsTransportHandoverDO> getPage(@Valid LogisticsTransportHandoverPageReqVO reqVO);

    /**
     * 转响应 VO（含要件状态名与照片列表）。
     */
    LogisticsTransportHandoverRespVO toResp(LogisticsTransportHandoverDO handover);

}
