package cn.iocoder.yudao.module.waste.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderPriceAdjustmentCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderPriceAdjustmentPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderPriceAdjustmentRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderPriceAdjustmentDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单价格调整记录 Service 接口
 *
 * @author 芋道源码
 */
public interface OrderPriceAdjustmentService {

    /**
     * 创建订单价格调整记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createOrderPriceAdjustment(@Valid OrderPriceAdjustmentCreateReqVO createReqVO);

    /**
     * 删除订单价格调整记录
     *
     * @param id 编号
     */
    void deleteOrderPriceAdjustment(Long id);

    /**
     * 获得订单价格调整记录
     *
     * @param id 编号
     * @return 订单价格调整记录
     */
    OrderPriceAdjustmentDO getOrderPriceAdjustment(Long id);

    /**
     * 获得订单价格调整记录详情
     *
     * @param id 编号
     * @return 订单价格调整记录详情
     */
    OrderPriceAdjustmentRespVO getOrderPriceAdjustmentDetail(Long id);

    /**
     * 获得订单价格调整记录分页
     *
     * @param pageReqVO 分页查询
     * @return 订单价格调整记录分页
     */
    PageResult<OrderPriceAdjustmentDO> getOrderPriceAdjustmentPage(OrderPriceAdjustmentPageReqVO pageReqVO);

    /**
     * 获得订单价格调整记录列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 订单价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentList(OrderPriceAdjustmentPageReqVO exportReqVO);

    /**
     * 根据订单ID获得价格调整记录列表
     *
     * @param orderId 订单ID
     * @return 价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByOrderId(Long orderId);

    /**
     * 根据调整类型获得价格调整记录列表
     *
     * @param adjustmentType 调整类型
     * @return 价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByAdjustmentType(Integer adjustmentType);

    /**
     * 根据状态获得价格调整记录列表
     *
     * @param status 状态
     * @return 价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByStatus(Integer status);

    /**
     * 获得待确认的价格调整记录列表
     *
     * @return 待确认的价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getPendingOrderPriceAdjustments();

    /**
     * 获得已确认的价格调整记录列表
     *
     * @return 已确认的价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getConfirmedOrderPriceAdjustments();

    /**
     * 获得已拒绝的价格调整记录列表
     *
     * @return 已拒绝的价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getRejectedOrderPriceAdjustments();

    /**
     * 根据关联过磅记录ID获得价格调整记录列表
     *
     * @param relatedWeighingId 关联过磅记录ID
     * @return 价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByRelatedWeighingId(Long relatedWeighingId);

    /**
     * 根据确认人获得价格调整记录列表
     *
     * @param confirmedBy 确认人
     * @return 价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByConfirmedBy(String confirmedBy);

    /**
     * 根据确认时间范围获得价格调整记录列表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getOrderPriceAdjustmentListByConfirmedTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获得价格调整记录列表
     *
     * @return 价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getPriceAdjustments();

    /**
     * 获得数量调整记录列表
     *
     * @return 数量调整记录列表
     */
    List<OrderPriceAdjustmentDO> getQuantityAdjustments();

    /**
     * 获得正调整记录列表（增加金额）
     *
     * @return 正调整记录列表
     */
    List<OrderPriceAdjustmentDO> getPositiveAdjustments();

    /**
     * 获得负调整记录列表（减少金额）
     *
     * @return 负调整记录列表
     */
    List<OrderPriceAdjustmentDO> getNegativeAdjustments();

    /**
     * 获得订单的最新价格调整记录
     *
     * @param orderId 订单ID
     * @return 最新价格调整记录
     */
    OrderPriceAdjustmentDO getLatestOrderPriceAdjustmentByOrderId(Long orderId);

    /**
     * 获得订单的最新已确认价格调整记录
     *
     * @param orderId 订单ID
     * @return 最新已确认价格调整记录
     */
    OrderPriceAdjustmentDO getLatestConfirmedOrderPriceAdjustmentByOrderId(Long orderId);

    /**
     * 确认价格调整
     *
     * @param id 调整记录ID
     * @param confirmedBy 确认人
     */
    void confirmOrderPriceAdjustment(Long id, String confirmedBy);

    /**
     * 拒绝价格调整
     *
     * @param id 调整记录ID
     * @param confirmedBy 确认人
     * @param rejectReason 拒绝原因
     */
    void rejectOrderPriceAdjustment(Long id, String confirmedBy, String rejectReason);

    /**
     * 创建价格调整记录
     *
     * @param orderId 订单ID
     * @param adjustmentType 调整类型
     * @param originalPrice 原价格
     * @param adjustedPrice 调整后价格
     * @param originalQuantity 原数量
     * @param adjustedQuantity 调整后数量
     * @param adjustmentReason 调整原因
     * @param relatedWeighingId 关联过磅记录ID
     * @return 调整记录ID
     */
    Long createPriceAdjustment(Long orderId, Integer adjustmentType, BigDecimal originalPrice, 
                              BigDecimal adjustedPrice, BigDecimal originalQuantity, BigDecimal adjustedQuantity,
                              String adjustmentReason, Long relatedWeighingId);

    /**
     * 批量确认价格调整
     *
     * @param ids 调整记录ID列表
     * @param confirmedBy 确认人
     */
    void batchConfirmOrderPriceAdjustments(List<Long> ids, String confirmedBy);

    /**
     * 批量拒绝价格调整
     *
     * @param ids 调整记录ID列表
     * @param confirmedBy 确认人
     * @param rejectReason 拒绝原因
     */
    void batchRejectOrderPriceAdjustments(List<Long> ids, String confirmedBy, String rejectReason);

    /**
     * 校验价格调整记录是否存在
     *
     * @param id 调整记录ID
     * @return 价格调整记录信息
     */
    OrderPriceAdjustmentDO validateOrderPriceAdjustmentExists(Long id);

    /**
     * 申请价格调整
     *
     * @param orderId 订单ID
     * @param adjustmentType 调整类型
     * @param adjustmentAmount 调整金额
     * @param adjustmentReason 调整原因
     */
    void applyPriceAdjustment(Long orderId, Integer adjustmentType, BigDecimal adjustmentAmount, String adjustmentReason);

    /**
     * 审批价格调整
     *
     * @param id 调整记录ID
     * @param approved 是否批准
     * @param approvalReason 审批原因
     */
    void approveAdjustment(Long id, Boolean approved, String approvalReason);

    /**
     * 执行价格调整
     *
     * @param id 调整记录ID
     */
    void executeAdjustment(Long id);

    /**
     * 根据订单ID获取价格调整记录
     *
     * @param orderId 订单ID
     * @return 价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getAdjustmentsByOrderId(Long orderId);

    /**
     * 获取待审批的价格调整记录
     *
     * @return 待审批的价格调整记录列表
     */
    List<OrderPriceAdjustmentDO> getPendingApprovalAdjustments();

} 