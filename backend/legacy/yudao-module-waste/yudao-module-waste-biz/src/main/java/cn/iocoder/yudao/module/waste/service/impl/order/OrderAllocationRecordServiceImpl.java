package cn.iocoder.yudao.module.waste.service.impl.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderAllocationRecordCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderAllocationRecordPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderAllocationRecordRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderAllocationRecordDO;
import cn.iocoder.yudao.module.waste.dal.mysql.order.OrderAllocationRecordMapper;
import cn.iocoder.yudao.module.waste.service.order.OrderAllocationRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.ORDER_ALLOCATION_RECORD_NOT_EXISTS;

/**
 * 订单过磅分摊记录 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class OrderAllocationRecordServiceImpl implements OrderAllocationRecordService {

    @Resource
    private OrderAllocationRecordMapper orderAllocationRecordMapper;

    @Override
    public Long createOrderAllocationRecord(@Valid OrderAllocationRecordCreateReqVO createReqVO) {
        // 插入
        OrderAllocationRecordDO orderAllocationRecord = BeanUtils.toBean(createReqVO, OrderAllocationRecordDO.class);
        orderAllocationRecordMapper.insert(orderAllocationRecord);
        // 返回
        return orderAllocationRecord.getId();
    }

    @Override
    public void deleteOrderAllocationRecord(Long id) {
        // 校验存在
        validateOrderAllocationRecordExists(id);
        // 删除
        orderAllocationRecordMapper.deleteById(id);
    }

    @Override
    public OrderAllocationRecordDO getOrderAllocationRecord(Long id) {
        return orderAllocationRecordMapper.selectById(id);
    }

    @Override
    public OrderAllocationRecordRespVO getOrderAllocationRecordDetail(Long id) {
        OrderAllocationRecordDO orderAllocationRecord = validateOrderAllocationRecordExists(id);
        return BeanUtils.toBean(orderAllocationRecord, OrderAllocationRecordRespVO.class);
    }

    @Override
    public PageResult<OrderAllocationRecordDO> getOrderAllocationRecordPage(OrderAllocationRecordPageReqVO pageReqVO) {
        PageResult<OrderAllocationRecordDO> pageResult = orderAllocationRecordMapper.selectPage(pageReqVO);
        return pageResult;
    }

    @Override
    public List<OrderAllocationRecordDO> getOrderAllocationRecordList(OrderAllocationRecordPageReqVO exportReqVO) {
        return orderAllocationRecordMapper.selectList(exportReqVO);
    }

    @Override
    public List<OrderAllocationRecordDO> getOrderAllocationRecordListByOrderId(Long orderId) {
        return orderAllocationRecordMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<OrderAllocationRecordDO> getOrderAllocationRecordListByVehicleWeighingId(Long vehicleWeighingId) {
        return orderAllocationRecordMapper.selectListByVehicleWeighingId(vehicleWeighingId);
    }

    @Override
    public List<OrderAllocationRecordDO> getOrderAllocationRecordListByAllocationMethod(Integer allocationMethod) {
        return orderAllocationRecordMapper.selectListByAllocationMethod(allocationMethod);
    }

    @Override
    public List<OrderAllocationRecordDO> getSystemAllocationRecords() {
        return orderAllocationRecordMapper.selectSystemAllocationRecords();
    }

    @Override
    public List<OrderAllocationRecordDO> getManualAdjustmentRecords() {
        return orderAllocationRecordMapper.selectManualAdjustmentRecords();
    }

    @Override
    public List<OrderAllocationRecordDO> getOrderAllocationRecordListByAllocationTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return orderAllocationRecordMapper.selectListByAllocationTimeRange(startTime, endTime);
    }

    @Override
    public OrderAllocationRecordDO getLatestOrderAllocationRecordByOrderId(Long orderId) {
        return orderAllocationRecordMapper.selectLatestByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAllocationRecord(Long orderId, Long vehicleWeighingId, Integer allocationMethod,
                                      BigDecimal allocationRatio, BigDecimal allocatedQuantity, BigDecimal allocatedAmount,
                                      Boolean isManualAdjustment, String adjustmentReason) {
        OrderAllocationRecordDO record = new OrderAllocationRecordDO();
        record.setOrderId(orderId);
        record.setVehicleWeighingId(vehicleWeighingId);
        record.setAllocationMethod(allocationMethod);
        record.setAllocationRatio(allocationRatio);
        record.setAllocatedQuantity(allocatedQuantity);
        record.setAllocatedAmount(allocatedAmount);
        record.setIsManualAdjustment(isManualAdjustment);
        record.setAdjustmentReason(adjustmentReason);
        record.setAllocationTime(LocalDateTime.now());
        
        orderAllocationRecordMapper.insert(record);
        log.info("[createAllocationRecord][创建分摊记录成功] orderId={}, vehicleWeighingId={}, allocatedAmount={}", 
                orderId, vehicleWeighingId, allocatedAmount);
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> executeAutoAllocation(Long vehicleWeighingId, Integer allocationMethod) {
        // TODO: 实现自动分摊逻辑
        // 1. 获取该过磅记录关联的所有订单
        // 2. 根据分摊方法计算每个订单的分摊比例和金额
        // 3. 创建分摊记录
        log.info("[executeAutoAllocation][执行自动分摊] vehicleWeighingId={}, allocationMethod={}", 
                vehicleWeighingId, allocationMethod);
        // 暂时返回空列表，具体实现需要根据业务逻辑
        return Collections.emptyList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long executeManualAdjustment(Long orderId, Long vehicleWeighingId, BigDecimal adjustedRatio,
                                       BigDecimal adjustedQuantity, BigDecimal adjustedAmount, String adjustmentReason) {
        return createAllocationRecord(orderId, vehicleWeighingId, null, adjustedRatio, 
                                    adjustedQuantity, adjustedAmount, true, adjustmentReason);
    }

    @Override
    public OrderAllocationRecordDO validateOrderAllocationRecordExists(Long id) {
        OrderAllocationRecordDO orderAllocationRecord = orderAllocationRecordMapper.selectById(id);
        if (orderAllocationRecord == null) {
            throw exception(ORDER_ALLOCATION_RECORD_NOT_EXISTS);
        }
        return orderAllocationRecord;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeAutoAllocation(Long orderId, Long weighingRecordId) {
        // TODO: 实现自动分摊逻辑
        log.info("[executeAutoAllocation][执行自动分摊] orderId={}, weighingRecordId={}", orderId, weighingRecordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualAdjustment(Long id, String adjustedQuantity, String adjustedAmount, String adjustmentReason) {
        // 校验存在
        validateOrderAllocationRecordExists(id);
        
        // TODO: 实现人工调整逻辑
        log.info("[manualAdjustment][人工调整分摊] id={}, adjustedQuantity={}, adjustedAmount={}, reason={}", 
                id, adjustedQuantity, adjustedAmount, adjustmentReason);
    }

    @Override
    public List<OrderAllocationRecordDO> getRecordsByOrderId(Long orderId) {
        return orderAllocationRecordMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<OrderAllocationRecordDO> getRecordsByWeighingRecordId(Long weighingRecordId) {
        return orderAllocationRecordMapper.selectListByVehicleWeighingId(weighingRecordId);
    }

}