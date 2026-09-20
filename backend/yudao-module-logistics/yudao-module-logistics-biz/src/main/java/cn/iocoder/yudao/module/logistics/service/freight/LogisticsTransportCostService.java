package cn.iocoder.yudao.module.logistics.service.freight;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.freight.LogisticsTransportCostDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 运输费用（内部成本）Service（V8 #75）。
 *
 * <p>自有车真实发生的路桥 / 燃油等，**按实际承担方**记在这里。它不是承运商运费（那是付给承运商的
 * 应付，走 {@code logistics_freight_order}），也不进入收购单 / 发票金额（CONTEXT.md「运费」）。
 */
public interface LogisticsTransportCostService {

    Long createTransportCost(@Valid LogisticsTransportCostSaveReqVO createReqVO);

    void updateTransportCost(@Valid LogisticsTransportCostSaveReqVO updateReqVO);

    void deleteTransportCost(Long id);

    LogisticsTransportCostDO getTransportCost(Long id);

    PageResult<LogisticsTransportCostDO> getTransportCostPage(LogisticsTransportCostPageReqVO pageReqVO);

    List<LogisticsTransportCostDO> getTransportCostList(LogisticsTransportCostPageReqVO exportReqVO);

    /**
     * 按任务取运输费用，按登记时间倒序。
     */
    List<LogisticsTransportCostDO> getTransportCostListByTaskId(Long taskId);

    LogisticsTransportCostRespVO toResp(LogisticsTransportCostDO cost);

}
