package cn.iocoder.yudao.module.icbc.service.goodscfg.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo.IcbcGoodsConfigPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo.IcbcGoodsConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.GOODS_CONFIG_NAME_EXISTS;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.GOODS_CONFIG_NOT_EXISTS;

/**
 * 品类与税收分类编码配置 Service 实现
 */
@Service
@Validated
public class IcbcGoodsConfigServiceImpl implements IcbcGoodsConfigService {

    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;

    @Override
    public Long createGoodsConfig(IcbcGoodsConfigSaveReqVO createReqVO) {
        validateNameUnique(null, createReqVO.getName());
        IcbcGoodsConfigDO config = BeanUtils.toBean(createReqVO, IcbcGoodsConfigDO.class);
        goodsConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateGoodsConfig(IcbcGoodsConfigSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateNameUnique(updateReqVO.getId(), updateReqVO.getName());
        goodsConfigMapper.updateById(BeanUtils.toBean(updateReqVO, IcbcGoodsConfigDO.class));
    }

    @Override
    public void deleteGoodsConfig(Long id) {
        validateExists(id);
        goodsConfigMapper.deleteById(id);
    }

    @Override
    public IcbcGoodsConfigDO getGoodsConfig(Long id) {
        return goodsConfigMapper.selectById(id);
    }

    @Override
    public PageResult<IcbcGoodsConfigDO> getGoodsConfigPage(IcbcGoodsConfigPageReqVO pageReqVO) {
        return goodsConfigMapper.selectPage(pageReqVO);
    }

    @Override
    public List<IcbcGoodsConfigDO> getEnabledList() {
        return goodsConfigMapper.selectEnabledList();
    }

    private void validateExists(Long id) {
        if (id == null || goodsConfigMapper.selectById(id) == null) {
            throw exception(GOODS_CONFIG_NOT_EXISTS);
        }
    }

    private void validateNameUnique(Long id, String name) {
        IcbcGoodsConfigDO existing = goodsConfigMapper.selectByName(name);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(GOODS_CONFIG_NAME_EXISTS);
        }
    }

}
