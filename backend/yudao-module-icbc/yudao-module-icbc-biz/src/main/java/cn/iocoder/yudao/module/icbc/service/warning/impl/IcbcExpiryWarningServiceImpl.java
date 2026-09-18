package cn.iocoder.yudao.module.icbc.service.warning.impl;

import cn.iocoder.yudao.module.icbc.dal.dataobject.qualification.IcbcQualificationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.warning.IcbcExpiryWarningDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.warning.IcbcExpiryWarningMapper;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import cn.iocoder.yudao.module.icbc.service.warning.IcbcExpiryWarningService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.EXPIRY_WARNING_NOT_EXISTS;

/**
 * 资质到期预警 Service 实现
 */
@Service
@Validated
public class IcbcExpiryWarningServiceImpl implements IcbcExpiryWarningService {

    /** 待处理 */
    private static final int STATUS_OPEN = 0;
    /** 已处理 */
    private static final int STATUS_ACKNOWLEDGED = 1;

    @Resource
    private IcbcExpiryWarningMapper expiryWarningMapper;

    @Resource
    private IcbcQualificationService qualificationService;

    @Override
    public int scan(int days) {
        List<IcbcQualificationDO> expiring = qualificationService.getExpiringList(days);
        int created = 0;
        for (IcbcQualificationDO qualification : expiring) {
            if (expiryWarningMapper.selectOpenByQualificationId(qualification.getId()) != null) {
                continue; // 已有未处理预警，幂等跳过
            }
            IcbcExpiryWarningDO warning = new IcbcExpiryWarningDO();
            warning.setQualificationId(qualification.getId());
            warning.setType(qualification.getType());
            warning.setName(qualification.getName());
            warning.setValidTo(qualification.getValidTo());
            warning.setStatus(STATUS_OPEN);
            warning.setWarnedAt(LocalDateTime.now());
            expiryWarningMapper.insert(warning);
            created++;
        }
        return created;
    }

    @Override
    public List<IcbcExpiryWarningDO> getOpenList() {
        return expiryWarningMapper.selectOpenList();
    }

    @Override
    public void acknowledge(Long id) {
        IcbcExpiryWarningDO warning = expiryWarningMapper.selectById(id);
        if (warning == null) {
            throw exception(EXPIRY_WARNING_NOT_EXISTS);
        }
        IcbcExpiryWarningDO updateObj = new IcbcExpiryWarningDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_ACKNOWLEDGED);
        expiryWarningMapper.updateById(updateObj);
    }

}
