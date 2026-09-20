package cn.iocoder.yudao.module.waste.convert.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderAllocationRecordCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.OrderAllocationRecordRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.OrderAllocationRecordDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 订单过磅分摊记录 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface OrderAllocationRecordConvert {

    OrderAllocationRecordConvert INSTANCE = Mappers.getMapper(OrderAllocationRecordConvert.class);

    OrderAllocationRecordDO convert(OrderAllocationRecordCreateReqVO bean);

    OrderAllocationRecordRespVO convert(OrderAllocationRecordDO bean);

    List<OrderAllocationRecordRespVO> convertList(List<OrderAllocationRecordDO> list);

    PageResult<OrderAllocationRecordRespVO> convertPage(PageResult<OrderAllocationRecordDO> page);

} 