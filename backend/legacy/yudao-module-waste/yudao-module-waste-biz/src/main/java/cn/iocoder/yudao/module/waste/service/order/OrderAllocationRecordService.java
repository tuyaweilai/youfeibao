package cn.iocoder.yudao.module.waste.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderAllocationRecordCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderAllocationRecordPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderAllocationRecordRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderAllocationRecordDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单过磅分摊记录 Service 接口
 *
 * @author 芋道源码
 */
public interface OrderAllocationRecordService {

    /**
     * 创建订单过磅分摊记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createOrderAllocationRecord(@Valid OrderAllocationRecordCreateReqVO createReqVO);

    /**
     * 删除订单过磅分摊记录
     *
     * @param id 编号
     */
    void deleteOrderAllocationRecord(Long id);

    /**
     * 获得订单过磅分摊记录
     *
     * @param id 编号
     * @return 订单过磅分摊记录
     */
    OrderAllocationRecordDO getOrderAllocationRecord(Long id);

    /**
     * 获得订单过磅分摊记录详情
     *
     * @param id 编号
     * @return 订单过磅分摊记录详情
     */
    OrderAllocationRecordRespVO getOrderAllocationRecordDetail(Long id);

    /**
     * 获得订单过磅分摊记录分页
     *
     * @param pageReqVO 分页查询
     * @return 订单过磅分摊记录分页
     */
    PageResult<OrderAllocationRecordDO> getOrderAllocationRecordPage(OrderAllocationRecordPageReqVO pageReqVO);

    /**
     * 获得订单过磅分摊记录列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 订单过磅分摊记录列表
     */
    List<OrderAllocationRecordDO> getOrderAllocationRecordList(OrderAllocationRecordPageReqVO exportReqVO);

    /**
     * 根据订单ID获得分摊记录列表
     *
     * @param orderId 订单ID
     * @return 分摊记录列表
     */
    List<OrderAllocationRecordDO> getOrderAllocationRecordListByOrderId(Long orderId);

    /**
     * 根据车辆过磅ID获得分摊记录列表
     *
     * @param vehicleWeighingId 车辆过磅ID
     * @return 分摊记录列表
     */
    List<OrderAllocationRecordDO> getOrderAllocationRecordListByVehicleWeighingId(Long vehicleWeighingId);

    /**
     * 根据分摊方法获得分摊记录列表
     *
     * @param allocationMethod 分摊方法
     * @return 分摊记录列表
     */
    List<OrderAllocationRecordDO> getOrderAllocationRecordListByAllocationMethod(Integer allocationMethod);

    /**
     * 获得系统自动分摊记录列表
     *
     * @return 系统自动分摊记录列表
     */
    List<OrderAllocationRecordDO> getSystemAllocationRecords();

    /**
     * 获得人工调整分摊记录列表
     *
     * @return 人工调整分摊记录列表
     */
    List<OrderAllocationRecordDO> getManualAdjustmentRecords();

    /**
     * 根据分摊时间范围获得分摊记录列表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分摊记录列表
     */
    List<OrderAllocationRecordDO> getOrderAllocationRecordListByAllocationTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获得订单的最新分摊记录
     *
     * @param orderId 订单ID
     * @return 最新分摊记录
     */
    OrderAllocationRecordDO getLatestOrderAllocationRecordByOrderId(Long orderId);

    /**
     * 创建分摊记录
     *
     * @param orderId 订单ID
     * @param vehicleWeighingId 车辆过磅ID
     * @param allocationMethod 分摊方法
     * @param allocationRatio 分摊比例
     * @param allocatedQuantity 分摊数量
     * @param allocatedAmount 分摊金额
     * @param isManualAdjustment 是否人工调整
     * @param adjustmentReason 调整原因
     * @return 分摊记录ID
     */
    Long createAllocationRecord(Long orderId, Long vehicleWeighingId, Integer allocationMethod,
                               BigDecimal allocationRatio, BigDecimal allocatedQuantity, BigDecimal allocatedAmount,
                               Boolean isManualAdjustment, String adjustmentReason);

    /**
     * 执行自动分摊
     *
     * @param vehicleWeighingId 车辆过磅ID
     * @param allocationMethod 分摊方法
     * @return 分摊记录ID列表
     */
    List<Long> executeAutoAllocation(Long vehicleWeighingId, Integer allocationMethod);

    /**
     * 执行人工调整分摊
     *
     * @param orderId 订单ID
     * @param vehicleWeighingId 车辆过磅ID
     * @param adjustedRatio 调整后比例
     * @param adjustedQuantity 调整后数量
     * @param adjustedAmount 调整后金额
     * @param adjustmentReason 调整原因
     * @return 分摊记录ID
     */
    Long executeManualAdjustment(Long orderId, Long vehicleWeighingId, BigDecimal adjustedRatio,
                                BigDecimal adjustedQuantity, BigDecimal adjustedAmount, String adjustmentReason);

    /**
     * 校验分摊记录是否存在
     *
     * @param id 分摊记录ID
     * @return 分摊记录信息
     */
    OrderAllocationRecordDO validateOrderAllocationRecordExists(Long id);

    /**
     * 执行自动分摊
     *
     * @param orderId 订单ID
     * @param weighingRecordId 过磅记录ID
     */
    void executeAutoAllocation(Long orderId, Long weighingRecordId);

    /**
     * 人工调整分摊
     *
     * @param id 分摊记录ID
     * @param adjustedQuantity 调整后数量
     * @param adjustedAmount 调整后金额
     * @param adjustmentReason 调整原因
     */
    void manualAdjustment(Long id, String adjustedQuantity, String adjustedAmount, String adjustmentReason);

    /**
     * 根据订单ID获取分摊记录
     *
     * @param orderId 订单ID
     * @return 分摊记录列表
     */
    List<OrderAllocationRecordDO> getRecordsByOrderId(Long orderId);

    /**
     * 根据过磅记录ID获取分摊记录
     *
     * @param weighingRecordId 过磅记录ID
     * @return 分摊记录列表
     */
    List<OrderAllocationRecordDO> getRecordsByWeighingRecordId(Long weighingRecordId);

} 