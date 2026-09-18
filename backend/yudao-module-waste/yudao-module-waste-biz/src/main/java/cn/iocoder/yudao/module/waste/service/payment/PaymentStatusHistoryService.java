package cn.iocoder.yudao.module.waste.service.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.PaymentStatusHistoryCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.PaymentStatusHistoryPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.PaymentStatusHistoryRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.PaymentStatusHistoryDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 付款状态变更历史 Service 接口
 *
 * @author 芋道源码
 */
public interface PaymentStatusHistoryService {

    /**
     * 创建付款状态变更历史
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPaymentStatusHistory(@Valid PaymentStatusHistoryCreateReqVO createReqVO);

    /**
     * 删除付款状态变更历史
     *
     * @param id 编号
     */
    void deletePaymentStatusHistory(Long id);

    /**
     * 获得付款状态变更历史
     *
     * @param id 编号
     * @return 付款状态变更历史
     */
    PaymentStatusHistoryDO getPaymentStatusHistory(Long id);

    /**
     * 获得付款状态变更历史详情
     *
     * @param id 编号
     * @return 付款状态变更历史详情
     */
    PaymentStatusHistoryRespVO getPaymentStatusHistoryDetail(Long id);

    /**
     * 获得付款状态变更历史分页
     *
     * @param pageReqVO 分页查询
     * @return 付款状态变更历史分页
     */
    PageResult<PaymentStatusHistoryDO> getPaymentStatusHistoryPage(PaymentStatusHistoryPageReqVO pageReqVO);

    /**
     * 获得付款状态变更历史列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoryList(PaymentStatusHistoryPageReqVO exportReqVO);

    /**
     * 根据订单ID获得付款状态变更历史列表
     *
     * @param orderId 订单ID
     * @return 付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByOrderId(Long orderId);

    /**
     * 根据原状态获得付款状态变更历史列表
     *
     * @param statusFrom 原状态
     * @return 付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByStatusFrom(Integer statusFrom);

    /**
     * 根据目标状态获得付款状态变更历史列表
     *
     * @param statusTo 目标状态
     * @return 付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByStatusTo(Integer statusTo);

    /**
     * 根据操作者类型获得付款状态变更历史列表
     *
     * @param operatorType 操作者类型
     * @return 付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByOperatorType(Integer operatorType);

    /**
     * 根据操作者ID获得付款状态变更历史列表
     *
     * @param operatorId 操作者ID
     * @return 付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByOperatorId(Long operatorId);

    /**
     * 根据操作者姓名获得付款状态变更历史列表
     *
     * @param operatorName 操作者姓名
     * @return 付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByOperatorName(String operatorName);

    /**
     * 根据付款方式获得付款状态变更历史列表
     *
     * @param paymentMethod 付款方式
     * @return 付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByPaymentMethod(Integer paymentMethod);

    /**
     * 根据凭证ID获得付款状态变更历史列表
     *
     * @param voucherId 凭证ID
     * @return 付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByVoucherId(Long voucherId);

    /**
     * 根据变更时间范围获得付款状态变更历史列表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByChangeTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获得系统操作的付款状态变更历史列表
     *
     * @return 系统操作的付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getSystemOperationPaymentStatusHistories();

    /**
     * 获得产废企业操作的付款状态变更历史列表
     *
     * @return 产废企业操作的付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getProducerOperationPaymentStatusHistories();

    /**
     * 获得回收企业操作的付款状态变更历史列表
     *
     * @return 回收企业操作的付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getRecyclerOperationPaymentStatusHistories();

    /**
     * 获得平台管理员操作的付款状态变更历史列表
     *
     * @return 平台管理员操作的付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getAdminOperationPaymentStatusHistories();

    /**
     * 获得银行转账付款的状态变更历史列表
     *
     * @return 银行转账付款的状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getBankTransferPaymentStatusHistories();

    /**
     * 获得现金付款的状态变更历史列表
     *
     * @return 现金付款的状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getCashPaymentStatusHistories();

    /**
     * 获得支票付款的状态变更历史列表
     *
     * @return 支票付款的状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getCheckPaymentStatusHistories();

    /**
     * 获得有凭证的付款状态变更历史列表
     *
     * @return 有凭证的付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoriesWithVoucher();

    /**
     * 获得订单的最新付款状态变更历史
     *
     * @param orderId 订单ID
     * @return 最新付款状态变更历史
     */
    PaymentStatusHistoryDO getLatestPaymentStatusHistoryByOrderId(Long orderId);

