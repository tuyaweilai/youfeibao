package cn.iocoder.yudao.module.logistics.service.temporaryorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.temporaryorder.vo.*;
import cn.iocoder.yudao.module.logistics.convert.temporaryorder.TemporaryOrderConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.temporaryorder.TemporaryOrderDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.temporaryorder.TemporaryOrderMapper;
import cn.iocoder.yudao.module.logistics.enums.TemporaryOrderPaymentStatusEnum;
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
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 物流临时订单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class TemporaryOrderServiceImpl implements TemporaryOrderService {

    @Resource
    private TemporaryOrderMapper temporaryOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemporaryOrder(@Valid TemporaryOrderCreateReqVO createReqVO) {
        // 校验订单编号唯一性
        validateOrderNoUnique(null, createReqVO.getOrderNo());
        
        // 插入
        TemporaryOrderDO temporaryOrder = TemporaryOrderConvert.INSTANCE.convert(createReqVO);
        // 设置默认支付状态
        if (temporaryOrder.getPaymentStatus() == null) {
            temporaryOrder.setPaymentStatus(TemporaryOrderPaymentStatusEnum.UNPAID.getStatus());
        }
        // 设置默认转换状态
        temporaryOrder.setConvertedToFormal(false);
        
        temporaryOrderMapper.insert(temporaryOrder);
        
        log.info("[createTemporaryOrder][创建临时订单成功，订单编号：{}，司机：{}]", 
                createReqVO.getOrderNo(), createReqVO.getDriverName());
        
        // 返回
        return temporaryOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemporaryOrder(@Valid TemporaryOrderUpdateReqVO updateReqVO) {
        // 校验存在
        validateTemporaryOrderExists(updateReqVO.getId());
        // 校验订单编号唯一性
        validateOrderNoUnique(updateReqVO.getId(), updateReqVO.getOrderNo());
        
        // 更新
        TemporaryOrderDO updateObj = TemporaryOrderConvert.INSTANCE.convert(updateReqVO);
        temporaryOrderMapper.updateById(updateObj);
        
        log.info("[updateTemporaryOrder][更新临时订单成功，ID：{}]", updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemporaryOrder(Long id) {
        // 校验存在
        TemporaryOrderDO temporaryOrder = validateTemporaryOrderExists(id);
        
        // 校验是否可以删除
        if (Boolean.TRUE.equals(temporaryOrder.getConvertedToFormal())) {
            throw exception(TEMPORARY_ORDER_CANNOT_DELETE_CONVERTED);
        }
        if (TemporaryOrderPaymentStatusEnum.PAID.getStatus().equals(temporaryOrder.getPaymentStatus())) {
            throw exception(TEMPORARY_ORDER_CANNOT_DELETE_PAID);
        }
        
        // 删除
        temporaryOrderMapper.deleteById(id);
        
        log.info("[deleteTemporaryOrder][删除临时订单成功，ID：{}]", id);
    }

    private void validateOrderNoUnique(Long id, String orderNo) {
        TemporaryOrderDO temporaryOrder = temporaryOrderMapper.selectByOrderNo(orderNo);
        if (temporaryOrder == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的订单
        if (id == null) {
            throw exception(TEMPORARY_ORDER_ORDER_NO_DUPLICATE);
        }
        if (!temporaryOrder.getId().equals(id)) {
            throw exception(TEMPORARY_ORDER_ORDER_NO_DUPLICATE);
        }
    }

    @Override
    public TemporaryOrderDO getTemporaryOrder(Long id) {
        return temporaryOrderMapper.selectById(id);
    }

    @Override
    public TemporaryOrderRespVO getTemporaryOrderDetail(Long id) {
        TemporaryOrderDO temporaryOrder = getTemporaryOrder(id);
        return TemporaryOrderConvert.INSTANCE.convert(temporaryOrder);
    }

    @Override
    public PageResult<TemporaryOrderRespVO> getTemporaryOrderPage(TemporaryOrderPageReqVO pageReqVO) {
        PageResult<TemporaryOrderDO> pageResult = temporaryOrderMapper.selectPage(pageReqVO);
        return TemporaryOrderConvert.INSTANCE.convertPage(pageResult);
    }

    @Override
    public List<TemporaryOrderDO> getTemporaryOrderList(TemporaryOrderPageReqVO exportReqVO) {
        return temporaryOrderMapper.selectList(exportReqVO);
    }

    @Override
    public TemporaryOrderDO getTemporaryOrderByOrderNo(String orderNo) {
        return temporaryOrderMapper.selectByOrderNo(orderNo);
    }

    @Override
    public List<TemporaryOrderDO> getTemporaryOrderListByTaskId(Long taskId) {
        return temporaryOrderMapper.selectByTaskId(taskId);
    }

    @Override
    public List<TemporaryOrderDO> getTemporaryOrderListByDriverId(Long driverId) {
        return temporaryOrderMapper.selectByDriverId(driverId);
    }

    @Override
    public List<TemporaryOrderDO> getTemporaryOrderListByPaymentStatus(Integer paymentStatus) {
        return temporaryOrderMapper.selectByPaymentStatus(paymentStatus);
    }

    @Override
    public List<TemporaryOrderDO> getTemporaryOrderListByConvertedToFormal(Boolean convertedToFormal) {
        return temporaryOrderMapper.selectByConvertedToFormal(convertedToFormal);
    }

    @Override
    public List<TemporaryOrderDO> getTemporaryOrderListByWasteType(String wasteType) {
        return temporaryOrderMapper.selectByWasteType(wasteType);
    }

    @Override
    public Long getTemporaryOrderCountByTaskId(Long taskId) {
        return temporaryOrderMapper.selectCountByTaskId(taskId);
    }

    @Override
    public Long getTemporaryOrderCountByDriverId(Long driverId) {
        return temporaryOrderMapper.selectCountByDriverId(driverId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payTemporaryOrder(Long id, BigDecimal paymentAmount, String paymentMethod, String paymentVoucherUrl) {
        // 校验存在
        TemporaryOrderDO temporaryOrder = validateTemporaryOrderExists(id);
        
        // 校验状态
        if (!TemporaryOrderPaymentStatusEnum.UNPAID.getStatus().equals(temporaryOrder.getPaymentStatus())) {
            throw exception(TEMPORARY_ORDER_PAYMENT_STATUS_NOT_UNPAID);
        }
        
        // 更新支付信息
        TemporaryOrderDO updateObj = new TemporaryOrderDO();
        updateObj.setId(id);
        updateObj.setPaymentStatus(TemporaryOrderPaymentStatusEnum.PAID.getStatus());
        updateObj.setPaymentAmount(paymentAmount);
        updateObj.setPaymentTime(LocalDateTime.now());
        updateObj.setPaymentMethod(paymentMethod);
        updateObj.setPaymentVoucherUrl(paymentVoucherUrl);
        
        temporaryOrderMapper.updateById(updateObj);
        
        log.info("[payTemporaryOrder][临时订单支付成功，ID：{}，金额：{}]", id, paymentAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundTemporaryOrder(Long id, String refundReason) {
        // 校验存在
        TemporaryOrderDO temporaryOrder = validateTemporaryOrderExists(id);
        
        // 校验状态
        if (!TemporaryOrderPaymentStatusEnum.PAID.getStatus().equals(temporaryOrder.getPaymentStatus())) {
            throw exception(TEMPORARY_ORDER_PAYMENT_STATUS_NOT_PAID);
        }
        
        // 更新退款状态
        TemporaryOrderDO updateObj = new TemporaryOrderDO();
        updateObj.setId(id);
        updateObj.setPaymentStatus(TemporaryOrderPaymentStatusEnum.REFUNDED.getStatus());
        updateObj.setRemark(temporaryOrder.getRemark() + "；退款原因：" + refundReason);
        
        temporaryOrderMapper.updateById(updateObj);
        
        log.info("[refundTemporaryOrder][临时订单退款成功，ID：{}，原因：{}]", id, refundReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTemporaryOrder(Long id, String cancelReason) {
        // 校验存在
        TemporaryOrderDO temporaryOrder = validateTemporaryOrderExists(id);
        
        // 校验状态
        if (Boolean.TRUE.equals(temporaryOrder.getConvertedToFormal())) {
            throw exception(TEMPORARY_ORDER_CANNOT_CANCEL_CONVERTED);
        }
        
        // 更新取消状态
        TemporaryOrderDO updateObj = new TemporaryOrderDO();
        updateObj.setId(id);
        updateObj.setPaymentStatus(TemporaryOrderPaymentStatusEnum.CANCELLED.getStatus());
        updateObj.setRemark(temporaryOrder.getRemark() + "；取消原因：" + cancelReason);
        
        temporaryOrderMapper.updateById(updateObj);
        
        log.info("[cancelTemporaryOrder][临时订单取消成功，ID：{}，原因：{}]", id, cancelReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void convertToFormalOrder(Long id, Long formalOrderId) {
        // 校验存在
        TemporaryOrderDO temporaryOrder = validateTemporaryOrderExists(id);
        
        // 校验状态
        if (Boolean.TRUE.equals(temporaryOrder.getConvertedToFormal())) {
            throw exception(TEMPORARY_ORDER_ALREADY_CONVERTED);
        }
        
        // 更新转换状态
        TemporaryOrderDO updateObj = new TemporaryOrderDO();
        updateObj.setId(id);
        updateObj.setConvertedToFormal(true);
        updateObj.setFormalOrderId(formalOrderId);
        updateObj.setConversionTime(LocalDateTime.now());
        
        temporaryOrderMapper.updateById(updateObj);
        
        log.info("[convertToFormalOrder][临时订单转为正式订单成功，临时订单ID：{}，正式订单ID：{}]", id, formalOrderId);
    }

    @Override
    public TemporaryOrderDO validateTemporaryOrderExists(Long id) {
        TemporaryOrderDO temporaryOrder = temporaryOrderMapper.selectById(id);
        if (temporaryOrder == null) {
            throw exception(TEMPORARY_ORDER_NOT_EXISTS);
        }
        return temporaryOrder;
    }

} 