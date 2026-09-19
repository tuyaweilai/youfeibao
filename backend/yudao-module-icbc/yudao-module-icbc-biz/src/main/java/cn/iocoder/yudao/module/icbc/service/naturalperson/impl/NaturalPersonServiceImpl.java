package cn.iocoder.yudao.module.icbc.service.naturalperson.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonLoginDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.naturalperson.IcbcNaturalPersonLoginMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.naturalperson.IcbcNaturalPersonMapper;
import cn.iocoder.yudao.module.icbc.enums.NaturalPersonStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
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
import java.util.List;
import java.util.Objects;

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
     */
    private IcbcNaturalPersonDO reuse(IcbcNaturalPersonDO existing, NaturalPersonRegisterReqVO reqVO) {
        if (NaturalPersonStatusEnum.DISABLED.getStatus().equals(existing.getStatus())) {
            throw exception(NATURAL_PERSON_DISABLED);
        }
        if (conflicts(existing.getMobile(), reqVO.getMobile()) || conflicts(existing.getName(), reqVO.getName())) {
            throw exception(NATURAL_PERSON_IDENTITY_TAKEN);
        }
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

    /**
     * 生成平台级外部用户编号。生成后不变：实人认证、收方入驻、预下单、付款都用它。
     */
    private String generateOutUserId() {
        return "NP" + IdUtil.fastSimpleUUID().toUpperCase();
    }

}
