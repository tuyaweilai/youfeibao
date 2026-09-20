package cn.iocoder.yudao.module.waste.convert.recycler;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPriceCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPriceRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPriceUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerCustomerPriceDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 回收企业客户专属价格配置 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface RecyclerCustomerPriceConvert {

    RecyclerCustomerPriceConvert INSTANCE = Mappers.getMapper(RecyclerCustomerPriceConvert.class);

    RecyclerCustomerPriceDO convert(RecyclerCustomerPriceCreateReqVO bean);

    RecyclerCustomerPriceDO convert(RecyclerCustomerPriceUpdateReqVO bean);

    RecyclerCustomerPriceRespVO convert(RecyclerCustomerPriceDO bean);

    List<RecyclerCustomerPriceRespVO> convertList(List<RecyclerCustomerPriceDO> list);

    PageResult<RecyclerCustomerPriceRespVO> convertPage(PageResult<RecyclerCustomerPriceDO> page);

} 