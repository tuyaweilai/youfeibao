package cn.iocoder.yudao.module.waste.service.impl.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.PaymentStatusHistoryCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.PaymentStatusHistoryPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.PaymentStatusHistoryRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.PaymentStatusHistoryDO;
import cn.iocoder.yudao.module.waste.dal.mysql.payment.PaymentStatusHistoryMapper;
import cn.iocoder.yudao.module.waste.service.payment.PaymentStatusHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.PAYMENT_STATUS_HISTORY_NOT_EXISTS;

/**
 * 付款状态变更历史 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class PaymentStatusHistoryServiceImpl implements PaymentStatusHistoryService {

    @Resource
    private PaymentStatusHistoryMapper paymentStatusHistoryMapper;

    @Override
    public Long createPaymentStatusHistory(@Valid PaymentStatusHistoryCreateReqVO createReqVO) {
        // 插入
        PaymentStatusHistoryDO paymentStatusHistory = BeanUtils.toBean(createReqVO, PaymentStatusHistoryDO.class);
        paymentStatusHistoryMapper.insert(paymentStatusHistory);
        // 返回
        return paymentStatusHistory.getId();
    }

    @Override
    public void deletePaymentStatusHistory(Long id) {
        // 校验存在
        validatePaymentStatusHistoryExists(id);
        // 删除
        paymentStatusHistoryMapper.deleteById(id);
    }

    @Override
    public PaymentStatusHistoryDO getPaymentStatusHistory(Long id) {
        return paymentStatusHistoryMapper.selectById(id);
    }

    @Override
    public PaymentStatusHistoryRespVO getPaymentStatusHistoryDetail(Long id) {
        PaymentStatusHistoryDO paymentStatusHistory = validatePaymentStatusHistoryExists(id);
        return BeanUtils.toBean(paymentStatusHistory, PaymentStatusHistoryRespVO.class);
    }

    @Override
    public PageResult<PaymentStatusHistoryDO> getPaymentStatusHistoryPage(PaymentStatusHistoryPageReqVO pageReqVO) {
        PageResult<PaymentStatusHistoryDO> pageResult = paymentStatusHistoryMapper.selectPage(pageReqVO);
        return pageResult;
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoryList(PaymentStatusHistoryPageReqVO exportReqVO) {
        return paymentStatusHistoryMapper.selectList(exportReqVO);
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByOrderId(Long orderId) {
        return paymentStatusHistoryMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByStatusFrom(Integer statusFrom) {
        return paymentStatusHistoryMapper.selectListByStatusFrom(statusFrom);
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByStatusTo(Integer statusTo) {
        return paymentStatusHistoryMapper.selectListByStatusTo(statusTo);
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByOperatorType(Integer operatorType) {
        return paymentStatusHistoryMapper.selectListByOperatorType(operatorType);
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByOperatorId(Long operatorId) {
        return paymentStatusHistoryMapper.selectListByOperatorId(operatorId);
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByOperatorName(String operatorName) {
        return paymentStatusHistoryMapper.selectListByOperatorName(operatorName);
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByPaymentMethod(Integer paymentMethod) {
        return paymentStatusHistoryMapper.selectListByPaymentMethod(paymentMethod);
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByVoucherId(Long voucherId) {
        return paymentStatusHistoryMapper.selectListByVoucherId(voucherId);
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByChangeTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return paymentStatusHistoryMapper.selectListByChangeTimeRange(startTime, endTime);
    }

    @Override
    public List<PaymentStatusHistoryDO> getSystemOperationPaymentStatusHistories() {
        return paymentStatusHistoryMapper.selectSystemOperationHistories();
    }

    @Override
    public List<PaymentStatusHistoryDO> getProducerOperationPaymentStatusHistories() {
        return paymentStatusHistoryMapper.selectProducerOperationHistories();
    }

    @Override
    public List<PaymentStatusHistoryDO> getRecyclerOperationPaymentStatusHistories() {
        return paymentStatusHistoryMapper.selectRecyclerOperationHistories();
    }

    @Override
    public List<PaymentStatusHistoryDO> getAdminOperationPaymentStatusHistories() {
        return paymentStatusHistoryMapper.selectAdminOperationHistories();
    }

    @Override
    public List<PaymentStatusHistoryDO> getBankTransferPaymentStatusHistories() {
        return paymentStatusHistoryMapper.selectBankTransferHistories();
    }

    @Override
    public List<PaymentStatusHistoryDO> getCashPaymentStatusHistories() {
        return paymentStatusHistoryMapper.selectCashPaymentHistories();
    }

    @Override
    public List<PaymentStatusHistoryDO> getCheckPaymentStatusHistories() {
        return paymentStatusHistoryMapper.selectCheckPaymentHistories();
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoriesWithVoucher() {
        return paymentStatusHistoryMapper.selectHistoriesWithVoucher();
    }

    @Override
    public PaymentStatusHistoryDO getLatestPaymentStatusHistoryByOrderId(Long orderId) {
        return paymentStatusHistoryMapper.selectLatestByOrderId(orderId);
    }

    @Override
    public PaymentStatusHistoryDO getFirstPaymentStatusHistoryByOrderId(Long orderId) {
        return paymentStatusHistoryMapper.selectFirstByOrderId(orderId);
    }

    @Override
    public List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByStatusTransition(Integer statusFrom, Integer statusTo) {
        return paymentStatusHistoryMapper.selectListByStatusTransition(statusFrom, statusTo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordPaymentStatusChange(Long orderId, Integer statusFrom, Integer statusTo, String statusName,
                                         String changeReason, Integer operatorType, Long operatorId, String operatorName,
                                         BigDecimal paymentAmount, Integer paymentMethod, Long voucherId, String businessData) {
        PaymentStatusHistoryDO history = new PaymentStatusHistoryDO();
        history.setOrderId(orderId);
        history.setStatusFrom(statusFrom);
        history.setStatusTo(statusTo);
        history.setStatusName(statusName);
        history.setChangeReason(changeReason);
        history.setOperatorType(operatorType);
        history.setOperatorId(operatorId);
        history.setOperatorName(operatorName);
        history.setPaymentAmount(paymentAmount);
        history.setPaymentMethod(paymentMethod);
        history.setVoucherId(voucherId);
        history.setBusinessData(businessData);
        history.setChangeTime(LocalDateTime.now());
        
        paymentStatusHistoryMapper.insert(history);
        log.info("[recordPaymentStatusChange][记录付款状态变更] orderId={}, statusFrom={}, statusTo={}, operatorType={}", 
                orderId, statusFrom, statusTo, operatorType);
        return history.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordSystemStatusChange(Long orderId, Integer statusFrom, Integer statusTo, String statusName, String changeReason) {
        return recordPaymentStatusChange(orderId, statusFrom, statusTo, statusName, changeReason, 
                                       1, null, "系统", null, null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordEnterpriseStatusChange(Long orderId, Integer statusFrom, Integer statusTo, String statusName,
                                           String changeReason, Integer operatorType, Long operatorId, String operatorName,
                                           BigDecimal paymentAmount, Integer paymentMethod, Long voucherId) {
        return recordPaymentStatusChange(orderId, statusFrom, statusTo, statusName, changeReason, 
                                       operatorType, operatorId, operatorName, paymentAmount, paymentMethod, voucherId, null);
    }

    @Override
    public PaymentStatusHistoryDO validatePaymentStatusHistoryExists(Long id) {
        PaymentStatusHistoryDO paymentStatusHistory = paymentStatusHistoryMapper.selectById(id);
        if (paymentStatusHistory == null) {
            throw exception(PAYMENT_STATUS_HISTORY_NOT_EXISTS);
        }
        return paymentStatusHistory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordStatusChange(Long orderId, Long paymentRecordId, Integer fromStatus, Integer toStatus, String changeReason) {
        PaymentStatusHistoryDO history = new PaymentStatusHistoryDO();
        history.setOrderId(orderId);
        history.setStatusFrom(fromStatus);
        history.setStatusTo(toStatus);
        history.setChangeReason(changeReason);
        history.setChangeTime(LocalDateTime.now());
        history.setOperatorType(2); // 手动操作
        
        paymentStatusHistoryMapper.insert(history);
        log.info("[recordStatusChange][记录状态变更] orderId={}, paymentRecordId={}, fromStatus={}, toStatus={}", 
                orderId, paymentRecordId, fromStatus, toStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSystemChange(Long orderId, Long paymentRecordId, Integer fromStatus, Integer toStatus, String changeReason, String systemAction) {
        PaymentStatusHistoryDO history = new PaymentStatusHistoryDO();
        history.setOrderId(orderId);
        history.setStatusFrom(fromStatus);
        history.setStatusTo(toStatus);
        history.setChangeReason(changeReason);
        history.setChangeTime(LocalDateTime.now());
        history.setOperatorType(1); // 系统操作
        history.setOperatorName("系统");
        history.setBusinessData(systemAction);
        
        paymentStatusHistoryMapper.insert(history);
        log.info("[recordSystemChange][记录系统变更] orderId={}, paymentRecordId={}, fromStatus={}, toStatus={}, systemAction={}", 
                orderId, paymentRecordId, fromStatus, toStatus, systemAction);
    }

    @Override
    public List<PaymentStatusHistoryDO> getHistoryByOrderId(Long orderId) {
        return paymentStatusHistoryMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<PaymentStatusHistoryDO> getHistoryByPaymentRecordId(Long paymentRecordId) {
        return paymentStatusHistoryMapper.selectListByPaymentRecordId(paymentRecordId);
    }

    @Override
    public List<PaymentStatusHistoryDO> getHistoryByOperator(Long operatorId) {
        return paymentStatusHistoryMapper.selectListByOperatorId(operatorId);
    }

    @Override
    public List<PaymentStatusHistoryDO> getSystemChanges() {
        return paymentStatusHistoryMapper.selectSystemOperationHistories();
    }

    @Override
    public List<PaymentStatusHistoryDO> getManualChanges() {
        return paymentStatusHistoryMapper.selectListByOperatorType(2); // 2-手动操作
    }

} 