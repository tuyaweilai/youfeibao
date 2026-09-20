package cn.iocoder.yudao.module.waste.convert.price;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.RecyclerPriceConfigDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 回收企业价格配置 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface RecyclerPriceConfigConvert {

    RecyclerPriceConfigConvert INSTANCE = Mappers.getMapper(RecyclerPriceConfigConvert.class);

    RecyclerPriceConfigDO convert(RecyclerPriceConfigCreateReqVO bean);

    RecyclerPriceConfigDO convert(RecyclerPriceConfigUpdateReqVO bean);

    RecyclerPriceConfigRespVO convert(RecyclerPriceConfigDO bean);

    List<RecyclerPriceConfigRespVO> convertList(List<RecyclerPriceConfigDO> list);

    PageResult<RecyclerPriceConfigRespVO> convertPage(PageResult<RecyclerPriceConfigDO> page);

} 