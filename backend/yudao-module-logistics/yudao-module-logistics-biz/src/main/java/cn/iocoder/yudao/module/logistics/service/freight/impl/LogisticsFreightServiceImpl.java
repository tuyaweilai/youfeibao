package cn.iocoder.yudao.module.logistics.service.freight.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractSurchargeVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightConfirmReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightPayReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightReconciliationReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightReconciliationRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightUpdateReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierContractDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.freight.LogisticsFreightOrderDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.freight.LogisticsFreightOrderMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBearerEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBillingModeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsFreightStatusEnum;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierContractService;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierService;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.freight.LogisticsFreightService;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 承运商运费 Service 实现（V8 #75）。
 *
 * <p><b>本类不碰收购单 / 结算单 / 发票 / 付款</b>：运费是另一笔账（CONTEXT.md「运费」）。
 * 这里只把「应付给承运商多少」算清、对清、留清。
 *
 * <p>运价从**有效期内的承运合同**取并快照，合同改了不回算历史账。
 */
@Service
@Validated
public class LogisticsFreightServiceImpl implements LogisticsFreightService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int AMOUNT_SCALE = 2;

    @Resource
    private LogisticsFreightOrderMapper logisticsFreightOrderMapper;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsDriverService logisticsDriverService;
    @Resource
    private LogisticsCarrierService logisticsCarrierService;
    @Resource
    private LogisticsCarrierContractService logisticsCarrierContractService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFreight(LogisticsFreightCreateReqVO reqVO) {
        // 1. 任务必须存在，且这一趟必须是承运商的车：自有车不虚造承运商运费
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTask(reqVO.getTaskId());
        LogisticsCarrierDO carrier = resolveCarrierOfTask(task);

        // 2. 一趟一张
        if (logisticsFreightOrderMapper.selectByTaskId(task.getId()) != null) {
            throw exception(FREIGHT_TASK_ALREADY_GATHERED);
        }

        // 3. 合同必须在有效期内、且属于这一趟的承运商；运价与计费方式快照下来
        if (reqVO.getContractId() == null) {
            throw exception(FREIGHT_CONTRACT_REQUIRED);
        }
        LogisticsCarrierContractDO contract =
                logisticsCarrierContractService.getEffectiveContract(reqVO.getContractId());
        if (!carrier.getId().equals(contract.getCarrierId())) {
            throw exception(CARRIER_CONTRACT_CARRIER_MISMATCH);
        }
        assertBillQuantityValid(reqVO.getBillQuantity());

        BigDecimal baseAmount = baseAmount(reqVO.getBillQuantity(), contract.getUnitPrice());
        BigDecimal surchargeAmount = netSurcharge(contract.getSurcharges());

        LogisticsFreightOrderDO order = LogisticsFreightOrderDO.builder()
                .freightNo(generateFreightNo())
                .taskId(task.getId())
                .taskNo(task.getTaskNo())
                .carrierId(carrier.getId())
                .carrierName(carrier.getName())
                .contractId(contract.getId())
                .contractNo(contract.getContractNo())
                .billingMode(contract.getBillingMode())
                .billQuantity(reqVO.getBillQuantity())
                .billUnitPrice(contract.getUnitPrice())
                .baseAmount(baseAmount)
                .surchargeAmount(surchargeAmount)
                .expectedAmount(baseAmount.add(surchargeAmount))
                .status(LogisticsFreightStatusEnum.PENDING_CONFIRM.getStatus())
                .remark(reqVO.getRemark())
                .build();
        logisticsFreightOrderMapper.insert(order);
        return order.getId();
    }

    @Override
    public void updateFreight(LogisticsFreightUpdateReqVO updateReqVO) {
        LogisticsFreightOrderDO order = getFreight(updateReqVO.getId());
        if (!LogisticsFreightStatusEnum.PENDING_CONFIRM.getStatus().equals(order.getStatus())) {
            throw exception(FREIGHT_STATUS_NOT_ALLOW_UPDATE);
        }
        assertBillQuantityValid(updateReqVO.getBillQuantity());

        BigDecimal baseAmount = baseAmount(updateReqVO.getBillQuantity(), order.getBillUnitPrice());
        BigDecimal expectedAmount = baseAmount.add(nullToZero(order.getSurchargeAmount()));
        LogisticsFreightOrderDO update = LogisticsFreightOrderDO.builder()
                .id(order.getId())
                .billQuantity(updateReqVO.getBillQuantity())
                .baseAmount(baseAmount)
                .expectedAmount(expectedAmount)
                .remark(updateReqVO.getRemark())
                .build();
        applyActualAmount(update, expectedAmount, updateReqVO.getActualAmount(),
                updateReqVO.getVarianceReason());
        logisticsFreightOrderMapper.updateById(update);
    }

    @Override
    public void confirmPayable(LogisticsFreightConfirmReqVO confirmReqVO) {
        LogisticsFreightOrderDO order = getFreight(confirmReqVO.getId());
        if (!LogisticsFreightStatusEnum.PENDING_CONFIRM.getStatus().equals(order.getStatus())) {
            throw exception(FREIGHT_STATUS_NOT_ALLOW_CONFIRM);
        }
        if (confirmReqVO.getActualAmount() == null) {
            throw exception(FREIGHT_ACTUAL_AMOUNT_REQUIRED);
        }
        BigDecimal actualAmount = scale2(confirmReqVO.getActualAmount());
        BigDecimal expectedAmount = nullToZero(order.getExpectedAmount());
        BigDecimal variance = actualAmount.subtract(expectedAmount);
        assertVarianceReason(variance, confirmReqVO.getVarianceReason());

        LogisticsFreightOrderDO update = LogisticsFreightOrderDO.builder()
                .id(order.getId())
                .actualAmount(actualAmount)
                .varianceAmount(scale2(variance))
                .varianceReason(StrUtil.trim(confirmReqVO.getVarianceReason()))
                .status(LogisticsFreightStatusEnum.CONFIRMED.getStatus())
                .confirmBy(SecurityFrameworkUtils.getLoginUserId())
                .confirmByName(SecurityFrameworkUtils.getLoginUserNickname())
                .confirmTime(LocalDateTime.now())
                .confirmRemark(StrUtil.trim(confirmReqVO.getConfirmRemark()))
                .build();
        logisticsFreightOrderMapper.updateById(update);
    }

    @Override
    public void registerPaymentVoucher(LogisticsFreightPayReqVO payReqVO) {
        LogisticsFreightOrderDO order = getFreight(payReqVO.getId());
        if (!LogisticsFreightStatusEnum.CONFIRMED.getStatus().equals(order.getStatus())) {
            throw exception(FREIGHT_STATUS_NOT_ALLOW_PAY);
        }
        if (StrUtil.isBlank(payReqVO.getPaymentVoucherNo())
                && StrUtil.isBlank(payReqVO.getPaymentVoucherUrl())) {
            throw exception(FREIGHT_VOUCHER_REQUIRED);
        }
        LogisticsFreightOrderDO update = LogisticsFreightOrderDO.builder()
                .id(order.getId())
                .paymentVoucherNo(StrUtil.trim(payReqVO.getPaymentVoucherNo()))
                .paymentVoucherUrl(StrUtil.trim(payReqVO.getPaymentVoucherUrl()))
                .paymentAmount(payReqVO.getPaymentAmount() != null ? scale2(payReqVO.getPaymentAmount())
                        : order.getActualAmount())
                .paidAt(payReqVO.getPaidAt() != null ? payReqVO.getPaidAt() : LocalDateTime.now())
                .paymentRemark(StrUtil.trim(payReqVO.getPaymentRemark()))
                .status(LogisticsFreightStatusEnum.VOUCHER_REGISTERED.getStatus())
                .build();
        logisticsFreightOrderMapper.updateById(update);
    }

    @Override
    public LogisticsFreightOrderDO getFreight(Long id) {
        LogisticsFreightOrderDO order = id == null ? null : logisticsFreightOrderMapper.selectById(id);
        if (order == null) {
            throw exception(FREIGHT_NOT_EXISTS);
        }
        return order;
    }

    @Override
    public LogisticsFreightOrderDO getFreightByTaskId(Long taskId) {
        return logisticsFreightOrderMapper.selectByTaskId(taskId);
    }

    @Override
    public PageResult<LogisticsFreightOrderDO> getFreightPage(LogisticsFreightPageReqVO pageReqVO) {
        return logisticsFreightOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public List<LogisticsFreightOrderDO> getFreightList(LogisticsFreightPageReqVO exportReqVO) {
        return logisticsFreightOrderMapper.selectList(exportReqVO);
    }

    @Override
    public List<LogisticsFreightReconciliationRespVO> getReconciliation(
            LogisticsFreightReconciliationReqVO reqVO) {
        List<LogisticsFreightOrderDO> orders = logisticsFreightOrderMapper.selectList(reqVO);
        Map<String, LogisticsFreightReconciliationRespVO> grouped = new LinkedHashMap<>();
        for (LogisticsFreightOrderDO order : orders) {
            String key = order.getCarrierId() + "|" + order.getContractId();
            LogisticsFreightReconciliationRespVO row = grouped.computeIfAbsent(key, ignored -> {
                LogisticsFreightReconciliationRespVO created = new LogisticsFreightReconciliationRespVO();
                created.setCarrierId(order.getCarrierId());
                created.setCarrierName(order.getCarrierName());
                created.setContractId(order.getContractId());
                created.setContractNo(order.getContractNo());
                created.setTripCount(0);
                created.setExpectedTotal(scale2(BigDecimal.ZERO));
                created.setActualTotal(scale2(BigDecimal.ZERO));
                created.setVarianceTotal(scale2(BigDecimal.ZERO));
                created.setPendingConfirmCount(0);
                created.setVoucherRegisteredCount(0);
                return created;
            });
            BigDecimal expected = nullToZero(order.getExpectedAmount());
            // 未对账的按应有计：对账汇总不能因为「还没确认」而少算欠款
            BigDecimal actual = order.getActualAmount() != null ? order.getActualAmount() : expected;
            row.setTripCount(row.getTripCount() + 1);
            row.setExpectedTotal(scale2(row.getExpectedTotal().add(expected)));
            row.setActualTotal(scale2(row.getActualTotal().add(actual)));
            row.setVarianceTotal(scale2(row.getVarianceTotal().add(actual.subtract(expected))));
            if (LogisticsFreightStatusEnum.PENDING_CONFIRM.getStatus().equals(order.getStatus())) {
                row.setPendingConfirmCount(row.getPendingConfirmCount() + 1);
            }
            if (LogisticsFreightStatusEnum.VOUCHER_REGISTERED.getStatus().equals(order.getStatus())) {
                row.setVoucherRegisteredCount(row.getVoucherRegisteredCount() + 1);
            }
        }
        return List.copyOf(grouped.values());
    }

    @Override
    public LogisticsFreightRespVO toResp(LogisticsFreightOrderDO order) {
        LogisticsFreightRespVO resp = BeanUtils.toBean(order, LogisticsFreightRespVO.class);
        resp.setBillingModeName(LogisticsFreightBillingModeEnum.nameOf(order.getBillingMode()));
        resp.setStatusName(LogisticsFreightStatusEnum.nameOf(order.getStatus()));
        return resp;
    }

    // ==================== 内部方法 ====================

    /**
     * 这一趟的承运商：只有**承运商来源**的司机所挂的承运商才算数；自有车直接拒绝。
     */
    private LogisticsCarrierDO resolveCarrierOfTask(LogisticsTransportTaskDO task) {
        if (task.getDriverId() == null) {
            throw exception(FREIGHT_CARRIER_REQUIRED);
        }
        LogisticsDriverDO driver = logisticsDriverService.getDriver(task.getDriverId());
        if (!LogisticsDriverSourceEnum.CARRIER.getSource().equals(driver.getSource())
                || driver.getCarrierId() == null) {
            throw exception(FREIGHT_CARRIER_REQUIRED);
        }
        LogisticsCarrierDO carrier = logisticsCarrierService.getCarrier(driver.getCarrierId());
        if (!CommonStatusEnum.isEnable(carrier.getStatus())) {
            throw exception(FREIGHT_CARRIER_NOT_ACTIVE);
        }
        return carrier;
    }

    private void assertBillQuantityValid(BigDecimal billQuantity) {
        if (billQuantity == null || billQuantity.signum() <= 0) {
            throw exception(FREIGHT_BILL_QUANTITY_INVALID);
        }
    }

    private void applyActualAmount(LogisticsFreightOrderDO update, BigDecimal expectedAmount,
                                   BigDecimal actualAmount, String varianceReason) {
        if (actualAmount == null) {
            update.setActualAmount(null);
            update.setVarianceAmount(null);
            update.setVarianceReason(null);
            return;
        }
        BigDecimal actual = scale2(actualAmount);
        BigDecimal variance = actual.subtract(expectedAmount);
        assertVarianceReason(variance, varianceReason);
        update.setActualAmount(actual);
        update.setVarianceAmount(scale2(variance));
        update.setVarianceReason(StrUtil.trim(varianceReason));
    }

    private void assertVarianceReason(BigDecimal variance, String varianceReason) {
        if (variance.signum() != 0 && StrUtil.isBlank(varianceReason)) {
            throw exception(FREIGHT_VARIANCE_REASON_REQUIRED);
        }
    }

    private static BigDecimal baseAmount(BigDecimal billQuantity, BigDecimal billUnitPrice) {
        return scale2(billQuantity.multiply(nullToZero(billUnitPrice)));
    }

    /**
     * 附加费净额 = 本企业承担的 − 承运商承担的：前者加进应付，后者从应付里净掉。
     */
    private static BigDecimal netSurcharge(String surchargesJson) {
        if (StrUtil.isBlank(surchargesJson)) {
            return scale2(BigDecimal.ZERO);
        }
        List<LogisticsCarrierContractSurchargeVO> surcharges =
                JsonUtils.parseArray(surchargesJson, LogisticsCarrierContractSurchargeVO.class);
        BigDecimal net = BigDecimal.ZERO;
        for (LogisticsCarrierContractSurchargeVO surcharge : surcharges) {
            BigDecimal amount = nullToZero(surcharge.getAmount());
            if (LogisticsFreightBearerEnum.COMPANY.getBearer().equals(surcharge.getBearer())) {
                net = net.add(amount);
            } else if (LogisticsFreightBearerEnum.CARRIER.getBearer().equals(surcharge.getBearer())) {
                net = net.subtract(amount);
            }
        }
        return scale2(net);
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal scale2(BigDecimal value) {
        return value.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private String generateFreightNo() {
        return "FR" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

}
