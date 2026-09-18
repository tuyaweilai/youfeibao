package cn.iocoder.yudao.module.waste.service.impl.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.CompanyPaymentVoucherUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.CompanyPaymentVoucherDO;
import cn.iocoder.yudao.module.waste.dal.mysql.payment.CompanyPaymentVoucherMapper;
import cn.iocoder.yudao.module.waste.service.payment.CompanyPaymentVoucherService;
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
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.COMPANY_PAYMENT_VOUCHER_NOT_EXISTS;

/**
 * 对公付款凭证 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class CompanyPaymentVoucherServiceImpl implements CompanyPaymentVoucherService {

    @Resource
    private CompanyPaymentVoucherMapper companyPaymentVoucherMapper;

    @Override
    public Long createCompanyPaymentVoucher(@Valid CompanyPaymentVoucherCreateReqVO createReqVO) {
        // 插入
        CompanyPaymentVoucherDO companyPaymentVoucher = BeanUtils.toBean(createReqVO, CompanyPaymentVoucherDO.class);
        companyPaymentVoucherMapper.insert(companyPaymentVoucher);
        // 返回
        return companyPaymentVoucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCompanyPaymentVoucher(@Valid CompanyPaymentVoucherUpdateReqVO updateReqVO) {
        // 校验存在
        validateCompanyPaymentVoucherExists(updateReqVO.getId());
        // 更新
        CompanyPaymentVoucherDO updateObj = BeanUtils.toBean(updateReqVO, CompanyPaymentVoucherDO.class);
        companyPaymentVoucherMapper.updateById(updateObj);
    }

    @Override
    public void deleteCompanyPaymentVoucher(Long id) {
        // 校验存在
        validateCompanyPaymentVoucherExists(id);
        // 删除
        companyPaymentVoucherMapper.deleteById(id);
    }

    @Override
    public CompanyPaymentVoucherDO getCompanyPaymentVoucher(Long id) {
        return companyPaymentVoucherMapper.selectById(id);
    }

    @Override
    public CompanyPaymentVoucherRespVO getCompanyPaymentVoucherDetail(Long id) {
        CompanyPaymentVoucherDO companyPaymentVoucher = validateCompanyPaymentVoucherExists(id);
        return BeanUtils.toBean(companyPaymentVoucher, CompanyPaymentVoucherRespVO.class);
    }

    @Override
    public PageResult<CompanyPaymentVoucherDO> getCompanyPaymentVoucherPage(CompanyPaymentVoucherPageReqVO pageReqVO) {
        PageResult<CompanyPaymentVoucherDO> pageResult = companyPaymentVoucherMapper.selectPage(pageReqVO);
        return pageResult;
    }

    @Override
    public List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherList(CompanyPaymentVoucherPageReqVO exportReqVO) {
        return companyPaymentVoucherMapper.selectList(exportReqVO);
    }

    @Override
    public List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherListByOrderId(Long orderId) {
        return companyPaymentVoucherMapper.selectListByOrderId(orderId);
    }

    @Override
    public CompanyPaymentVoucherDO getCompanyPaymentVoucherByVoucherNo(String voucherNo) {
        return companyPaymentVoucherMapper.selectByVoucherNo(voucherNo);
    }

    @Override
    public List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherListByPaymentType(Integer paymentType) {
        return companyPaymentVoucherMapper.selectListByPaymentType(paymentType);
    }

    @Override
    public List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherListByProducerConfirmStatus(Integer producerConfirmStatus) {
        return companyPaymentVoucherMapper.selectListByProducerConfirmStatus(producerConfirmStatus);
    }

    @Override
    public List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherListByRecyclerConfirmStatus(Integer recyclerConfirmStatus) {
        return companyPaymentVoucherMapper.selectListByRecyclerConfirmStatus(recyclerConfirmStatus);
    }

    @Override
    public List<CompanyPaymentVoucherDO> getDisputedCompanyPaymentVouchers() {
        return companyPaymentVoucherMapper.selectDisputedVouchers();
    }

    @Override
    public List<CompanyPaymentVoucherDO> getPendingConfirmationCompanyPaymentVouchers() {
        return companyPaymentVoucherMapper.selectPendingConfirmationVouchers();
    }

    @Override
    public List<CompanyPaymentVoucherDO> getCompanyPaymentVoucherListByPaymentTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return companyPaymentVoucherMapper.selectListByPaymentTimeRange(startTime, endTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmByProducer(Long id, String confirmedBy, String confirmRemark) {
        CompanyPaymentVoucherDO voucher = validateCompanyPaymentVoucherExists(id);
        voucher.setConfirmedByProducer(true); // 已确认
        voucher.setProducerConfirmTime(LocalDateTime.now());
        // 注意：CompanyPaymentVoucherDO中没有producerConfirmedBy、producerConfirmRemark字段
        companyPaymentVoucherMapper.updateById(voucher);
        
        log.info("[confirmByProducer][产废企业确认凭证] id={}, confirmedBy={}", id, confirmedBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmByRecycler(Long id, String confirmedBy, String confirmRemark) {
        CompanyPaymentVoucherDO voucher = validateCompanyPaymentVoucherExists(id);
        voucher.setConfirmedByRecycling(true); // 已确认
        voucher.setRecyclingConfirmTime(LocalDateTime.now());
        // 注意：CompanyPaymentVoucherDO中没有recyclerConfirmedBy、recyclerConfirmRemark字段
        companyPaymentVoucherMapper.updateById(voucher);
        
        log.info("[confirmByRecycler][回收企业确认凭证] id={}, confirmedBy={}", id, confirmedBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectByProducer(Long id, String rejectedBy, String rejectReason) {
        CompanyPaymentVoucherDO voucher = validateCompanyPaymentVoucherExists(id);
        voucher.setConfirmedByProducer(false); // 已拒绝
        voucher.setProducerConfirmTime(LocalDateTime.now());
        voucher.setDisputeReason(rejectReason);
        companyPaymentVoucherMapper.updateById(voucher);
        
        log.info("[rejectByProducer][产废企业拒绝凭证] id={}, rejectedBy={}, rejectReason={}", 
                id, rejectedBy, rejectReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectByRecycler(Long id, String rejectedBy, String rejectReason) {
        CompanyPaymentVoucherDO voucher = validateCompanyPaymentVoucherExists(id);
        voucher.setConfirmedByRecycling(false); // 已拒绝
        voucher.setRecyclingConfirmTime(LocalDateTime.now());
        voucher.setDisputeReason(rejectReason);
        companyPaymentVoucherMapper.updateById(voucher);
        
        log.info("[rejectByRecycler][回收企业拒绝凭证] id={}, rejectedBy={}, rejectReason={}", 
                id, rejectedBy, rejectReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPaymentVoucher(Long orderId, String voucherNo, Integer paymentType, BigDecimal paymentAmount,
                                    String payerCompanyName, String payerAccountName, String payerAccountNumber, String payerBankName,
                                    String payeeCompanyName, String payeeAccountName, String payeeAccountNumber, String payeeBankName,
                                    String transactionId, LocalDateTime paymentTime, String attachmentUrls, String remark) {
        CompanyPaymentVoucherDO voucher = new CompanyPaymentVoucherDO();
        voucher.setOrderId(orderId);
        voucher.setVoucherNo(voucherNo);
        voucher.setPaymentType(paymentType);
        voucher.setPaymentAmount(paymentAmount);
        voucher.setPaymentBank(payerBankName);
        voucher.setPaymentAccount(payerAccountNumber);
        voucher.setPayeeName(payeeAccountName);
        voucher.setPayeeBank(payeeBankName);
        voucher.setPayeeAccount(payeeAccountNumber);
        voucher.setTransactionNo(transactionId);
        voucher.setTransferVoucherUrl(attachmentUrls);
        voucher.setRemark(remark);
        voucher.setConfirmedByProducer(false); // 待确认
        voucher.setConfirmedByRecycling(false); // 待确认
        
        companyPaymentVoucherMapper.insert(voucher);
        log.info("[createPaymentVoucher][创建付款凭证] orderId={}, voucherNo={}, paymentAmount={}", 
                orderId, voucherNo, paymentAmount);
        return voucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchConfirmVouchers(List<Long> ids, String confirmedBy, Integer confirmType) {
        for (Long id : ids) {
            if (confirmType == 1) {
                confirmByProducer(id, confirmedBy, "批量确认");
            } else if (confirmType == 2) {
                confirmByRecycler(id, confirmedBy, "批量确认");
            }
        }
        log.info("[batchConfirmVouchers][批量确认凭证] ids={}, confirmedBy={}, confirmType={}", 
                ids, confirmedBy, confirmType);
    }

    @Override
    public CompanyPaymentVoucherDO validateCompanyPaymentVoucherExists(Long id) {
        CompanyPaymentVoucherDO companyPaymentVoucher = companyPaymentVoucherMapper.selectById(id);
        if (companyPaymentVoucher == null) {
            throw exception(COMPANY_PAYMENT_VOUCHER_NOT_EXISTS);
        }
        return companyPaymentVoucher;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void producerConfirm(Long id, Boolean confirmed, String comments) {
        CompanyPaymentVoucherDO voucher = validateCompanyPaymentVoucherExists(id);
        voucher.setConfirmedByProducer(confirmed);
        voucher.setProducerConfirmTime(LocalDateTime.now());
        if (!confirmed) {
            voucher.setDisputeReason(comments);
        }
        companyPaymentVoucherMapper.updateById(voucher);
        
        log.info("[producerConfirm][产废企业确认凭证] id={}, confirmed={}, comments={}", id, confirmed, comments);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recyclerConfirm(Long id, Boolean confirmed, String comments) {
        CompanyPaymentVoucherDO voucher = validateCompanyPaymentVoucherExists(id);
        voucher.setConfirmedByRecycling(confirmed);
        voucher.setRecyclingConfirmTime(LocalDateTime.now());
        if (!confirmed) {
            voucher.setDisputeReason(comments);
        }
        companyPaymentVoucherMapper.updateById(voucher);
        
        log.info("[recyclerConfirm][回收企业确认凭证] id={}, confirmed={}, comments={}", id, confirmed, comments);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleDispute(Long id, String resolution) {
        CompanyPaymentVoucherDO voucher = validateCompanyPaymentVoucherExists(id);
        voucher.setVoucherStatus(1); // 已处理
        voucher.setDisputeReason(resolution);
        companyPaymentVoucherMapper.updateById(voucher);
        
        log.info("[handleDispute][处理争议] id={}, resolution={}", id, resolution);
    }

    @Override
    public List<CompanyPaymentVoucherDO> getVouchersByOrderId(Long orderId) {
        return companyPaymentVoucherMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<CompanyPaymentVoucherDO> getPendingProducerConfirmVouchers() {
        return companyPaymentVoucherMapper.selectListByProducerConfirmStatus(1); // 1-待确认
    }

    @Override
    public List<CompanyPaymentVoucherDO> getPendingRecyclerConfirmVouchers() {
        return companyPaymentVoucherMapper.selectListByRecyclerConfirmStatus(1); // 1-待确认
    }

    @Override
    public List<CompanyPaymentVoucherDO> getDisputeVouchers() {
        return companyPaymentVoucherMapper.selectDisputedVouchers();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchConfirm(List<Long> ids, String confirmType, String comments) {
        for (Long id : ids) {
            if ("producer".equals(confirmType)) {
                producerConfirm(id, true, comments);
            } else if ("recycler".equals(confirmType)) {
                recyclerConfirm(id, true, comments);
            }
        }
        log.info("[batchConfirm][批量确认] ids={}, confirmType={}, comments={}", ids, confirmType, comments);
    }

} 