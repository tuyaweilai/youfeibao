package cn.iocoder.yudao.module.icbc.service.goodscfg;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo.IcbcGoodsConfigPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo.IcbcGoodsConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;

import java.util.List;

/**
 * 品类与税收分类编码配置 Service 接口
 */
public interface IcbcGoodsConfigService {

    Long createGoodsConfig(IcbcGoodsConfigSaveReqVO createReqVO);

    void updateGoodsConfig(IcbcGoodsConfigSaveReqVO updateReqVO);

    void deleteGoodsConfig(Long id);

    IcbcGoodsConfigDO getGoodsConfig(Long id);

    PageResult<IcbcGoodsConfigDO> getGoodsConfigPage(IcbcGoodsConfigPageReqVO pageReqVO);

    /**
     * 启用的品类列表（供开票申请页选择）
     */
    List<IcbcGoodsConfigDO> getEnabledList();

}
