package cn.iocoder.yudao.module.icbc.service.naturalperson.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonConflictRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonLoginDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.naturalperson.IcbcNaturalPersonLoginMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.naturalperson.IcbcNaturalPersonMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.NaturalPersonStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 自然人主体 Service 实现。
 *
 * <p>本表是平台级（非租户）表，所以这里的查询天然跨租户；这是**唯一**允许跨租户读取自然人身份的地方，
 * 与额度台账（{@code NaturalPersonQuotaServiceImpl}）同一性质，理由也一样：身份本来就是跨企业的（ADR 0017）。
 */
@Service
@Validated
@Slf4j
public class NaturalPersonServiceImpl implements NaturalPersonService {

    /** 绑定来源：本人注册 */
    public static final String SOURCE_REGISTER = "REGISTER";
    /** 绑定来源：平台运营人工认领 */
    public static final String SOURCE_OPS_CLAIM = "OPS_CLAIM";

    @Resource
    private IcbcNaturalPersonMapper naturalPersonMapper;
    @Resource
    private IcbcNaturalPersonLoginMapper naturalPersonLoginMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @Override
    public IcbcNaturalPersonDO register(@Valid NaturalPersonRegisterReqVO reqVO) {
        IcbcNaturalPersonDO existing = naturalPersonMapper.selectByIdCardNo(reqVO.getIdCardNo());
        if (existing != null) {
            return reuse(existing, reqVO);
        }
        IcbcNaturalPersonDO created = IcbcNaturalPersonDO.builder()
                .outUserId(generateOutUserId())
                .name(reqVO.getName())
                .idCardNo(reqVO.getIdCardNo())
                .mobile(reqVO.getMobile())
                .idSignDate(reqVO.getIdSignDate())
                .idValidityPeriod(reqVO.getIdValidityPeriod())
                .realNameStatus(PayeeRealNameStatusEnum.NOT_STARTED.getStatus())
                .status(NaturalPersonStatusEnum.NORMAL.getStatus())
                .build();
        try {
            naturalPersonMapper.insert(created);
        } catch (DuplicateKeyException e) {
            // 并发登记：唯一索引兜底，回读既有主体再走同一套「不覆盖」规则
            IcbcNaturalPersonDO concurrent = naturalPersonMapper.selectByIdCardNo(reqVO.getIdCardNo());
            if (concurrent == null) {
                throw e;
            }
            return reuse(concurrent, reqVO);
        }
        return created;
    }

    /**
     * 已有主体时的处置：一致则幂等返回，不一致则**拒绝**（不覆盖、不自动合并）。
     *
     * <p>这是 ADR 0017 的核心约束——自动合并等于让任何人用同一个身份证号接管他人的身份与收款。
     *
     * <p>证件签发 / 截止日期是这条约束的例外：它们不是身份锚点，只是身份字段的**补充**，
     * 所以只在空缺处回填（已有值不覆盖）。这样一来第二家回收企业建档时，第一家填过的值不会被本次
     * 向导里确认的值改写；反过来第一家没填、第二家填了，也补得进去。
     */
    private IcbcNaturalPersonDO reuse(IcbcNaturalPersonDO existing, NaturalPersonRegisterReqVO reqVO) {
        if (NaturalPersonStatusEnum.DISABLED.getStatus().equals(existing.getStatus())) {
            throw exception(NATURAL_PERSON_DISABLED);
        }
        if (conflicts(existing.getMobile(), reqVO.getMobile()) || conflicts(existing.getName(), reqVO.getName())) {
            throw exception(NATURAL_PERSON_IDENTITY_TAKEN);
        }
        return backfillIdValidity(existing, reqVO);
    }

    /**
     * 证件有效期只在空缺处回填：已有值不覆盖（与 {@link #reuse} 的「不覆盖」同一条理由）。
     */
    private IcbcNaturalPersonDO backfillIdValidity(IcbcNaturalPersonDO existing, NaturalPersonRegisterReqVO reqVO) {
        String signDate = StrUtil.isNotBlank(existing.getIdSignDate())
                ? existing.getIdSignDate() : StrUtil.trimToNull(reqVO.getIdSignDate());
        String validityPeriod = StrUtil.isNotBlank(existing.getIdValidityPeriod())
                ? existing.getIdValidityPeriod() : StrUtil.trimToNull(reqVO.getIdValidityPeriod());
        if (Objects.equals(signDate, existing.getIdSignDate())
                && Objects.equals(validityPeriod, existing.getIdValidityPeriod())) {
            return existing; // 没有可回填的空缺：不写库
        }
        IcbcNaturalPersonDO update = new IcbcNaturalPersonDO();
        update.setId(existing.getId());
        update.setIdSignDate(signDate);
        update.setIdValidityPeriod(validityPeriod);
        naturalPersonMapper.updateById(update);
        existing.setIdSignDate(signDate);
        existing.setIdValidityPeriod(validityPeriod);
        return existing;
    }

