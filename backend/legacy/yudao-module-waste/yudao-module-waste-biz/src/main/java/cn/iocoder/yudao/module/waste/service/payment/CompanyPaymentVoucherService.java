package cn.iocoder.yudao.module.waste.service.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.CompanyPaymentVoucherDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 对公付款凭证 Service 接口
 *
 * @author 芋道源码
 */
public interface CompanyPaymentVoucherService {

    /**
     * 创建对公付款凭证
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCompanyPaymentVoucher(@Valid CompanyPaymentVoucherCreateReqVO createReqVO);

    /**
     * 更新对公付款凭证
     *
     * @param updateReqVO 更新信息
     */
    void updateCompanyPaymentVoucher(@Valid CompanyPaymentVoucherUpdateReqVO updateReqVO);

    /**
     * 删除对公付款凭证
     *
     * @param id 编号
     */
    void deleteCompanyPaymentVoucher(Long id);

    /**
     * 获得对公付款凭证
     *
     * @param id 编号
     * @return 对公付款凭证
     */
    CompanyPaymentVoucherDO getCompanyPaymentVoucher(Long id);

    /**
     * 获得对公付款凭证详情
     *
     * @param id 编号
     * @return 对公付款凭证详情
     */
    CompanyPaymentVoucherRespVO getCompanyPaymentVoucherDetail(Long id);

    /**
     * 获得企业付款凭证分页
     *
     * @param pageReqVO 分页查询
     * @return 企业付款凭证分页
     */
    PageResult<CompanyPaymentVoucherDO> getCompanyPaymentVoucherPage(CompanyPaymentVoucherPageReqVO pageReqVO);

    /**
     * 获得企业付款凭证列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 企业付款凭证列表
     */
    List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherList(CompanyPaymentVoucherPageReqVO exportReqVO);

    /**
     * 根据订单ID获得付款凭证列表
     *
     * @param orderId 订单ID
     * @return 付款凭证列表
     */
    List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherListByOrderId(Long orderId);

    /**
     * 根据凭证号获得付款凭证
     *
     * @param voucherNo 凭证号
     * @return 付款凭证
     */
    CompanyPaymentVoucherDO getCompanyPaymentVoucherByVoucherNo(String voucherNo);

    /**
     * 根据付款类型获得付款凭证列表
     *
     * @param paymentType 付款类型
     * @return 付款凭证列表
     */
    List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherListByPaymentType(Integer paymentType);

    /**
     * 根据产废企业确认状态获得付款凭证列表
     *
     * @param producerConfirmStatus 产废企业确认状态
     * @return 付款凭证列表
     */
    List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherListByProducerConfirmStatus(Integer producerConfirmStatus);

    /**
     * 根据回收企业确认状态获得付款凭证列表
     *
     * @param recyclerConfirmStatus 回收企业确认状态
     * @return 付款凭证列表
     */
    List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherListByRecyclerConfirmStatus(Integer recyclerConfirmStatus);

    /**
     * 获得争议凭证列表
     *
     * @return 争议凭证列表
     */
    List<CompanyPaymentVoucherDO> getDisputedCompanyPaymentVouchers();

    /**
     * 获得待确认凭证列表
     *
     * @return 待确认凭证列表
     */
    List<CompanyPaymentVoucherDO> getPendingConfirmationCompanyPaymentVouchers();

    /**
     * 根据付款时间范围获得付款凭证列表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 付款凭证列表
     */
    List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherListByPaymentTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 产废企业确认凭证
     *
     * @param id 凭证ID
     * @param confirmedBy 确认人
     * @param confirmRemark 确认备注
     */
    void confirmByProducer(Long id, String confirmedBy, String confirmRemark);

    /**
     * 回收企业确认凭证
     *
     * @param id 凭证ID
     * @param confirmedBy 确认人
     * @param confirmRemark 确认备注
     */
    void confirmByRecycler(Long id, String confirmedBy, String confirmRemark);

    /**
     * 产废企业拒绝凭证
     *
     * @param id 凭证ID
     * @param rejectedBy 拒绝人
     * @param rejectReason 拒绝原因
     */
    void rejectByProducer(Long id, String rejectedBy, String rejectReason);

    /**
     * 回收企业拒绝凭证
     *
     * @param id 凭证ID
     * @param rejectedBy 拒绝人
     * @param rejectReason 拒绝原因
     */
    void rejectByRecycler(Long id, String rejectedBy, String rejectReason);

    /**
     * 创建付款凭证
     *
     * @param orderId 订单ID
     * @param voucherNo 凭证号
     * @param paymentType 付款类型
     * @param paymentAmount 付款金额
     * @param payerCompanyName 付款方公司名称
     * @param payerAccountName 付款方账户名称
     * @param payerAccountNumber 付款方账户号码
     * @param payerBankName 付款方银行名称
     * @param payeeCompanyName 收款方公司名称
     * @param payeeAccountName 收款方账户名称
     * @param payeeAccountNumber 收款方账户号码
     * @param payeeBankName 收款方银行名称
     * @param transactionId 交易流水号
     * @param paymentTime 付款时间
     * @param attachmentUrls 附件URL列表
     * @param remark 备注
     * @return 凭证ID
     */
    Long createPaymentVoucher(Long orderId, String voucherNo, Integer paymentType, BigDecimal paymentAmount,
                             String payerCompanyName, String payerAccountName, String payerAccountNumber, String payerBankName,
                             String payeeCompanyName, String payeeAccountName, String payeeAccountNumber, String payeeBankName,
                             String transactionId, LocalDateTime paymentTime, String attachmentUrls, String remark);

    /**
     * 批量确认凭证
     *
     * @param ids 凭证ID列表
     * @param confirmedBy 确认人
     * @param confirmType 确认类型（1-产废企业，2-回收企业）
     */
    void batchConfirmVouchers(List<Long> ids, String confirmedBy, Integer confirmType);

    /**
     * 校验付款凭证是否存在
     *
     * @param id 凭证ID
     * @return 付款凭证信息
     */
    CompanyPaymentVoucherDO validateCompanyPaymentVoucherExists(Long id);

    /**
     * 产废企业确认
     *
     * @param id 凭证ID
     * @param confirmed 是否确认
     * @param comments 确认意见
     */
    void producerConfirm(Long id, Boolean confirmed, String comments);

    /**
     * 回收企业确认
     *
     * @param id 凭证ID
     * @param confirmed 是否确认
     * @param comments 确认意见
     */
    void recyclerConfirm(Long id, Boolean confirmed, String comments);

    /**
     * 处理争议
     *
     * @param id 凭证ID
     * @param resolution 处理方案
     */
    void handleDispute(Long id, String resolution);

    /**
     * 根据订单ID获取凭证
     *
     * @param orderId 订单ID
     * @return 凭证列表
     */
    List<CompanyPaymentVoucherDO> getVouchersByOrderId(Long orderId);

    /**
     * 获取待产废企业确认的凭证
     *
     * @return 凭证列表
     */
    List<CompanyPaymentVoucherDO> getPendingProducerConfirmVouchers();

    /**
     * 获取待回收企业确认的凭证
     *
     * @return 凭证列表
     */
    List<CompanyPaymentVoucherDO> getPendingRecyclerConfirmVouchers();

    /**
     * 获取争议凭证
     *
     * @return 凭证列表
     */
    List<CompanyPaymentVoucherDO> getDisputeVouchers();

    /**
     * 批量确认
     *
     * @param ids 凭证ID列表
     * @param confirmType 确认类型
     * @param comments 确认意见
     */
    void batchConfirm(List<Long> ids, String confirmType, String comments);

} 