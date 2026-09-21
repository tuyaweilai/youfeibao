package cn.iocoder.yudao.module.icbc.service.payee;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.*;
import cn.iocoder.yudao.module.icbc.convert.payee.PayeeInfoConvert;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.IcbcStatusEnum;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
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
    @Resource
    private NaturalPersonService naturalPersonService;

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
        // 挂到平台级自然人主体：同一个身份证在别的租户已建档时复用同一个主体（ADR 0017）
        payeeInfo.setNaturalPersonId(registerNaturalPerson(createReqVO.getName(), createReqVO.getIdCardNo(),
                createReqVO.getMobile(), createReqVO.getIdSignDate(), createReqVO.getIdValidityPeriod()).getId());
        payeeInfoMapper.insert(payeeInfo);
        // 返回
        return payeeInfo.getId();
    }

    @Override
    public void updatePayeeInfo(@Valid PayeeInfoSaveReqVO updateReqVO) {
        // 校验存在
        PayeeInfoDO existing = validatePayeeInfoExists(updateReqVO.getId());
        // 校验身份证号码唯一性
        validateIdCardNoUnique(updateReqVO.getId(), updateReqVO.getIdCardNo());
        // 校验手机号码唯一性
        validateMobileUnique(updateReqVO.getId(), updateReqVO.getMobile());

        // 更新
        PayeeInfoDO updateObj = BeanUtils.toBean(updateReqVO, PayeeInfoDO.class);
        // 身份证件号码没变就保留原有身份：改手机号是纠正联系方式，不是换一个人，更不能把别人的身份抢过来
        if (existing.getNaturalPersonId() != null && existing.getIdCardNo() != null
                && existing.getIdCardNo().equals(updateReqVO.getIdCardNo())) {
            updateObj.setNaturalPersonId(existing.getNaturalPersonId());
        } else {
            updateObj.setNaturalPersonId(registerNaturalPerson(updateReqVO.getName(), updateReqVO.getIdCardNo(),
                    updateReqVO.getMobile(), updateReqVO.getIdSignDate(), updateReqVO.getIdValidityPeriod()).getId());
        }
        payeeInfoMapper.updateById(updateObj);
    }

    @Override
    public void deletePayeeInfo(Long id) {
        // 校验存在
        validatePayeeInfoExists(id);
        // 删除
        payeeInfoMapper.deleteById(id);
    }

    private PayeeInfoDO validatePayeeInfoExists(Long id) {
        PayeeInfoDO payee = payeeInfoMapper.selectById(id);
        if (payee == null) {
            throw exception(PAYEE_NOT_EXISTS);
        }
        return payee;
    }

    /**
     * 身份登记：按身份证件号码取或建平台级自然人主体。
     *
     * <p>同一身份证已在别的回收企业建档、且手机号/姓名不一致时，这里会直接拒绝（不覆盖、不自动合并），
     * 由平台运营人工核实后认领（ADR 0017）。证件签发 / 截止日期是**平台级身份字段**，一并登记到主体上；
     * 主体上已填的值不会被覆盖，只在空缺处回填（见 {@code NaturalPersonServiceImpl#reuse}）。
     */
    private IcbcNaturalPersonDO registerNaturalPerson(String name, String idCardNo, String mobile,
                                                    String idSignDate, String idValidityPeriod) {
        NaturalPersonRegisterReqVO reqVO = new NaturalPersonRegisterReqVO();
        reqVO.setName(name);
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        reqVO.setIdSignDate(idSignDate);
        reqVO.setIdValidityPeriod(idValidityPeriod);
        return naturalPersonService.register(reqVO);
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
        
        // 请求里的 outUserId 是**平台级**外部用户编号，先对应到自然人主体（找不到就按身份登记建一个）
        IcbcNaturalPersonDO person = naturalPersonService.getByOutUserId(reqVO.getOutUserId());
        if (person == null) {
            person = registerNaturalPerson(reqVO.getReceiverName(), reqVO.getIdNo(), reqVO.getMobile(),
                    reqVO.getIdSignDate(), reqVO.getIdValidityPeriod());
        }
        payeeInfo.setNaturalPersonId(person.getId());

        // 模拟工行返回的收方编号
        payeeInfo.setPayeeNo("ICBC" + System.currentTimeMillis());
        // 收方档案编号（原 partnerPayeeId）：不再是工行 outUserId，outUserId 属于自然人主体
        if (payeeInfo.getPartnerPayeeId() == null) {
            payeeInfo.setPartnerPayeeId(generatePartnerPayeeId());
        }
        payeeInfo.setIcbcReceiverStatus("0"); // 初始状态为不可用
        
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
    public void handlePayeeAuditCallback(String outUserId, String auditStatus, String auditMsg) {
        // outUserId 是平台级外部用户编号：先找到自然人主体，再取本租户的收方档案
        IcbcNaturalPersonDO person = naturalPersonService.getByOutUserId(outUserId);
        PayeeInfoDO payeeInfo = person == null ? null : payeeInfoMapper.selectByNaturalPersonId(person.getId());
        if (payeeInfo == null) {
            log.warn("收方审核回调：未找到收方信息，outUserId={}", outUserId);
            return;
        }

        // 更新审核状态
        PayeeInfoDO updateObj = new PayeeInfoDO();
        updateObj.setId(payeeInfo.getId());
        updateObj.setAuditMsg(auditMsg);
        
        if ("1".equals(auditStatus)) {
            // 审核通过
            updateObj.setStatus(IcbcStatusEnum.AuditStatus.APPROVED.getStatus());
            updateObj.setIcbcReceiverStatus("1"); // 可用
        } else {
            // 审核拒绝
            updateObj.setStatus(IcbcStatusEnum.AuditStatus.REJECTED.getStatus());
            updateObj.setIcbcReceiverStatus("0"); // 不可用
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

    @Override
    public PayeeInfoDO getPayeeInfoByNaturalPersonId(Long naturalPersonId) {
        return payeeInfoMapper.selectByNaturalPersonId(naturalPersonId);
    }

    @Override
    public IcbcNaturalPersonDO ensureNaturalPerson(PayeeInfoDO payee) {
        if (StrUtil.isBlank(payee.getIdCardNo())) {
            // 没有身份证件号码就没有身份锚点：不能凭空建一个档案，也不能挂到别人身上
            throw exception(NATURAL_PERSON_ID_CARD_REQUIRED);
        }
        if (payee.getNaturalPersonId() != null) {
            List<IcbcNaturalPersonDO> found = naturalPersonService.getNaturalPersonList(
                    Collections.singletonList(payee.getNaturalPersonId()));
            if (!found.isEmpty()) {
                return found.get(0);
            }
        }
        IcbcNaturalPersonDO person = naturalPersonService.getByIdCardNo(payee.getIdCardNo());
        if (person == null) {
            person = registerNaturalPerson(payee.getName(), payee.getIdCardNo(), payee.getMobile(),
                    payee.getIdSignDate(), payee.getIdValidityPeriod());
        }
        if (!person.getId().equals(payee.getNaturalPersonId())) {
            PayeeInfoDO link = new PayeeInfoDO();
            link.setId(payee.getId());
            link.setNaturalPersonId(person.getId());
            payeeInfoMapper.updateById(link);
            payee.setNaturalPersonId(person.getId());
        }
        return person;
    }

    /**
     * 生成合作方收方编号
     */
    private String generatePartnerPayeeId() {
        return "PAYEE_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

} 