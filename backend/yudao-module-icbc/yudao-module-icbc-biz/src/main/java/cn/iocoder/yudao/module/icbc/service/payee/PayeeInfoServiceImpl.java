package cn.iocoder.yudao.module.icbc.service.payee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.*;
import cn.iocoder.yudao.module.icbc.convert.payee.PayeeInfoConvert;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.IcbcStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 工行收方信息 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class PayeeInfoServiceImpl implements PayeeInfoService {

    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @Override
    public Long createPayeeInfo(@Valid PayeeInfoSaveReqVO createReqVO) {
        // 校验身份证号码唯一性
        validateIdCardNoUnique(null, createReqVO.getIdCardNo());
        // 校验手机号码唯一性
        validateMobileUnique(null, createReqVO.getMobile());

        // 插入
        PayeeInfoDO payeeInfo = BeanUtils.toBean(createReqVO, PayeeInfoDO.class);
        // 生成合作方收方编号
        if (payeeInfo.getPartnerPayeeId() == null) {
            payeeInfo.setPartnerPayeeId(generatePartnerPayeeId());
        }
        payeeInfoMapper.insert(payeeInfo);
        // 返回
        return payeeInfo.getId();
    }

    @Override
    public void updatePayeeInfo(@Valid PayeeInfoSaveReqVO updateReqVO) {
        // 校验存在
        validatePayeeInfoExists(updateReqVO.getId());
        // 校验身份证号码唯一性
        validateIdCardNoUnique(updateReqVO.getId(), updateReqVO.getIdCardNo());
        // 校验手机号码唯一性
        validateMobileUnique(updateReqVO.getId(), updateReqVO.getMobile());

        // 更新
        PayeeInfoDO updateObj = BeanUtils.toBean(updateReqVO, PayeeInfoDO.class);
        payeeInfoMapper.updateById(updateObj);
    }

    @Override
    public void deletePayeeInfo(Long id) {
        // 校验存在
        validatePayeeInfoExists(id);
        // 删除
        payeeInfoMapper.deleteById(id);
    }

    private void validatePayeeInfoExists(Long id) {
        if (payeeInfoMapper.selectById(id) == null) {
            throw exception(PAYEE_NOT_EXISTS);
        }
    }

    private void validateIdCardNoUnique(Long id, String idCardNo) {
        PayeeInfoDO payeeInfo = payeeInfoMapper.selectByIdCardNo(idCardNo);
        if (payeeInfo == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的收方信息
        if (id == null) {
            throw exception(PAYEE_ID_CARD_EXISTS);
        }
        if (!payeeInfo.getId().equals(id)) {
            throw exception(PAYEE_ID_CARD_EXISTS);
        }
    }

    private void validateMobileUnique(Long id, String mobile) {
        PayeeInfoDO payeeInfo = payeeInfoMapper.selectByMobile(mobile);
        if (payeeInfo == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的收方信息
        if (id == null) {
            throw exception(PAYEE_MOBILE_EXISTS);
        }
        if (!payeeInfo.getId().equals(id)) {
            throw exception(PAYEE_MOBILE_EXISTS);
        }
    }

    @Override
    public PayeeInfoDO getPayeeInfo(Long id) {
        return payeeInfoMapper.selectById(id);
    }

    @Override
    public PageResult<PayeeInfoDO> getPayeeInfoPage(PayeeInfoPageReqVO pageReqVO) {
        return payeeInfoMapper.selectPage(pageReqVO);
    }

    @Override
    public List<PayeeInfoDO> getPayeeInfoList(PayeeInfoPageReqVO exportReqVO) {
        return payeeInfoMapper.selectList(exportReqVO);
    }

    // ==================== 工行接口相关方法 ====================

    @Override
    public Long addPayeeToIcbc(@Valid PayeeAddReqVO reqVO) {
        // 校验身份证号码唯一性
        validateIdCardNoUnique(null, reqVO.getIdNo());
        // 校验手机号码唯一性
        validateMobileUnique(null, reqVO.getMobile());

        // 转换为DO对象
        PayeeInfoDO payeeInfo = PayeeInfoConvert.INSTANCE.convert(reqVO);
        
        // TODO: 调用工行收方新增接口
        // 这里应该调用工行API，暂时模拟处理
        log.info("调用工行收方新增接口，请求参数：{}", reqVO);
        
        // 模拟工行返回的收方编号
        payeeInfo.setPayeeNo("ICBC" + System.currentTimeMillis());
        payeeInfo.setIcbcReceiverStatus("0"); // 初始状态为不可用
        payeeInfo.setIcbcOpenacctStatus("01"); // 工行开户状态（openacctStatus）：开户中
        
        // 保存到数据库
        payeeInfoMapper.insert(payeeInfo);
        
        return payeeInfo.getId();
    }

    @Override
    public List<PayeeInfoDO> queryPayeeFromIcbc(@Valid PayeeQueryReqVO reqVO) {
        // TODO: 调用工行收方查询接口
        // 这里应该调用工行API，暂时从本地数据库查询
        log.info("调用工行收方查询接口，请求参数：{}", reqVO);
        
        PayeeInfoPageReqVO pageReqVO = new PayeeInfoPageReqVO();
        pageReqVO.setIdCardNo(reqVO.getIdNo());
        
        return payeeInfoMapper.selectList(pageReqVO);
    }

    @Override
    public void handlePayeeAuditCallback(String outUserId, String auditStatus, String auditMsg, String icbcMediumId) {
        // 根据外部用户编号查找收方信息
        PayeeInfoDO payeeInfo = payeeInfoMapper.selectByPartnerPayeeId(outUserId);
        if (payeeInfo == null) {
            log.warn("收方审核回调：未找到收方信息，outUserId={}", outUserId);
            return;
        }

        // 更新审核状态
        PayeeInfoDO updateObj = new PayeeInfoDO();
        updateObj.setId(payeeInfo.getId());
        updateObj.setAuditMsg(auditMsg);
        updateObj.setIcbcMediumId(icbcMediumId);
        
        if ("1".equals(auditStatus)) {
            // 审核通过
            updateObj.setStatus(IcbcStatusEnum.AuditStatus.APPROVED.getStatus());
            updateObj.setIcbcReceiverStatus("1"); // 可用
            updateObj.setIcbcOpenacctStatus("02"); // 工行开户状态（openacctStatus）：开户成功
        } else {
            // 审核拒绝
            updateObj.setStatus(IcbcStatusEnum.AuditStatus.REJECTED.getStatus());
            updateObj.setIcbcReceiverStatus("0"); // 不可用
            updateObj.setIcbcOpenacctStatus("03"); // 工行开户状态（openacctStatus）：开户失败
        }
        
        payeeInfoMapper.updateById(updateObj);
        
        log.info("收方审核回调处理完成，outUserId={}，auditStatus={}", outUserId, auditStatus);
    }

    @Override
    public PayeeInfoDO getPayeeInfoByPartnerPayeeId(String partnerPayeeId) {
        return payeeInfoMapper.selectByPartnerPayeeId(partnerPayeeId);
    }

    @Override
    public PayeeInfoDO getPayeeInfoByIdCardNo(String idCardNo) {
        return payeeInfoMapper.selectByIdCardNo(idCardNo);
    }

    /**
     * 生成合作方收方编号
     */
    private String generatePartnerPayeeId() {
        return "PAYEE_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

} 