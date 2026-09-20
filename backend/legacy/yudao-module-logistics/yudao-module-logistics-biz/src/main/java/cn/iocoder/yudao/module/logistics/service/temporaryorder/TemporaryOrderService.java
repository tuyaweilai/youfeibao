package cn.iocoder.yudao.module.logistics.service.temporaryorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.temporaryorder.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.temporaryorder.TemporaryOrderDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流临时订单 Service 接口
 *
 * @author 芋道源码
 */
public interface TemporaryOrderService {

    /**
     * 创建物流临时订单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTemporaryOrder(@Valid TemporaryOrderCreateReqVO createReqVO);

    /**
     * 更新物流临时订单
     *
     * @param updateReqVO 更新信息
     */
    void updateTemporaryOrder(@Valid TemporaryOrderUpdateReqVO updateReqVO);

    /**
     * 删除物流临时订单
     *
     * @param id 编号
     */
    void deleteTemporaryOrder(Long id);

    /**
     * 获得物流临时订单
     *
     * @param id 编号
     * @return 物流临时订单
     */
    TemporaryOrderDO getTemporaryOrder(Long id);

    /**
     * 获得物流临时订单详情
     *
     * @param id 编号
     * @return 物流临时订单详情
     */
    TemporaryOrderRespVO getTemporaryOrderDetail(Long id);

    /**
     * 获得物流临时订单分页
     *
     * @param pageReqVO 分页查询
     * @return 物流临时订单分页
     */
    PageResult<TemporaryOrderRespVO> getTemporaryOrderPage(TemporaryOrderPageReqVO pageReqVO);

    /**
     * 获得物流临时订单列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 物流临时订单列表
     */
    List<TemporaryOrderDO> getTemporaryOrderList(TemporaryOrderPageReqVO exportReqVO);

    /**
     * 根据订单编号获得物流临时订单
     *
     * @param orderNo 订单编号
     * @return 物流临时订单
     */
    TemporaryOrderDO getTemporaryOrderByOrderNo(String orderNo);

    /**
     * 根据任务ID获得物流临时订单列表
     *
     * @param taskId 任务ID
     * @return 物流临时订单列表
     */
    List<TemporaryOrderDO> getTemporaryOrderListByTaskId(Long taskId);

    /**
     * 根据司机ID获得物流临时订单列表
     *
     * @param driverId 司机ID
     * @return 物流临时订单列表
     */
    List<TemporaryOrderDO> getTemporaryOrderListByDriverId(Long driverId);

    /**
     * 根据支付状态获得物流临时订单列表
     *
     * @param paymentStatus 支付状态
     * @return 物流临时订单列表
     */
    List<TemporaryOrderDO> getTemporaryOrderListByPaymentStatus(Integer paymentStatus);

    /**
     * 根据是否转为正式订单获得物流临时订单列表
     *
     * @param convertedToFormal 是否已转为正式订单
     * @return 物流临时订单列表
     */
    List<TemporaryOrderDO> getTemporaryOrderListByConvertedToFormal(Boolean convertedToFormal);

    /**
     * 根据废料类型获得物流临时订单列表
     *
     * @param wasteType 废料类型
     * @return 物流临时订单列表
     */
    List<TemporaryOrderDO> getTemporaryOrderListByWasteType(String wasteType);

    /**
     * 统计任务的临时订单数量
     *
     * @param taskId 任务ID
     * @return 临时订单数量
     */
    Long getTemporaryOrderCountByTaskId(Long taskId);

    /**
     * 统计司机的临时订单数量
     *
     * @param driverId 司机ID
     * @return 临时订单数量
     */
    Long getTemporaryOrderCountByDriverId(Long driverId);

    // ========== 业务方法 ==========

    /**
     * 支付临时订单
     *
     * @param id 订单ID
     * @param paymentAmount 支付金额
     * @param paymentMethod 支付方式
     * @param paymentVoucherUrl 支付凭证URL
     */
    void payTemporaryOrder(Long id, BigDecimal paymentAmount, String paymentMethod, String paymentVoucherUrl);

    /**
     * 退款临时订单
     *
     * @param id 订单ID
     * @param refundReason 退款原因
     */
    void refundTemporaryOrder(Long id, String refundReason);

    /**
     * 取消临时订单
     *
     * @param id 订单ID
     * @param cancelReason 取消原因
     */
    void cancelTemporaryOrder(Long id, String cancelReason);

    /**
     * 转为正式订单
     *
     * @param id 临时订单ID
     * @param formalOrderId 正式订单ID
     */
    void convertToFormalOrder(Long id, Long formalOrderId);

    /**
     * 校验临时订单是否存在
     *
     * @param id 订单ID
     * @return 临时订单信息
     */
    TemporaryOrderDO validateTemporaryOrderExists(Long id);

} 