    private boolean conflicts(String registered, String incoming) {
        return StrUtil.isNotBlank(registered) && StrUtil.isNotBlank(incoming) && !registered.equals(incoming);
    }

    @Override
    public IcbcNaturalPersonDO getNaturalPerson(Long id) {
        IcbcNaturalPersonDO person = id == null ? null : naturalPersonMapper.selectById(id);
        if (person == null) {
            throw exception(NATURAL_PERSON_NOT_EXISTS);
        }
        return person;
    }

    @Override
    public IcbcNaturalPersonDO getByIdCardNo(String idCardNo) {
        return StrUtil.isBlank(idCardNo) ? null : naturalPersonMapper.selectByIdCardNo(idCardNo);
    }

    @Override
    public IcbcNaturalPersonDO getByOutUserId(String outUserId) {
        return StrUtil.isBlank(outUserId) ? null : naturalPersonMapper.selectByOutUserId(outUserId);
    }

    @Override
    public List<IcbcNaturalPersonDO> getNaturalPersonList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return naturalPersonMapper.selectListByIds(ids);
    }

    @Override
    public void markRealNamePending(Long naturalPersonId) {
        IcbcNaturalPersonDO person = getNaturalPerson(naturalPersonId);
        if (PayeeRealNameStatusEnum.PASSED.getStatus().equals(person.getRealNameStatus())) {
            return; // 已通过的不回退
        }
        IcbcNaturalPersonDO update = new IcbcNaturalPersonDO();
        update.setId(person.getId());
        update.setRealNameStatus(PayeeRealNameStatusEnum.PENDING.getStatus());
        naturalPersonMapper.updateById(update);
    }

    @Override
    public void applyRealNameResult(Long naturalPersonId, boolean passed, String failReason) {
        IcbcNaturalPersonDO person = getNaturalPerson(naturalPersonId);
        if (PayeeRealNameStatusEnum.PASSED.getStatus().equals(person.getRealNameStatus())) {
            // 已通过是终态：异步通知与主动查询都可能晚到，晚到的失败不许把「通过」改回去（#82 先到先写）
            return;
        }
        if (!passed && StrUtil.isBlank(failReason)) {
            return; // 认证中，不动状态
        }
        LambdaUpdateWrapper<IcbcNaturalPersonDO> update = new LambdaUpdateWrapper<IcbcNaturalPersonDO>()
                .eq(IcbcNaturalPersonDO::getId, person.getId());
        if (passed) {
            update.set(IcbcNaturalPersonDO::getRealNameStatus, PayeeRealNameStatusEnum.PASSED.getStatus())
                    .set(IcbcNaturalPersonDO::getRealNameMsg, null)
                    .set(IcbcNaturalPersonDO::getRealNameTime, LocalDateTime.now());
        } else {
            update.set(IcbcNaturalPersonDO::getRealNameStatus, PayeeRealNameStatusEnum.FAILED.getStatus())
                    .set(IcbcNaturalPersonDO::getRealNameMsg, failReason);
        }
        naturalPersonMapper.update(null, update);
    }

    // ==================== 登录凭证 ====================

    @Override
    public void bindLogin(Long naturalPersonId, Long memberUserId, String bindSource, String remark) {
        getNaturalPerson(naturalPersonId);
        if (memberUserId == null) {
            throw exception(NATURAL_PERSON_NOT_BOUND_TO_LOGIN);
        }
        if (naturalPersonLoginMapper.selectByNaturalPersonAndMember(naturalPersonId, memberUserId) != null) {
            return; // 幂等
        }
        naturalPersonLoginMapper.insert(IcbcNaturalPersonLoginDO.builder()
                .naturalPersonId(naturalPersonId)
                .memberUserId(memberUserId)
                .boundAt(LocalDateTime.now())
                .bindSource(bindSource)
                .remark(remark)
                .build());
    }

    @Override
    public void unbindLogin(Long naturalPersonId, Long memberUserId) {
        naturalPersonLoginMapper.deleteByNaturalPersonAndMember(naturalPersonId, memberUserId);
    }

    @Override
    public List<IcbcNaturalPersonDO> getNaturalPersonListByMemberUserId(Long memberUserId) {
        if (memberUserId == null) {
            return Collections.emptyList();
        }
        List<IcbcNaturalPersonLoginDO> bindings = naturalPersonLoginMapper.selectListByMemberUserId(memberUserId);
        if (bindings.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = bindings.stream().map(IcbcNaturalPersonLoginDO::getNaturalPersonId)
                .filter(Objects::nonNull).distinct().toList();
        return getNaturalPersonList(ids);
    }

    @Override
    public boolean isBoundToLogin(Long naturalPersonId, Long memberUserId) {
        if (naturalPersonId == null || memberUserId == null) {
            return false;
        }
        return naturalPersonLoginMapper.selectByNaturalPersonAndMember(naturalPersonId, memberUserId) != null;
    }

    // ==================== 平台运营 ====================

    @Override
    public PageResult<IcbcNaturalPersonDO> getNaturalPersonPage(NaturalPersonPageReqVO reqVO) {
        return naturalPersonMapper.selectPage(reqVO);
    }

    @Override
    public void updateStatus(Long id, Integer status, String remark) {
        getNaturalPerson(id);
        IcbcNaturalPersonDO update = new IcbcNaturalPersonDO();
        update.setId(id);
        update.setStatus(status);
        update.setRemark(remark);
        naturalPersonMapper.updateById(update);
    }

    // ==================== 身份冲突人工清单 ====================

    @Override
    public List<NaturalPersonConflictRespVO> getIdentityConflictList() {
        // 收方档案是租户级的，而冲突恰恰发生在不同租户之间：这里只读一次、跨租户扫一遍。
        // 本方法不做任何写操作，也不挑「谁是对的」——那正是 ADR 0017 要交给人处理的部分。
        List<PayeeInfoDO> payees = TenantUtils.executeIgnore(() -> payeeInfoMapper.selectList(
                new LambdaQueryWrapperX<PayeeInfoDO>()
                        .select(PayeeInfoDO::getId, PayeeInfoDO::getTenantId, PayeeInfoDO::getIdCardNo,
                                PayeeInfoDO::getName, PayeeInfoDO::getMobile, PayeeInfoDO::getPayeeNo,
                                PayeeInfoDO::getPartnerPayeeId, PayeeInfoDO::getCreateTime)
                        .isNotNull(PayeeInfoDO::getIdCardNo)
                        .ne(PayeeInfoDO::getIdCardNo, "")
                        .orderByAsc(PayeeInfoDO::getIdCardNo)
                        .orderByAsc(PayeeInfoDO::getId)));
        Map<String, List<PayeeInfoDO>> byIdCardNo = payees.stream()
                .collect(Collectors.groupingBy(PayeeInfoDO::getIdCardNo, LinkedHashMap::new, Collectors.toList()));
        return byIdCardNo.entrySet().stream()
                .filter(entry -> hasIdentityConflict(entry.getValue()))
                .map(entry -> toConflict(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 姓名或手机号在多个租户之间不一致即命中「不自动合并」规则。同一身份证完全一致的多条档案是正常的
     * （同一个人在多家企业卖货），不算冲突。
     */
    private boolean hasIdentityConflict(List<PayeeInfoDO> records) {
        Set<String> names = records.stream().map(PayeeInfoDO::getName)
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        Set<String> mobiles = records.stream().map(PayeeInfoDO::getMobile)
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        return names.size() > 1 || mobiles.size() > 1;
    }

    private NaturalPersonConflictRespVO toConflict(String idCardNo, List<PayeeInfoDO> records) {
        NaturalPersonConflictRespVO vo = new NaturalPersonConflictRespVO();
        // 与自然人主体页同一口径：平台运营看到的是「是谁」，不是可复制走的原始证件号
        vo.setIdCardNo(MaskUtils.maskIdCard(idCardNo));
        IcbcNaturalPersonDO person = getByIdCardNo(idCardNo);
        if (person != null) {
            vo.setNaturalPersonId(person.getId());
            vo.setOutUserId(person.getOutUserId());
            vo.setStatus(person.getStatus());
        }
        vo.setRecordCount(records.size());
        vo.setRecords(records.stream().map(this::toConflictRecord).toList());
        return vo;
    }

    private NaturalPersonConflictRespVO.Record toConflictRecord(PayeeInfoDO payee) {
        NaturalPersonConflictRespVO.Record record = new NaturalPersonConflictRespVO.Record();
        record.setPayeeId(payee.getId());
        record.setTenantId(payee.getTenantId());
        record.setName(payee.getName());
        record.setMobile(MaskUtils.maskMobile(payee.getMobile()));
        record.setPayeeNo(payee.getPayeeNo());
        record.setPartnerPayeeId(payee.getPartnerPayeeId());
        record.setCreateTime(payee.getCreateTime());
        return record;
    }

    /**
     * 生成平台级外部用户编号。生成后不变：实人认证、收方入驻、预下单、付款都用它。
     */
    private String generateOutUserId() {
        return "NP" + IdUtil.fastSimpleUUID().toUpperCase();
    }

}
