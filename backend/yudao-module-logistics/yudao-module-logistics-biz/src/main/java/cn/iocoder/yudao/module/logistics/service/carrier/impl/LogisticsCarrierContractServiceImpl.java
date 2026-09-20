package cn.iocoder.yudao.module.logistics.service.carrier.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractSurchargeVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierContractDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.carrier.LogisticsCarrierContractMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBearerEnum;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierContractService;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 承运合同 Service 实现（V8 #75）。
 *
 * <p>校验的出发点：一份**不限定范围**或**没写运价**的合同，对账时解释不了「这条运价凭什么适用于这趟货」。
 * 因此「适用线路或品类至少一个」「运价非负」「附加费写清名称金额承担方」都是硬校验。
 */
@Service
@Validated
public class LogisticsCarrierContractServiceImpl implements LogisticsCarrierContractService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private LogisticsCarrierContractMapper logisticsCarrierContractMapper;
    @Resource
    private LogisticsCarrierService logisticsCarrierService;

    @Override
    public Long createCarrierContract(LogisticsCarrierContractSaveReqVO createReqVO) {
        LogisticsCarrierDO carrier = logisticsCarrierService.getAssignableCarrier(createReqVO.getCarrierId());
        assertContractValid(createReqVO);
        String contractNo = StrUtil.blankToDefault(StrUtil.trim(createReqVO.getContractNo()),
                generateContractNo());
        assertContractNoAvailable(contractNo, null);

        LogisticsCarrierContractDO contract = BeanUtils.toBean(createReqVO, LogisticsCarrierContractDO.class);
        contract.setId(null);
        contract.setContractNo(contractNo);
        contract.setCarrierName(carrier.getName());
        contract.setSurcharges(toSurchargesJson(createReqVO.getSurcharges()));
        logisticsCarrierContractMapper.insert(contract);
        return contract.getId();
    }

    @Override
    public void updateCarrierContract(LogisticsCarrierContractSaveReqVO updateReqVO) {
        LogisticsCarrierContractDO exists = getCarrierContract(updateReqVO.getId());
        LogisticsCarrierDO carrier = logisticsCarrierService.getAssignableCarrier(updateReqVO.getCarrierId());
        assertContractValid(updateReqVO);
        String contractNo = StrUtil.blankToDefault(StrUtil.trim(updateReqVO.getContractNo()),
                exists.getContractNo());
        assertContractNoAvailable(contractNo, exists.getId());

        LogisticsCarrierContractDO update = BeanUtils.toBean(updateReqVO, LogisticsCarrierContractDO.class);
        update.setContractNo(contractNo);
        update.setCarrierName(carrier.getName());
        update.setSurcharges(toSurchargesJson(updateReqVO.getSurcharges()));
        logisticsCarrierContractMapper.updateById(update);
    }

    @Override
    public void deleteCarrierContract(Long id) {
        getCarrierContract(id);
        logisticsCarrierContractMapper.deleteById(id);
    }

    @Override
    public LogisticsCarrierContractDO getCarrierContract(Long id) {
        LogisticsCarrierContractDO contract = id == null ? null : logisticsCarrierContractMapper.selectById(id);
        if (contract == null) {
            throw exception(CARRIER_CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    @Override
    public LogisticsCarrierContractDO getEffectiveContract(Long id) {
        LogisticsCarrierContractDO contract = getCarrierContract(id);
        if (!CommonStatusEnum.isEnable(contract.getStatus())) {
            throw exception(CARRIER_CONTRACT_NOT_EFFECTIVE);
        }
        LocalDate today = LocalDate.now();
        if (contract.getEffectiveFrom() != null && today.isBefore(contract.getEffectiveFrom())) {
            throw exception(CARRIER_CONTRACT_NOT_EFFECTIVE);
        }
        if (contract.getEffectiveTo() != null && today.isAfter(contract.getEffectiveTo())) {
            throw exception(CARRIER_CONTRACT_NOT_EFFECTIVE);
        }
        return contract;
    }

    @Override
    public PageResult<LogisticsCarrierContractDO> getCarrierContractPage(
            LogisticsCarrierContractPageReqVO pageReqVO) {
        return logisticsCarrierContractMapper.selectPage(pageReqVO);
    }

    @Override
    public List<LogisticsCarrierContractDO> getCarrierContractList(LogisticsCarrierContractPageReqVO exportReqVO) {
        return logisticsCarrierContractMapper.selectList(exportReqVO);
    }

    @Override
    public List<LogisticsCarrierContractDO> getContractListByCarrierId(Long carrierId) {
        return carrierId == null ? List.of() : logisticsCarrierContractMapper.selectListByCarrierId(carrierId);
    }

    // ==================== 内部方法 ====================

    private void assertContractValid(LogisticsCarrierContractSaveReqVO reqVO) {
        if (reqVO.getEffectiveTo() != null && reqVO.getEffectiveFrom() != null
                && reqVO.getEffectiveFrom().isAfter(reqVO.getEffectiveTo())) {
            throw exception(CARRIER_CONTRACT_EFFECTIVE_RANGE_INVALID);
        }
        if (StrUtil.isBlank(reqVO.getRoute()) && reqVO.getGoodsConfigId() == null) {
            throw exception(CARRIER_CONTRACT_SCOPE_REQUIRED);
        }
        if (reqVO.getUnitPrice().signum() < 0) {
            throw exception(CARRIER_CONTRACT_PRICE_INVALID);
        }
        if (CollUtil.isNotEmpty(reqVO.getSurcharges())) {
            for (LogisticsCarrierContractSurchargeVO surcharge : reqVO.getSurcharges()) {
                if (StrUtil.isBlank(surcharge.getName()) || surcharge.getAmount() == null
                        || surcharge.getAmount().signum() < 0
                        || LogisticsFreightBearerEnum.ofBearer(surcharge.getBearer()).isEmpty()) {
                    throw exception(CARRIER_CONTRACT_SURCHARGE_INVALID);
                }
            }
        }
    }

    private void assertContractNoAvailable(String contractNo, Long id) {
        LogisticsCarrierContractDO exists = logisticsCarrierContractMapper.selectByContractNo(contractNo);
        if (exists != null && !Objects.equals(exists.getId(), id)) {
            throw exception(CARRIER_CONTRACT_NO_DUPLICATE);
        }
    }

    private String toSurchargesJson(List<LogisticsCarrierContractSurchargeVO> surcharges) {
        return CollUtil.isEmpty(surcharges) ? null : JsonUtils.toJsonString(surcharges);
    }

    private String generateContractNo() {
        return "CC" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

}