    /**
     * 获得订单的第一条付款状态变更历史
     *
     * @param orderId 订单ID
     * @return 第一条付款状态变更历史
     */
    PaymentStatusHistoryDO getFirstPaymentStatusHistoryByOrderId(Long orderId);

    /**
     * 获得状态转换的付款状态变更历史列表
     *
     * @param statusFrom 原状态
     * @param statusTo 目标状态
     * @return 状态转换的付款状态变更历史列表
     */
    List<PaymentStatusHistoryDO> getPaymentStatusHistoryListByStatusTransition(Integer statusFrom, Integer statusTo);

    /**
     * 记录付款状态变更
     *
     * @param orderId 订单ID
     * @param statusFrom 原状态
     * @param statusTo 目标状态
     * @param statusName 状态名称
     * @param changeReason 变更原因
     * @param operatorType 操作者类型
     * @param operatorId 操作者ID
     * @param operatorName 操作者姓名
     * @param paymentAmount 付款金额
     * @param paymentMethod 付款方式
     * @param voucherId 凭证ID
     * @param businessData 业务数据
     * @return 状态变更历史ID
     */
    Long recordPaymentStatusChange(Long orderId, Integer statusFrom, Integer statusTo, String statusName,
                                  String changeReason, Integer operatorType, Long operatorId, String operatorName,
                                  BigDecimal paymentAmount, Integer paymentMethod, Long voucherId, String businessData);

    /**
     * 记录系统自动状态变更
     *
     * @param orderId 订单ID
     * @param statusFrom 原状态
     * @param statusTo 目标状态
     * @param statusName 状态名称
     * @param changeReason 变更原因
     * @return 状态变更历史ID
     */
    Long recordSystemStatusChange(Long orderId, Integer statusFrom, Integer statusTo, String statusName, String changeReason);

    /**
     * 记录企业操作状态变更
     *
     * @param orderId 订单ID
     * @param statusFrom 原状态
     * @param statusTo 目标状态
     * @param statusName 状态名称
     * @param changeReason 变更原因
     * @param operatorType 操作者类型（2-产废企业，3-回收企业）
     * @param operatorId 操作者ID
     * @param operatorName 操作者姓名
     * @param paymentAmount 付款金额
     * @param paymentMethod 付款方式
     * @param voucherId 凭证ID
     * @return 状态变更历史ID
     */
    Long recordEnterpriseStatusChange(Long orderId, Integer statusFrom, Integer statusTo, String statusName,
                                     String changeReason, Integer operatorType, Long operatorId, String operatorName,
                                     BigDecimal paymentAmount, Integer paymentMethod, Long voucherId);

    /**
     * 校验付款状态变更历史是否存在
     *
     * @param id 状态变更历史ID
     * @return 付款状态变更历史信息
     */
    PaymentStatusHistoryDO validatePaymentStatusHistoryExists(Long id);

    /**
     * 记录状态变更
     *
     * @param orderId 订单ID
     * @param paymentRecordId 付款记录ID
     * @param fromStatus 原状态
     * @param toStatus 新状态
     * @param changeReason 变更原因
     */
    void recordStatusChange(Long orderId, Long paymentRecordId, Integer fromStatus, Integer toStatus, String changeReason);

    /**
     * 记录系统变更
     *
     * @param orderId 订单ID
     * @param paymentRecordId 付款记录ID
     * @param fromStatus 原状态
     * @param toStatus 新状态
     * @param changeReason 变更原因
     * @param systemAction 系统动作
     */
    void recordSystemChange(Long orderId, Long paymentRecordId, Integer fromStatus, Integer toStatus, String changeReason, String systemAction);

    /**
     * 根据订单ID获取历史
     *
     * @param orderId 订单ID
     * @return 历史记录列表
     */
    List<PaymentStatusHistoryDO> getHistoryByOrderId(Long orderId);

    /**
     * 根据付款记录ID获取历史
     *
     * @param paymentRecordId 付款记录ID
     * @return 历史记录列表
     */
    List<PaymentStatusHistoryDO> getHistoryByPaymentRecordId(Long paymentRecordId);

    /**
     * 根据操作者获取历史
     *
     * @param operatorId 操作者ID
     * @return 历史记录列表
     */
    List<PaymentStatusHistoryDO> getHistoryByOperator(Long operatorId);

    /**
     * 获取系统变更记录
     *
     * @return 系统变更记录列表
     */
    List<PaymentStatusHistoryDO> getSystemChanges();

    /**
     * 获取手动变更记录
     *
     * @return 手动变更记录列表
     */
    List<PaymentStatusHistoryDO> getManualChanges();

} 