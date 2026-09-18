package cn.iocoder.yudao.module.waste.service.impl.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.TransferOrderCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.TransferOrderPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.TransferOrderUpdateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.TransferOrderRespVO;
import cn.iocoder.yudao.module.waste.convert.order.TransferOrderConvert;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.TransferOrderDO;
import cn.iocoder.yudao.module.waste.dal.mysql.order.TransferOrderMapper;
import cn.iocoder.yudao.module.waste.service.order.TransferOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.*;

/**
 * 危废转移订单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class TransferOrderServiceImpl implements TransferOrderService {

    @Resource
    private TransferOrderMapper transferOrderMapper;

    @Override
    public Long createTransferOrder(TransferOrderCreateReqVO createReqVO) {
        // 插入
        TransferOrderDO transferOrder = BeanUtils.toBean(createReqVO, TransferOrderDO.class);
        
        // 生成订单号
        transferOrder.setOrderNo(generateOrderNo());
        
        transferOrderMapper.insert(transferOrder);
        // 返回
        return transferOrder.getId();
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        // 格式：TO + yyyyMMdd + 6位随机数
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%06d", (int) (Math.random() * 1000000));
        return "TO" + date + random;
    }

    @Override
    public void updateTransferOrder(TransferOrderUpdateReqVO updateReqVO) {
        // 校验存在
        validateTransferOrderExists(updateReqVO.getId());
        // 更新
        TransferOrderDO updateObj = BeanUtils.toBean(updateReqVO, TransferOrderDO.class);
        transferOrderMapper.updateById(updateObj);
    }

    @Override
    public void deleteTransferOrder(Long id) {
        // 校验存在
        validateTransferOrderExists(id);
        // 删除
        transferOrderMapper.deleteById(id);
    }

    @Override
    public TransferOrderDO getTransferOrder(Long id) {
        return transferOrderMapper.selectById(id);
    }

    @Override
    public PageResult<TransferOrderDO> getTransferOrderPage(TransferOrderPageReqVO pageReqVO) {
        return transferOrderMapper.selectPage(pageReqVO);
    }

    // ==================== 业务方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long id) {
        TransferOrderDO order = validateTransferOrderExists(id);
        if (order.getBusinessStatus() != 0) { // 0为待确认状态
            throw exception(TRANSFER_ORDER_STATUS_ERROR);
        }
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setBusinessStatus(1); // 1为已确认状态
        transferOrderMapper.updateById(updateObj);
        
        log.info("[confirmOrder][订单({})确认成功]", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, String reason) {
        TransferOrderDO order = validateTransferOrderExists(id);
        if (order.getBusinessStatus() == 5) { // 5为已取消状态
            throw exception(TRANSFER_ORDER_ALREADY_CANCELLED);
        }
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setBusinessStatus(5); // 5为已取消状态
        // 注意：数据库中没有cancelReason和cancelTime字段，这里只更新业务状态
        transferOrderMapper.updateById(updateObj);
        
        log.info("[cancelOrder][订单({})取消成功，原因：{}]", id, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long id) {
        TransferOrderDO order = validateTransferOrderExists(id);
        if (order.getBusinessStatus() != 3) { // 3为已结算状态
            throw exception(TRANSFER_ORDER_STATUS_ERROR);
        }
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setBusinessStatus(4); // 4为已完成状态
        transferOrderMapper.updateById(updateObj);
        
        log.info("[completeOrder][订单({})完成]", id);
    }

    @Override
    public List<TransferOrderDO> getPendingAllocationOrders() {
        return transferOrderMapper.selectPendingAllocationOrders();
    }

    @Override
    public List<TransferOrderDO> getUnallocatedOrders() {
        return transferOrderMapper.selectUnallocatedOrders();
    }

    @Override
    public List<TransferOrderDO> getPendingPaymentOrders() {
        return transferOrderMapper.selectPendingPaymentOrders();
    }

    @Override
    public List<TransferOrderDO> getOrdersByProducerEnterprise(Long producerEnterpriseId) {
        return transferOrderMapper.selectByProducerEnterpriseId(producerEnterpriseId);
    }

    @Override
    public List<TransferOrderDO> getOrdersByRecyclerEnterprise(Long recyclerEnterpriseId) {
        return transferOrderMapper.selectByRecyclerEnterpriseId(recyclerEnterpriseId);
    }

    @Override
    public List<TransferOrderDO> getOrdersByStatus(Integer status) {
        return transferOrderMapper.selectByStatus(status);
    }

    @Override
    public List<TransferOrderDO> getOrdersByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return transferOrderMapper.selectByDateRange(startTime, endTime);
    }

    @Override
    public List<TransferOrderDO> getTransferOrderListByPaymentStatus(Integer paymentStatus) {
        return transferOrderMapper.selectByPaymentStatus(paymentStatus);
    }

    @Override
    public List<TransferOrderDO> getTransferOrderListByBusinessStatus(Integer businessStatus) {
        return transferOrderMapper.selectByBusinessStatus(businessStatus);
    }

    @Override
    public List<TransferOrderDO> getTransferOrderListByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return transferOrderMapper.selectByRecyclingEnterpriseId(recyclingEnterpriseId);
    }

    @Override
    public List<TransferOrderDO> getTransferOrderListByProducingEnterpriseId(Long producingEnterpriseId) {
        return transferOrderMapper.selectByProducingEnterpriseId(producingEnterpriseId);
    }

    @Override
    public List<TransferOrderDO> getTransferOrderListByAppointmentId(Long appointmentId) {
        return transferOrderMapper.selectByAppointmentId(appointmentId);
    }

    @Override
    public TransferOrderDO getTransferOrderByNo(String orderNo) {
        return transferOrderMapper.selectByOrderNo(orderNo);
    }

    @Override
    public List<TransferOrderDO> getTransferOrderList(TransferOrderPageReqVO exportReqVO) {
        return transferOrderMapper.selectList(exportReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderAmount(Long id, String finalAmount) {
        validateTransferOrderExists(id);
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setFinalAmount(new java.math.BigDecimal(finalAmount));
        transferOrderMapper.updateById(updateObj);
        
        log.info("[updateOrderAmount][订单({})最终金额更新为：{}]", id, finalAmount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePaymentStatus(Long id, Integer paymentStatus) {
        validateTransferOrderExists(id);
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setPaymentStatus(paymentStatus);
        if (paymentStatus == 1) { // 1为已付款
            updateObj.setPaymentCompletedTime(LocalDateTime.now());
        }
        transferOrderMapper.updateById(updateObj);
        
        log.info("[updatePaymentStatus][订单({})付款状态更新为：{}]", id, paymentStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaymentStatus(Long id, Integer paymentStatus, String operator, String reason) {
        validateTransferOrderExists(id);
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setPaymentStatus(paymentStatus);
        if (paymentStatus == 1) { // 1为已付款
            updateObj.setPaymentCompletedTime(LocalDateTime.now());
        }
        transferOrderMapper.updateById(updateObj);
        
        log.info("[updatePaymentStatus][订单({})付款状态更新为：{}，操作人：{}，原因：{}]", id, paymentStatus, operator, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBusinessStatus(Long id, Integer businessStatus, String operator, String reason) {
        validateTransferOrderExists(id);
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setBusinessStatus(businessStatus);
        transferOrderMapper.updateById(updateObj);
        
        log.info("[updateBusinessStatus][订单({})业务状态更新为：{}，操作人：{}，原因：{}]", id, businessStatus, operator, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePickupConfirmation(Long id, String confirmedBy, String location) {
        validateTransferOrderExists(id);
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setPickupConfirmedTime(LocalDateTime.now());
        updateObj.setPickupConfirmedBy(confirmedBy);
        updateObj.setPickupConfirmedLocation(location);
        transferOrderMapper.updateById(updateObj);
        
        log.info("[updatePickupConfirmation][订单({})收货确认完成，确认人：{}]", id, confirmedBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        for (Long id : ids) {
            validateTransferOrderExists(id);
        }
        
        transferOrderMapper.batchUpdateStatus(ids, status);
        log.info("[batchUpdateStatus][批量更新订单状态，订单数量：{}，状态：{}]", ids.size(), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeAllocation(Long id, BigDecimal allocatedQuantity, BigDecimal allocationRatio, BigDecimal finalAmount) {
        validateTransferOrderExists(id);
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setAllocatedQuantity(allocatedQuantity);
        updateObj.setAllocationRatio(allocationRatio);
        updateObj.setFinalAmount(finalAmount);
        updateObj.setAllocationCompletedTime(LocalDateTime.now());
        transferOrderMapper.updateById(updateObj);
        
        log.info("[completeAllocation][完成订单分摊] id={}, allocatedQuantity={}, allocationRatio={}, finalAmount={}", 
                id, allocatedQuantity, allocationRatio, finalAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkVehicleWeighing(Long id, Long vehicleWeighingId) {
        validateTransferOrderExists(id);
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setVehicleWeighingId(vehicleWeighingId);
        transferOrderMapper.updateById(updateObj);
        
        log.info("[linkVehicleWeighing][关联车辆过磅] id={}, vehicleWeighingId={}", id, vehicleWeighingId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkTransportTask(Long id, Long transportTaskId) {
        validateTransferOrderExists(id);
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setTransportTaskId(transportTaskId);
        transferOrderMapper.updateById(updateObj);
        
        log.info("[linkTransportTask][关联运输任务] id={}, transportTaskId={}", id, transportTaskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPickup(Long id, String confirmedBy, String location) {
        validateTransferOrderExists(id);
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setPickupConfirmedTime(LocalDateTime.now());
        updateObj.setPickupConfirmedBy(confirmedBy);
        updateObj.setPickupConfirmedLocation(location);
        transferOrderMapper.updateById(updateObj);
        
        log.info("[confirmPickup][确认收货] id={}, confirmedBy={}, location={}", id, confirmedBy, location);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrderFromAppointment(Long appointmentId, Long quotationId) {
        // TODO: 实现从预约单创建订单的逻辑
        log.info("[createOrderFromAppointment][从预约单创建订单] appointmentId={}, quotationId={}", appointmentId, quotationId);
        // 暂时返回null，具体实现需要根据业务逻辑
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTransferOrder(Long id, String completeReason) {
        TransferOrderDO order = validateTransferOrderExists(id);
        if (order.getBusinessStatus() != 3) { // 3为已结算状态
            throw exception(TRANSFER_ORDER_STATUS_ERROR);
        }
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setBusinessStatus(4); // 4为已完成状态
        transferOrderMapper.updateById(updateObj);
        
        log.info("[completeTransferOrder][订单({})完成，原因：{}]", id, completeReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTransferOrder(Long id, String cancelReason) {
        TransferOrderDO order = validateTransferOrderExists(id);
        if (order.getBusinessStatus() == 5) { // 5为已取消状态
            throw exception(TRANSFER_ORDER_ALREADY_CANCELLED);
        }
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setBusinessStatus(5); // 5为已取消状态
        transferOrderMapper.updateById(updateObj);
        
        log.info("[cancelTransferOrder][订单({})取消，原因：{}]", id, cancelReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmTransferOrder(Long id, BigDecimal confirmedQuantity, String confirmReason) {
        TransferOrderDO order = validateTransferOrderExists(id);
        if (order.getBusinessStatus() != 0) { // 0为待确认状态
            throw exception(TRANSFER_ORDER_STATUS_ERROR);
        }
        
        TransferOrderDO updateObj = new TransferOrderDO();
        updateObj.setId(id);
        updateObj.setBusinessStatus(1); // 1为已确认状态
        updateObj.setConfirmedQuantity(confirmedQuantity);
        transferOrderMapper.updateById(updateObj);
        
        log.info("[confirmTransferOrder][订单({})确认，确认数量：{}，原因：{}]", id, confirmedQuantity, confirmReason);
    }

    @Override
    public TransferOrderRespVO getTransferOrderDetail(Long id) {
        TransferOrderDO transferOrder = validateTransferOrderExists(id);
        return BeanUtils.toBean(transferOrder, TransferOrderRespVO.class);
    }

    // 辅助方法
    @Override
    public TransferOrderDO validateTransferOrderExists(Long id) {
        TransferOrderDO order = transferOrderMapper.selectById(id);
        if (order == null) {
            throw exception(TRANSFER_ORDER_NOT_EXISTS);
        }
        return order;
    }
} 