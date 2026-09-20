package cn.iocoder.yudao.module.logistics.service.carrier;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 承运商档案 Service（V3 #70）。
 *
 * <p>承运商是运输服务的提供方（CONTEXT.md「承运商」），司机通过 carrierId 挂到它下面，
 * 于是「运费该付给谁」有据可查。承运合同与运价归 V8（#75）。
 */
public interface LogisticsCarrierService {

    Long createCarrier(@Valid LogisticsCarrierSaveReqVO createReqVO);

    void updateCarrier(@Valid LogisticsCarrierSaveReqVO updateReqVO);

    void deleteCarrier(Long id);

    /**
     * 获得承运商；不存在时抛业务异常。
     */
    LogisticsCarrierDO getCarrier(Long id);

    /**
     * 获得**可指派**的承运商：必须存在且未停用。派承运商司机时用。
     */
    LogisticsCarrierDO getAssignableCarrier(Long carrierId);

    PageResult<LogisticsCarrierDO> getCarrierPage(LogisticsCarrierPageReqVO pageReqVO);

    List<LogisticsCarrierDO> getCarrierList(LogisticsCarrierPageReqVO exportReqVO);

}
