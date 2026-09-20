package cn.iocoder.yudao.module.waste.convert.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderPriceAdjustmentCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderPriceAdjustmentRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderPriceAdjustmentDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 订单价格调整记录 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface OrderPriceAdjustmentConvert {

    OrderPriceAdjustmentConvert INSTANCE = Mappers.getMapper(OrderPriceAdjustmentConvert.class);

    OrderPriceAdjustmentDO convert(OrderPriceAdjustmentCreateReqVO bean);

    OrderPriceAdjustmentRespVO convert(OrderPriceAdjustmentDO bean);

    List<OrderPriceAdjustmentRespVO> convertList(List<OrderPriceAdjustmentDO> list);

    PageResult<OrderPriceAdjustmentRespVO> convertPage(PageResult<OrderPriceAdjustmentDO> page);

} 