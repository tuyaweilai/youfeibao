package cn.iocoder.yudao.module.logistics.service.carrier;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierContractDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 承运合同 Service（V8 #75）。
 *
 * <p>承运合同约定运价与计费方式，是**运费对账**的依据（CONTEXT.md「承运合同」）。
 * 它属于某个承运商、有有效期、限定适用线路或品类，并可带附加费与承担方。
 */
public interface LogisticsCarrierContractService {

    Long createCarrierContract(@Valid LogisticsCarrierContractSaveReqVO createReqVO);

    void updateCarrierContract(@Valid LogisticsCarrierContractSaveReqVO updateReqVO);

    void deleteCarrierContract(Long id);

    /**
     * 获得承运合同；不存在时抛业务异常。
     */
    LogisticsCarrierContractDO getCarrierContract(Long id);

    /**
     * 获得**可用于汇集运费**的承运合同：必须存在、未停用、且在有效期内。
     *
     * <p>已停用或过期的合同仍然可以查询与查看（历史运单要看得到它），但**不能**再据它算新运费。
     */
    LogisticsCarrierContractDO getEffectiveContract(Long id);

    PageResult<LogisticsCarrierContractDO> getCarrierContractPage(LogisticsCarrierContractPageReqVO pageReqVO);

    List<LogisticsCarrierContractDO> getCarrierContractList(LogisticsCarrierContractPageReqVO exportReqVO);

    /**
     * 按承运商取合同（按生效日期倒序），运费汇集选合同时用。
     */
    List<LogisticsCarrierContractDO> getContractListByCarrierId(Long carrierId);

}
