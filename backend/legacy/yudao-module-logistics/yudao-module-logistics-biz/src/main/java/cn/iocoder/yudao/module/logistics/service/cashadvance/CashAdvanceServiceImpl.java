package cn.iocoder.yudao.module.logistics.service.cashadvance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo.*;
import cn.iocoder.yudao.module.logistics.convert.cashadvance.CashAdvanceConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.cashadvance.CashAdvanceDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.cashadvance.CashAdvanceMapper;
import cn.iocoder.yudao.module.logistics.enums.CashAdvanceNotifyStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.CashAdvanceReconcileStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 物流现金代付记录 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class CashAdvanceServiceImpl implements CashAdvanceService {

    @Resource
    private CashAdvanceMapper cashAdvanceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCashAdvance(@Valid CashAdvanceCreateReqVO createReqVO) {
        // 插入
        CashAdvanceDO cashAdvance = CashAdvanceConvert.INSTANCE.convert(createReqVO);
        // 设置默认通知状态
        if (cashAdvance.getNotifyStatus() == null) {
            cashAdvance.setNotifyStatus(CashAdvanceNotifyStatusEnum.NOT_NOTIFIED.getStatus());
        }
        // 设置默认对账状态
        if (cashAdvance.getReconcileStatus() == null) {
            cashAdvance.setReconcileStatus(CashAdvanceReconcileStatusEnum.NOT_RECONCILED.getStatus());
        }
        
        cashAdvanceMapper.insert(cashAdvance);
        
        log.info("[createCashAdvance][创建现金代付记录成功，司机：{}，金额：{}]", 
                createReqVO.getDriverName(), createReqVO.getPaymentAmount());
        
        // 返回
        return cashAdvance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCashAdvance(@Valid CashAdvanceUpdateReqVO updateReqVO) {
        // 校验存在
        validateCashAdvanceExists(updateReqVO.getId());
        
        // 更新
        CashAdvanceDO updateObj = CashAdvanceConvert.INSTANCE.convert(updateReqVO);
        cashAdvanceMapper.updateById(updateObj);
        
        log.info("[updateCashAdvance][更新现金代付记录成功，ID：{}]", updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCashAdvance(Long id) {
        // 校验存在
        CashAdvanceDO cashAdvance = validateCashAdvanceExists(id);
        
        // 校验是否可以删除
        if (CashAdvanceReconcileStatusEnum.RECONCILED.getStatus().equals(cashAdvance.getReconcileStatus())) {
            throw exception(CASH_ADVANCE_CANNOT_DELETE_RECONCILED);
        }
        
        // 删除
        cashAdvanceMapper.deleteById(id);
        
        log.info("[deleteCashAdvance][删除现金代付记录成功，ID：{}]", id);
    }

    @Override
    public CashAdvanceDO getCashAdvance(Long id) {
        return cashAdvanceMapper.selectById(id);
    }

    @Override
    public CashAdvanceRespVO getCashAdvanceDetail(Long id) {
        CashAdvanceDO cashAdvance = getCashAdvance(id);
        return CashAdvanceConvert.INSTANCE.convert(cashAdvance);
    }

    @Override
    public PageResult<CashAdvanceRespVO> getCashAdvancePage(CashAdvancePageReqVO pageReqVO) {
        PageResult<CashAdvanceDO> pageResult = cashAdvanceMapper.selectPage(pageReqVO);
        return CashAdvanceConvert.INSTANCE.convertPage(pageResult);
    }

    @Override
    public List<CashAdvanceDO> getCashAdvanceList(CashAdvancePageReqVO exportReqVO) {
        return cashAdvanceMapper.selectList(exportReqVO);
    }

    @Override
    public List<CashAdvanceDO> getCashAdvanceListByTaskId(Long taskId) {
        return cashAdvanceMapper.selectByTaskId(taskId);
    }

    @Override
    public List<CashAdvanceDO> getCashAdvanceListByOrderId(Long orderId) {
        return cashAdvanceMapper.selectByOrderId(orderId);
    }

    @Override
    public List<CashAdvanceDO> getCashAdvanceListByDriverId(Long driverId) {
        return cashAdvanceMapper.selectByDriverId(driverId);
    }

    @Override
    public List<CashAdvanceDO> getCashAdvanceListByNotifyStatus(Integer notifyStatus) {
        return cashAdvanceMapper.selectByNotifyStatus(notifyStatus);
    }

    @Override
    public List<CashAdvanceDO> getCashAdvanceListByReconcileStatus(Integer reconcileStatus) {
        return cashAdvanceMapper.selectByReconcileStatus(reconcileStatus);
    }

    @Override
    public List<CashAdvanceDO> getCashAdvanceListByPaymentMethod(String paymentMethod) {
        return cashAdvanceMapper.selectByPaymentMethod(paymentMethod);
    }

    @Override
    public Long getCashAdvanceCountByTaskId(Long taskId) {
        return cashAdvanceMapper.selectCountByTaskId(taskId);
    }

    @Override
    public Long getCashAdvanceCountByOrderId(Long orderId) {
        return cashAdvanceMapper.selectCountByOrderId(orderId);
    }

    @Override
    public Long getCashAdvanceCountByDriverId(Long driverId) {
        return cashAdvanceMapper.selectCountByDriverId(driverId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyCashAdvance(Long id) {
        // 校验存在
        CashAdvanceDO cashAdvance = validateCashAdvanceExists(id);
        
        // 校验状态
        if (!CashAdvanceNotifyStatusEnum.NOT_NOTIFIED.getStatus().equals(cashAdvance.getNotifyStatus())) {
            throw exception(CASH_ADVANCE_NOTIFY_STATUS_NOT_NOT_NOTIFIED);
        }
        
        // 更新通知状态
        CashAdvanceDO updateObj = new CashAdvanceDO();
        updateObj.setId(id);
        updateObj.setNotifyStatus(CashAdvanceNotifyStatusEnum.NOTIFIED.getStatus());
        updateObj.setNotifyTime(LocalDateTime.now());
        
        cashAdvanceMapper.updateById(updateObj);
        
        log.info("[notifyCashAdvance][现金代付记录通知成功，ID：{}]", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcileCashAdvance(Long id, String reconcileRemark, Long reconcileOperatorId, String reconcileOperatorName) {
        // 校验存在
        CashAdvanceDO cashAdvance = validateCashAdvanceExists(id);
        
        // 校验状态
        if (!CashAdvanceReconcileStatusEnum.NOT_RECONCILED.getStatus().equals(cashAdvance.getReconcileStatus())) {
            throw exception(CASH_ADVANCE_RECONCILE_STATUS_NOT_NOT_RECONCILED);
        }
        
        // 更新对账状态
        CashAdvanceDO updateObj = new CashAdvanceDO();
        updateObj.setId(id);
        updateObj.setReconcileStatus(CashAdvanceReconcileStatusEnum.RECONCILED.getStatus());
        updateObj.setReconcileTime(LocalDateTime.now());
        updateObj.setReconcileRemark(reconcileRemark);
        updateObj.setReconcileOperatorId(reconcileOperatorId);
        updateObj.setReconcileOperatorName(reconcileOperatorName);
        
        cashAdvanceMapper.updateById(updateObj);
        
        log.info("[reconcileCashAdvance][现金代付记录对账成功，ID：{}，操作员：{}]", id, reconcileOperatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcileFailedCashAdvance(Long id, String reconcileRemark, Long reconcileOperatorId, String reconcileOperatorName) {
        // 校验存在
        CashAdvanceDO cashAdvance = validateCashAdvanceExists(id);
        
        // 校验状态
        if (!CashAdvanceReconcileStatusEnum.NOT_RECONCILED.getStatus().equals(cashAdvance.getReconcileStatus())) {
            throw exception(CASH_ADVANCE_RECONCILE_STATUS_NOT_NOT_RECONCILED);
        }
        
        // 更新对账状态
        CashAdvanceDO updateObj = new CashAdvanceDO();
        updateObj.setId(id);
        updateObj.setReconcileStatus(CashAdvanceReconcileStatusEnum.RECONCILE_FAILED.getStatus());
        updateObj.setReconcileTime(LocalDateTime.now());
        updateObj.setReconcileRemark(reconcileRemark);
        updateObj.setReconcileOperatorId(reconcileOperatorId);
        updateObj.setReconcileOperatorName(reconcileOperatorName);
        
        cashAdvanceMapper.updateById(updateObj);
        
        log.info("[reconcileFailedCashAdvance][现金代付记录对账失败，ID：{}，操作员：{}，原因：{}]", id, reconcileOperatorName, reconcileRemark);
    }

    @Override
    public CashAdvanceDO validateCashAdvanceExists(Long id) {
        CashAdvanceDO cashAdvance = cashAdvanceMapper.selectById(id);
        if (cashAdvance == null) {
            throw exception(CASH_ADVANCE_NOT_EXISTS);
        }
        return cashAdvance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcileConfirm(@Valid CashAdvanceReconcileReqVO reconcileReqVO) {
        reconcileCashAdvance(reconcileReqVO.getId(), reconcileReqVO.getReconcileRemark(), 
                reconcileReqVO.getReconcileOperatorId(), reconcileReqVO.getReconcileOperatorName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashAdvanceBatchReconcileResultVO batchReconcileConfirm(@Valid CashAdvanceBatchReconcileReqVO batchReqVO) {
        CashAdvanceBatchReconcileResultVO result = new CashAdvanceBatchReconcileResultVO();
        result.setTotalCount(batchReqVO.getReconcileList().size());
        result.setSuccessCount(0);
        result.setFailureCount(0);
        
        // TODO: 实现批量对账逻辑
        log.info("[batchReconcileConfirm][批量对账确认，总数：{}]", result.getTotalCount());
        
        return result;
    }

    @Override
    public CashAdvanceReconcileSummaryVO getReconcileSummary(@Valid CashAdvanceReconcileSummaryReqVO summaryReqVO) {
        CashAdvanceReconcileSummaryVO summary = new CashAdvanceReconcileSummaryVO();
        
        // TODO: 实现对账汇总逻辑
        log.info("[getReconcileSummary][获取对账汇总]");
        
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyWasteModule(Long id) {
        // 校验存在
        validateCashAdvanceExists(id);
        
        // TODO: 实现通知危废模块逻辑
        log.info("[notifyWasteModule][通知危废模块，ID：{}]", id);
    }

} 