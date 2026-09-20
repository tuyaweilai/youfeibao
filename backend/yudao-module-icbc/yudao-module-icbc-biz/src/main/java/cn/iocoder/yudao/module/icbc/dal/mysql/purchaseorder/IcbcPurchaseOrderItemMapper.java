package cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 采购订单明细 Mapper（#46 T08）。
 */
@Mapper
public interface IcbcPurchaseOrderItemMapper extends BaseMapperX<IcbcPurchaseOrderItemDO> {

    default List<IcbcPurchaseOrderItemDO> selectListByOrderId(Long orderId) {
        if (orderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcPurchaseOrderItemDO>()
                .eq(IcbcPurchaseOrderItemDO::getOrderId, orderId)
                .orderByAsc(IcbcPurchaseOrderItemDO::getId));
    }

    default int deleteByOrderId(Long orderId) {
        return delete(IcbcPurchaseOrderItemDO::getOrderId, orderId);
    }

}
