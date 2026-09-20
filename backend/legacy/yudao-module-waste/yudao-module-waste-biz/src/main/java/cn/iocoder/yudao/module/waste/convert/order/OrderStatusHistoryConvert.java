package cn.iocoder.yudao.module.waste.convert.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderStatusHistoryCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderStatusHistoryRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderStatusHistoryDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 订单状态变更历史 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface OrderStatusHistoryConvert {

    OrderStatusHistoryConvert INSTANCE = Mappers.getMapper(OrderStatusHistoryConvert.class);

    OrderStatusHistoryDO convert(OrderStatusHistoryCreateReqVO bean);

    OrderStatusHistoryRespVO convert(OrderStatusHistoryDO bean);

    List<OrderStatusHistoryRespVO> convertList(List<OrderStatusHistoryDO> list);

    PageResult<OrderStatusHistoryRespVO> convertPage(PageResult<OrderStatusHistoryDO> page);

} 