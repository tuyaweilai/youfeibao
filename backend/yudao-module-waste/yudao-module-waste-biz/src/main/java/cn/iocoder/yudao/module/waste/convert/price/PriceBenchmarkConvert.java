package cn.iocoder.yudao.module.waste.convert.price;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.PriceBenchmarkDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 危险废物市场价格基准 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface PriceBenchmarkConvert {

    PriceBenchmarkConvert INSTANCE = Mappers.getMapper(PriceBenchmarkConvert.class);

    @Mapping(source = "benchmarkPrice", target = "price")
    @Mapping(source = "expiryDate", target = "expireDate")
    @Mapping(source = "source", target = "priceSource")
    PriceBenchmarkDO convert(PriceBenchmarkCreateReqVO bean);

    @Mapping(source = "benchmarkPrice", target = "price")
    @Mapping(source = "expiryDate", target = "expireDate")
    @Mapping(source = "source", target = "priceSource")
    PriceBenchmarkDO convert(PriceBenchmarkUpdateReqVO bean);

    @Mapping(source = "price", target = "benchmarkPrice")
    @Mapping(source = "expireDate", target = "expiryDate")
    @Mapping(source = "priceSource", target = "source")
    PriceBenchmarkRespVO convert(PriceBenchmarkDO bean);

    List<PriceBenchmarkRespVO> convertList(List<PriceBenchmarkDO> list);

    PageResult<PriceBenchmarkRespVO> convertPage(PageResult<PriceBenchmarkDO> page);

} 