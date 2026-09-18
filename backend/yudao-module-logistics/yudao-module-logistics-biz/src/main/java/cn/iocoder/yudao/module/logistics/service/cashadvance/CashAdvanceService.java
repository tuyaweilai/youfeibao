package cn.iocoder.yudao.module.logistics.service.cashadvance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.cashadvance.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.cashadvance.CashAdvanceDO;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流现金代付记录 Service 接口
 *
 * @author 芋道源码
 */
public interface CashAdvanceService {

    /**
     * 创建物流现金代付记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCashAdvance(@Valid CashAdvanceCreateReqVO createReqVO);

    /**
     * 更新物流现金代付记录
     *
     * @param updateReqVO 更新信息
     */
    void updateCashAdvance(@Valid CashAdvanceUpdateReqVO updateReqVO);

    /**
     * 删除物流现金代付记录
     *
     * @param id 编号
     */
    void deleteCashAdvance(Long id);

    /**
     * 获得物流现金代付记录
     *
     * @param id 编号
     * @return 物流现金代付记录
     */
    CashAdvanceDO getCashAdvance(Long id);

    /**
     * 获得物流现金代付记录详情
     *
     * @param id 编号
     * @return 物流现金代付记录详情
     */
    CashAdvanceRespVO getCashAdvanceDetail(Long id);

    /**
     * 获得物流现金代付记录分页
     *
     * @param pageReqVO 分页查询
     * @return 物流现金代付记录分页
     */
    PageResult<CashAdvanceRespVO> getCashAdvancePage(CashAdvancePageReqVO pageReqVO);

    /**
     * 获得物流现金代付记录列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 物流现金代付记录列表
     */
    List<CashAdvanceDO> getCashAdvanceList(CashAdvancePageReqVO exportReqVO);

    /**
     * 根据任务ID获得物流现金代付记录列表
     *
     * @param taskId 任务ID
     * @return 物流现金代付记录列表
     */
    List<CashAdvanceDO> getCashAdvanceListByTaskId(Long taskId);

    /**
     * 根据订单ID获得物流现金代付记录列表
     *
     * @param orderId 订单ID
     * @return 物流现金代付记录列表
     */
    List<CashAdvanceDO> getCashAdvanceListByOrderId(Long orderId);

    /**
     * 根据司机ID获得物流现金代付记录列表
     *
     * @param driverId 司机ID
     * @return 物流现金代付记录列表
     */
    List<CashAdvanceDO> getCashAdvanceListByDriverId(Long driverId);

    /**
     * 根据通知状态获得物流现金代付记录列表
     *
     * @param notifyStatus 通知状态
     * @return 物流现金代付记录列表
     */
    List<CashAdvanceDO> getCashAdvanceListByNotifyStatus(Integer notifyStatus);

    /**
     * 根据对账状态获得物流现金代付记录列表
     *
     * @param reconcileStatus 对账状态
     * @return 物流现金代付记录列表
     */
    List<CashAdvanceDO> getCashAdvanceListByReconcileStatus(Integer reconcileStatus);

    /**
     * 根据支付方式获得物流现金代付记录列表
     *
     * @param paymentMethod 支付方式
     * @return 物流现金代付记录列表
     */
    List<CashAdvanceDO> getCashAdvanceListByPaymentMethod(String paymentMethod);

    /**
     * 统计任务的现金代付记录数量
     *
     * @param taskId 任务ID
     * @return 现金代付记录数量
     */
    Long getCashAdvanceCountByTaskId(Long taskId);

    /**
     * 统计订单的现金代付记录数量
     *
     * @param orderId 订单ID
     * @return 现金代付记录数量
     */
    Long getCashAdvanceCountByOrderId(Long orderId);

    /**
     * 统计司机的现金代付记录数量
     *
     * @param driverId 司机ID
     * @return 现金代付记录数量
     */
    Long getCashAdvanceCountByDriverId(Long driverId);

    // ========== 业务方法 ==========

    /**
     * 通知现金代付记录
     *
     * @param id 记录ID
     */
    void notifyCashAdvance(Long id);

    /**
     * 对账现金代付记录
     *
     * @param id 记录ID
     * @param reconcileRemark 对账备注
     * @param reconcileOperatorId 对账操作员ID
     * @param reconcileOperatorName 对账操作员姓名
     */
    void reconcileCashAdvance(Long id, String reconcileRemark, Long reconcileOperatorId, String reconcileOperatorName);

    /**
     * 对账失败
     *
     * @param id 记录ID
     * @param reconcileRemark 对账备注
     * @param reconcileOperatorId 对账操作员ID
     * @param reconcileOperatorName 对账操作员姓名
     */
    void reconcileFailedCashAdvance(Long id, String reconcileRemark, Long reconcileOperatorId, String reconcileOperatorName);

    /**
     * 校验现金代付记录是否存在
     *
     * @param id 记录ID
     * @return 现金代付记录信息
     */
    CashAdvanceDO validateCashAdvanceExists(Long id);

    /**
     * 对账确认
     *
     * @param reconcileReqVO 对账请求
     */
    void reconcileConfirm(@Valid CashAdvanceReconcileReqVO reconcileReqVO);

    /**
     * 批量对账确认
     *
     * @param batchReqVO 批量对账请求
     * @return 批量对账结果
     */
    CashAdvanceBatchReconcileResultVO batchReconcileConfirm(@Valid CashAdvanceBatchReconcileReqVO batchReqVO);

    /**
     * 获取对账汇总
     *
     * @param summaryReqVO 汇总请求
     * @return 对账汇总信息
     */
    CashAdvanceReconcileSummaryVO getReconcileSummary(@Valid CashAdvanceReconcileSummaryReqVO summaryReqVO);

    /**
     * 通知危废模块
     *
     * @param id 记录ID
     */
    void notifyWasteModule(Long id);

} 