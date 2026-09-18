package cn.iocoder.yudao.module.enterprise.convert;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.EnterpriseStoreCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.EnterpriseStoreRespVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.EnterpriseStoreTreeRespVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.EnterpriseStoreUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseStoreDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 企业门店 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface EnterpriseStoreConvert {

    EnterpriseStoreConvert INSTANCE = Mappers.getMapper(EnterpriseStoreConvert.class);

    EnterpriseStoreDO convert(EnterpriseStoreCreateReqVO bean);

    EnterpriseStoreDO convert(EnterpriseStoreUpdateReqVO bean);

    EnterpriseStoreRespVO convert(EnterpriseStoreDO bean);

    List<EnterpriseStoreRespVO> convertList(List<EnterpriseStoreDO> list);

    PageResult<EnterpriseStoreRespVO> convertPage(PageResult<EnterpriseStoreDO> page);

    EnterpriseStoreTreeRespVO convertTree(EnterpriseStoreDO bean);

    List<EnterpriseStoreTreeRespVO> convertTreeList(List<EnterpriseStoreDO> list);

} 