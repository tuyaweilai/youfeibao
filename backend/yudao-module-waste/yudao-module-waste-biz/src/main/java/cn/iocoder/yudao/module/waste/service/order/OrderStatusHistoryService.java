package cn.iocoder.yudao.module.waste.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderStatusHistoryCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderStatusHistoryPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderStatusHistoryRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderStatusHistoryDO;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单状态变更历史 Service 接口
 *
 * @author 芋道源码
 */
public interface OrderStatusHistoryService {

    /**
     * 创建订单状态变更历史
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createOrderStatusHistory(@Valid OrderStatusHistoryCreateReqVO createReqVO);

    /**
     * 删除订单状态变更历史
     *
     * @param id 编号
     */
    void deleteOrderStatusHistory(Long id);

    /**
     * 获得订单状态变更历史
     *
     * @param id 编号
     * @return 订单状态变更历史
     */
    OrderStatusHistoryDO getOrderStatusHistory(Long id);

    /**
     * 获得订单状态变更历史详情
     *
     * @param id 编号
     * @return 订单状态变更历史详情
     */
    OrderStatusHistoryRespVO getOrderStatusHistoryDetail(Long id);

    /**
     * 获得订单状态变更历史分页
     *
     * @param pageReqVO 分页查询
     * @return 订单状态变更历史分页
     */
    PageResult<OrderStatusHistoryDO> getOrderStatusHistoryPage(OrderStatusHistoryPageReqVO pageReqVO);

    /**
     * 获得订单状态变更历史列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 订单状态变更历史列表
     */
    List<OrderStatusHistoryDO> getOrderStatusHistoryList(OrderStatusHistoryPageReqVO exportReqVO);

    /**
     * 根据订单ID获得状态变更历史列表
     *
     * @param orderId 订单ID
     * @return 状态变更历史列表
     */
    List<OrderStatusHistoryDO> getOrderStatusHistoryListByOrderId(Long orderId);

    /**
     * 根据状态获得状态变更历史列表
     *
     * @param status 状态
     * @return 状态变更历史列表
     */
    List<OrderStatusHistoryDO> getOrderStatusHistoryListByStatus(Integer status);

    /**
     * 根据操作者获得状态变更历史列表
     *
     * @param operator 操作者
     * @return 状态变更历史列表
     */
    List<OrderStatusHistoryDO> getOrderStatusHistoryListByOperator(String operator);

    /**
     * 根据操作时间范围获得状态变更历史列表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 状态变更历史列表
     */
    List<OrderStatusHistoryDO> getOrderStatusHistoryListByOperationTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获得里程碑状态变更历史列表
     *
     * @return 里程碑状态变更历史列表
     */
    List<OrderStatusHistoryDO> getMilestoneOrderStatusHistories();

    /**
     * 获得订单的最新状态变更历史
     *
     * @param orderId 订单ID
     * @return 最新状态变更历史
     */
    OrderStatusHistoryDO getLatestOrderStatusHistoryByOrderId(Long orderId);

    /**
     * 获得订单的第一条状态变更历史
     *
     * @param orderId 订单ID
     * @return 第一条状态变更历史
     */
    OrderStatusHistoryDO getFirstOrderStatusHistoryByOrderId(Long orderId);

    /**
     * 记录订单状态变更
     *
     * @param orderId 订单ID
     * @param status 状态
     * @param statusName 状态名称
     * @param operator 操作者
     * @param operationTime 操作时间
     * @param remark 备注
     * @param isMilestone 是否里程碑
     * @param businessData 业务数据
     * @return 状态变更历史ID
     */
    Long recordOrderStatusChange(Long orderId, Integer status, String statusName, String operator,
                                LocalDateTime operationTime, String remark, Boolean isMilestone, String businessData);

    /**
     * 记录里程碑状态变更
     *
     * @param orderId 订单ID
     * @param status 状态
     * @param statusName 状态名称
     * @param operator 操作者
     * @param remark 备注
     * @return 状态变更历史ID
     */
    Long recordMilestoneStatusChange(Long orderId, Integer status, String statusName, String operator, String remark);

    /**
     * 校验状态变更历史是否存在
     *
     * @param id 状态变更历史ID
     * @return 状态变更历史信息
     */
    OrderStatusHistoryDO validateOrderStatusHistoryExists(Long id);

    /**
     * 记录状态变更
     *
     * @param orderId 订单ID
     * @param fromStatus 原状态
     * @param toStatus 新状态
     * @param changeReason 变更原因
     */
    void recordStatusChange(Long orderId, Integer fromStatus, Integer toStatus, String changeReason);

    /**
     * 记录里程碑状态
     *
     * @param orderId 订单ID
     * @param status 状态
     * @param milestone 里程碑描述
     */
    void recordMilestone(Long orderId, Integer status, String milestone);

    /**
     * 根据订单ID获取状态变更历史
     *
     * @param orderId 订单ID
     * @return 状态变更历史列表
     */
    List<OrderStatusHistoryDO> getHistoryByOrderId(Long orderId);

    /**
     * 获取订单里程碑状态
     *
     * @param orderId 订单ID
     * @return 里程碑状态列表
     */
    List<OrderStatusHistoryDO> getMilestonesByOrderId(Long orderId);

} 