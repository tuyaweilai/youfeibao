package cn.iocoder.yudao.module.waste.convert.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.TransferOrderCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.TransferOrderRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.order.vo.TransferOrderUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.order.TransferOrderDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 危废转移订单 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface TransferOrderConvert {

    TransferOrderConvert INSTANCE = Mappers.getMapper(TransferOrderConvert.class);

    TransferOrderDO convert(TransferOrderCreateReqVO bean);

    TransferOrderDO convert(TransferOrderUpdateReqVO bean);

    TransferOrderRespVO convert(TransferOrderDO bean);

    List<TransferOrderRespVO> convertList(List<TransferOrderDO> list);

    PageResult<TransferOrderRespVO> convertPage(PageResult<TransferOrderDO> page);

} 