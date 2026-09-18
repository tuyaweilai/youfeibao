package cn.iocoder.yudao.module.waste.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.TransferOrderDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 危废转移订单 Service 接口
 *
 * @author 芋道源码
 */
public interface TransferOrderService {

    /**
     * 创建危废转移订单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTransferOrder(@Valid TransferOrderCreateReqVO createReqVO);

    /**
     * 更新危废转移订单
     *
     * @param updateReqVO 更新信息
     */
    void updateTransferOrder(@Valid TransferOrderUpdateReqVO updateReqVO);

    /**
     * 删除危废转移订单
     *
     * @param id 编号
     */
    void deleteTransferOrder(Long id);

    /**
     * 获得危废转移订单
     *
     * @param id 编号
     * @return 危废转移订单
     */
    TransferOrderDO getTransferOrder(Long id);

    /**
     * 获得危废转移订单详情
     *
     * @param id 编号
     * @return 危废转移订单详情
     */
    TransferOrderRespVO getTransferOrderDetail(Long id);

    /**
     * 获得危废转移订单分页
     *
     * @param pageReqVO 分页查询
     * @return 危废转移订单分页
     */
    PageResult<TransferOrderDO> getTransferOrderPage(TransferOrderPageReqVO pageReqVO);

    /**
     * 获得危废转移订单列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 危废转移订单列表
     */
    List<TransferOrderDO> getTransferOrderList(TransferOrderPageReqVO exportReqVO);

    /**
     * 根据订单号获得订单信息
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    TransferOrderDO getTransferOrderByNo(String orderNo);

    /**
     * 根据预约单ID获得订单列表
     *
     * @param appointmentId 预约单ID
     * @return 订单列表
     */
    List<TransferOrderDO> getTransferOrderListByAppointmentId(Long appointmentId);

    /**
     * 根据产废企业ID获得订单列表
     *
     * @param producingEnterpriseId 产废企业ID
     * @return 订单列表
     */
    List<TransferOrderDO> getTransferOrderListByProducingEnterpriseId(Long producingEnterpriseId);

    /**
     * 根据回收企业ID获得订单列表
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 订单列表
     */
    List<TransferOrderDO> getTransferOrderListByRecyclingEnterpriseId(Long recyclingEnterpriseId);

    /**
     * 根据业务状态获得订单列表
     *
     * @param businessStatus 业务状态
     * @return 订单列表
     */
    List<TransferOrderDO> getTransferOrderListByBusinessStatus(Integer businessStatus);

    /**
     * 根据付款状态获得订单列表
     *
     * @param paymentStatus 付款状态
     * @return 订单列表
     */
    List<TransferOrderDO> getTransferOrderListByPaymentStatus(Integer paymentStatus);

    /**
     * 确认订单
     *
     * @param id 订单ID
     * @param confirmedQuantity 确认数量
     * @param confirmReason 确认原因
     */
    void confirmTransferOrder(Long id, BigDecimal confirmedQuantity, String confirmReason);

    /**
     * 取消订单
     *
     * @param id 订单ID
     * @param cancelReason 取消原因
     */
    void cancelTransferOrder(Long id, String cancelReason);

    /**
     * 完成订单
     *
     * @param id 订单ID
     * @param completeReason 完成原因
     */
    void completeTransferOrder(Long id, String completeReason);

    /**
     * 更新订单业务状态
     *
     * @param id 订单ID
     * @param businessStatus 业务状态
     * @param operator 操作人
     * @param reason 原因
     */
    void updateBusinessStatus(Long id, Integer businessStatus, String operator, String reason);

    /**
     * 更新订单付款状态
     *
     * @param id 订单ID
     * @param paymentStatus 付款状态
     * @param operator 操作人
     * @param reason 原因
     */
    void updatePaymentStatus(Long id, Integer paymentStatus, String operator, String reason);

    /**
     * 确认收货
     *
     * @param id 订单ID
     * @param confirmedBy 确认人
     * @param location GPS位置
     */
    void confirmPickup(Long id, String confirmedBy, String location);

    /**
     * 关联运输任务
     *
     * @param id 订单ID
     * @param transportTaskId 运输任务ID
     */
    void linkTransportTask(Long id, Long transportTaskId);

    /**
     * 关联车辆过磅
     *
     * @param id 订单ID
     * @param vehicleWeighingId 车辆过磅ID
     */
    void linkVehicleWeighing(Long id, Long vehicleWeighingId);

    /**
     * 完成订单分摊
     *
     * @param id 订单ID
     * @param allocatedQuantity 分摊数量
     * @param allocationRatio 分摊比例
     * @param finalAmount 最终金额
     */
    void completeAllocation(Long id, BigDecimal allocatedQuantity, BigDecimal allocationRatio, BigDecimal finalAmount);

    /**
     * 获取待分摊的订单列表
     *
     * @return 待分摊订单列表
     */
    List<TransferOrderDO> getUnallocatedOrders();

    /**
     * 获取待付款的订单列表
     *
     * @return 待付款订单列表
     */
    List<TransferOrderDO> getPendingPaymentOrders();

    /**
     * 根据预约单创建订单
     *
     * @param appointmentId 预约单ID
     * @param quotationId 报价记录ID
     * @return 订单ID
     */
    Long createOrderFromAppointment(Long appointmentId, Long quotationId);

    /**
     * 校验订单是否存在
     *
     * @param id 订单ID
     * @return 订单信息
     */
    TransferOrderDO validateTransferOrderExists(Long id);

    /**
     * 确认订单
     *
     * @param id 订单ID
     */
    void confirmOrder(Long id);

    /**
     * 取消订单
     *
     * @param id 订单ID
     * @param reason 取消原因
     */
    void cancelOrder(Long id, String reason);

    /**
     * 完成订单
     *
     * @param id 订单ID
     */
    void completeOrder(Long id);

    /**
     * 获取待分摊订单列表
     *
     * @return 待分摊订单列表
     */
    List<TransferOrderDO> getPendingAllocationOrders();

    /**
     * 根据产废企业ID获得订单列表
     *
     * @param producerEnterpriseId 产废企业ID
     * @return 订单列表
     */
    List<TransferOrderDO> getOrdersByProducerEnterprise(Long producerEnterpriseId);

    /**
     * 根据回收企业ID获得订单列表
     *
     * @param recyclerEnterpriseId 回收企业ID
     * @return 订单列表
     */
    List<TransferOrderDO> getOrdersByRecyclerEnterprise(Long recyclerEnterpriseId);

    /**
     * 根据业务状态获得订单列表
     *
     * @param status 业务状态
     * @return 订单列表
     */
    List<TransferOrderDO> getOrdersByStatus(Integer status);

    /**
     * 根据时间范围获得订单列表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单列表
     */
    List<TransferOrderDO> getOrdersByDateRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 更新订单最终金额
     *
     * @param id 订单ID
     * @param finalAmount 最终金额
     */
    void updateOrderAmount(Long id, String finalAmount);

    /**
     * 更新收货确认信息
     *
     * @param id 订单ID
     * @param confirmedBy 确认人
     * @param location GPS位置
     */
    void updatePickupConfirmation(Long id, String confirmedBy, String location);

    /**
     * 批量更新订单状态
     *
     * @param ids 订单ID列表
     * @param status 状态
     */
    void batchUpdateStatus(List<Long> ids, Integer status);

} 