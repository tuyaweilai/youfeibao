package cn.iocoder.yudao.module.icbc.service.scrapcode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo.IcbcScrapCodePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo.IcbcScrapCodeSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.scrapcode.IcbcScrapCodeDO;

import java.util.List;

/**
 * 平台级报废产品税收分类编码 Service 接口。
 *
 * <p>编码表跨租户共享：平台运营维护，租户只读用于选编码。
 */
public interface IcbcScrapCodeService {

    Long createScrapCode(IcbcScrapCodeSaveReqVO createReqVO);

    void updateScrapCode(IcbcScrapCodeSaveReqVO updateReqVO);

    void deleteScrapCode(Long id);

    IcbcScrapCodeDO getScrapCode(Long id);

    PageResult<IcbcScrapCodeDO> getScrapCodePage(IcbcScrapCodePageReqVO pageReqVO);

    /**
     * 启用的编码列表（供租户配置品类时选择）
     */
    List<IcbcScrapCodeDO> getEnabledList();

}
