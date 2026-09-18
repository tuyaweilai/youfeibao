package cn.iocoder.yudao.module.icbc.service.scrapcode.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo.IcbcScrapCodePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo.IcbcScrapCodeSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.scrapcode.IcbcScrapCodeDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.scrapcode.IcbcScrapCodeMapper;
import cn.iocoder.yudao.module.icbc.service.scrapcode.IcbcScrapCodeService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.SCRAP_CODE_MERGED_CODE_EXISTS;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.SCRAP_CODE_NOT_EXISTS;

/**
 * 平台级报废产品税收分类编码 Service 实现
 */
@Service
@Validated
public class IcbcScrapCodeServiceImpl implements IcbcScrapCodeService {

    @Resource
    private IcbcScrapCodeMapper scrapCodeMapper;

    @Override
    public Long createScrapCode(IcbcScrapCodeSaveReqVO createReqVO) {
        validateMergedCodeUnique(null, createReqVO.getMergedCode());
        IcbcScrapCodeDO scrapCode = BeanUtils.toBean(createReqVO, IcbcScrapCodeDO.class);
        scrapCodeMapper.insert(scrapCode);
        return scrapCode.getId();
    }

    @Override
    public void updateScrapCode(IcbcScrapCodeSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateMergedCodeUnique(updateReqVO.getId(), updateReqVO.getMergedCode());
        scrapCodeMapper.updateById(BeanUtils.toBean(updateReqVO, IcbcScrapCodeDO.class));
    }

    @Override
    public void deleteScrapCode(Long id) {
        validateExists(id);
        scrapCodeMapper.deleteById(id);
    }

    @Override
    public IcbcScrapCodeDO getScrapCode(Long id) {
        return scrapCodeMapper.selectById(id);
    }

    @Override
    public PageResult<IcbcScrapCodeDO> getScrapCodePage(IcbcScrapCodePageReqVO pageReqVO) {
        return scrapCodeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<IcbcScrapCodeDO> getEnabledList() {
        return scrapCodeMapper.selectEnabledList();
    }

    private void validateExists(Long id) {
        if (id == null || scrapCodeMapper.selectById(id) == null) {
            throw exception(SCRAP_CODE_NOT_EXISTS);
        }
    }

    private void validateMergedCodeUnique(Long id, String mergedCode) {
        IcbcScrapCodeDO existing = scrapCodeMapper.selectByMergedCode(mergedCode);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(SCRAP_CODE_MERGED_CODE_EXISTS);
        }
    }

}
