package cn.iocoder.yudao.module.icbc.service.payer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.*;
import cn.iocoder.yudao.module.icbc.convert.payer.PayerInfoConvert;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 工行付方信息 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class PayerInfoServiceImpl implements PayerInfoService {

    @Resource
    private PayerInfoMapper payerInfoMapper;

    @Override
    public Long createPayerInfo(PayerInfoSaveReqVO createReqVO) {
        // 1. 校验唯一性
        validatePayerInfoUnique(null, createReqVO.getCreditCode(), createReqVO.getTaxNo());

        // 2. 生成合作方付方编号
        if (createReqVO.getPartnerPayerId() == null) {
            createReqVO.setPartnerPayerId(generatePartnerPayerId());
        }

        // 3. 插入
        PayerInfoDO payerInfo = PayerInfoConvert.INSTANCE.convert(createReqVO);
        payerInfoMapper.insert(payerInfo);
        return payerInfo.getId();
    }

    @Override
    public void updatePayerInfo(PayerInfoSaveReqVO updateReqVO) {
        // 1. 校验存在
        validatePayerInfoExists(updateReqVO.getId());
        // 2. 校验唯一性
        validatePayerInfoUnique(updateReqVO.getId(), updateReqVO.getCreditCode(), updateReqVO.getTaxNo());

        // 3. 更新
        PayerInfoDO updateObj = PayerInfoConvert.INSTANCE.convert(updateReqVO);
        payerInfoMapper.updateById(updateObj);
    }

    @Override
    public void deletePayerInfo(Long id) {
        // 校验存在
        validatePayerInfoExists(id);
        // 删除
        payerInfoMapper.deleteById(id);
    }

    @Override
    public void deletePayerInfos(Collection<Long> ids) {
        // 批量删除
        payerInfoMapper.deleteBatchIds(ids);
    }

    private void validatePayerInfoExists(Long id) {
        if (payerInfoMapper.selectById(id) == null) {
            throw exception(PAYER_INFO_NOT_EXISTS);
        }
    }

    private void validatePayerInfoUnique(Long id, String creditCode, String taxNo) {
        // 1. 校验统一社会信用代码唯一性
        PayerInfoDO payerInfo = payerInfoMapper.selectByCreditCode(creditCode);
        if (payerInfo != null && !payerInfo.getId().equals(id)) {
            throw exception(PAYER_INFO_CREDIT_CODE_EXISTS);
        }

        // 2. 校验纳税人识别号唯一性
        payerInfo = payerInfoMapper.selectByTaxNo(taxNo);
        if (payerInfo != null && !payerInfo.getId().equals(id)) {
            throw exception(PAYER_INFO_TAX_NO_EXISTS);
        }
    }

    private String generatePartnerPayerId() {
        return "PAYER_" + IdUtil.fastSimpleUUID();
    }

    @Override
    public PayerInfoDO getPayerInfo(Long id) {
        return payerInfoMapper.selectById(id);
    }

    @Override
    public PayerInfoDO getPayerInfoByCreditCode(String creditCode) {
        return payerInfoMapper.selectByCreditCode(creditCode);
    }

    @Override
    public PayerInfoDO getPayerInfoByTaxNo(String taxNo) {
        return payerInfoMapper.selectByTaxNo(taxNo);
    }

    @Override
    public List<PayerInfoDO> getPayerInfoList(Collection<Long> ids) {
        return payerInfoMapper.selectBatchIds(ids);
    }

    @Override
    public List<PayerInfoDO> getPayerInfoList(PayerInfoExportReqVO exportReqVO) {
        return payerInfoMapper.selectList(exportReqVO);
    }

    @Override
    public PageResult<PayerInfoDO> getPayerInfoPage(PayerInfoPageReqVO pageReqVO) {
        return payerInfoMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addPayerToIcbc(PayerAddReqVO reqVO) {
        // 1. 数据校验
        validateCreditCodeAndTaxNo(reqVO.getCreditCode(), reqVO.getTaxNo());

        // 2. 生成合作方付方编号
        String partnerPayerId = generatePartnerPayerId();

        // 3. 构建付方信息对象
        PayerInfoDO payerInfo = PayerInfoConvert.INSTANCE.convert(reqVO);
        payerInfo.setPartnerPayerId(partnerPayerId);
        payerInfo.setStatus(0); // 待审核
        payerInfo.setPayerNo(generatePayerNo()); // 生成工行付方编号

        // 4. 调用工行付方新增接口（模拟）
        // TODO: 实际实现中，这里应该调用工行的API接口
        // 示例: String payerNo = icbcApiService.addPayer(payerInfo);
        log.info("[addPayerToIcbc] 调用工行付方新增接口：{}", payerInfo);
        
        // 5. 保存到数据库
        payerInfoMapper.insert(payerInfo);

        // 6. 返回ID
        return payerInfo.getId();
    }

    @Override
    public PayerInfoDO queryPayerFromIcbc(PayerQueryReqVO reqVO) {
        // 1. 先从本地数据库查询
        PayerInfoDO payerInfo = payerInfoMapper.selectByCreditCode(reqVO.getCreditCode());
        if (payerInfo != null) {
            return payerInfo;
        }

        // 2. 调用工行付方查询接口（模拟）
        // TODO: 实际实现中，这里应该调用工行的API接口
        // 示例: PayerInfoDO result = icbcApiService.queryPayer(reqVO.getCreditCode(), reqVO.getTaxNo());
        log.info("[queryPayerFromIcbc] 调用工行付方查询接口：creditCode={}, taxNo={}", reqVO.getCreditCode(), reqVO.getTaxNo());

        // 3. 如果不存在，抛出异常
        throw exception(PAYER_INFO_NOT_EXISTS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePayerAuditCallback(String payerNo, Integer status, String auditMsg, String payerStatus) {
        // 1. 查询付方信息
        PayerInfoDO payerInfo = payerInfoMapper.selectOne(PayerInfoDO::getPayerNo, payerNo);
        if (payerInfo == null) {
            log.error("[handlePayerAuditCallback] 付方信息不存在：payerNo={}", payerNo);
            throw exception(PAYER_INFO_NOT_EXISTS);
        }

        // 2. 更新审核状态
        PayerInfoDO updateObj = new PayerInfoDO();
        updateObj.setId(payerInfo.getId());
        updateObj.setStatus(status);
        updateObj.setAuditMsg(auditMsg);
        updateObj.setIcbcPayerStatus(payerStatus);

        // 3. 更新数据库
        payerInfoMapper.updateById(updateObj);
        log.info("[handlePayerAuditCallback] 更新付方审核状态成功：payerNo={}, status={}", payerNo, status);
    }

    /**
     * 校验统一社会信用代码和纳税人识别号
     *
     * @param creditCode 统一社会信用代码
     * @param taxNo      纳税人识别号
     */
    private void validateCreditCodeAndTaxNo(String creditCode, String taxNo) {
        // 1. 校验统一社会信用代码是否已存在
        PayerInfoDO existingPayer = payerInfoMapper.selectByCreditCode(creditCode);
        if (existingPayer != null) {
            throw new ServiceException(PAYER_INFO_CREDIT_CODE_EXISTS);
        }

        // 2. 校验纳税人识别号是否已存在
        existingPayer = payerInfoMapper.selectByTaxNo(taxNo);
        if (existingPayer != null) {
            throw new ServiceException(PAYER_INFO_TAX_NO_EXISTS);
        }
    }

    /**
     * 生成工行付方编号
     *
     * @return 工行付方编号
     */
    private String generatePayerNo() {
        return "P" + System.currentTimeMillis() + RandomUtil.randomNumbers(6);
    }
} 