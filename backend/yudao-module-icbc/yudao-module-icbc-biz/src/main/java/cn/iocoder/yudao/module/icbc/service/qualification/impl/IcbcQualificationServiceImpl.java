package cn.iocoder.yudao.module.icbc.service.qualification.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo.IcbcQualificationPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo.IcbcQualificationSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.qualification.IcbcQualificationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.qualification.IcbcQualificationMapper;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.QUALIFICATION_NOT_EXISTS;

/**
 * 租户三层资质 Service 实现
 */
@Service
@Validated
public class IcbcQualificationServiceImpl implements IcbcQualificationService {

    /** 三层资质 */
    private static final Set<String> ALL_LAYERS = Set.of("TAX", "INDUSTRY", "PUBLIC_SECURITY");

    @Resource
    private IcbcQualificationMapper qualificationMapper;

    @Override
    public Long createQualification(IcbcQualificationSaveReqVO createReqVO) {
        IcbcQualificationDO qualification = BeanUtils.toBean(createReqVO, IcbcQualificationDO.class);
        qualificationMapper.insert(qualification);
        return qualification.getId();
    }

    @Override
    public void updateQualification(IcbcQualificationSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        qualificationMapper.updateById(BeanUtils.toBean(updateReqVO, IcbcQualificationDO.class));
    }

    @Override
    public void deleteQualification(Long id) {
        validateExists(id);
        qualificationMapper.deleteById(id);
    }

    @Override
    public IcbcQualificationDO getQualification(Long id) {
        return qualificationMapper.selectById(id);
    }

    @Override
    public PageResult<IcbcQualificationDO> getQualificationPage(IcbcQualificationPageReqVO pageReqVO) {
        return qualificationMapper.selectPage(pageReqVO);
    }

    @Override
    public List<IcbcQualificationDO> getQualificationList() {
        return qualificationMapper.selectList();
    }

    @Override
    public List<IcbcQualificationDO> getExpiringList(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);
        return qualificationMapper.selectList().stream()
                .filter(q -> Integer.valueOf(1).equals(q.getStatus()))
                .filter(q -> q.getValidTo() != null && !q.getValidTo().isBefore(today)
                        && !q.getValidTo().isAfter(deadline))
                .toList();
    }

    @Override
    public boolean isTenantReady() {
        LocalDate today = LocalDate.now();
        Set<String> readyLayers = qualificationMapper.selectList().stream()
                .filter(q -> Integer.valueOf(1).equals(q.getStatus()))
                .filter(q -> q.getValidTo() == null || !q.getValidTo().isBefore(today))
                .map(IcbcQualificationDO::getType)
                .collect(java.util.stream.Collectors.toSet());
        return readyLayers.containsAll(ALL_LAYERS);
    }

    private void validateExists(Long id) {
        if (id == null || qualificationMapper.selectById(id) == null) {
            throw exception(QUALIFICATION_NOT_EXISTS);
        }
    }

}
