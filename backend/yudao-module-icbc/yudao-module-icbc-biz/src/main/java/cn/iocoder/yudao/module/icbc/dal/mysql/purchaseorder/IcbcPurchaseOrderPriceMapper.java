package cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderPriceDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 采购订单交货日价格表 Mapper（#46 T08）。
 */
@Mapper
public interface IcbcPurchaseOrderPriceMapper extends BaseMapperX<IcbcPurchaseOrderPriceDO> {

    default List<IcbcPurchaseOrderPriceDO> selectListByOrderId(Long orderId) {
        if (orderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcPurchaseOrderPriceDO>()
                .eq(IcbcPurchaseOrderPriceDO::getOrderId, orderId)
                .orderByAsc(IcbcPurchaseOrderPriceDO::getItemId)
                .orderByAsc(IcbcPurchaseOrderPriceDO::getDeliveryDate)
                .orderByAsc(IcbcPurchaseOrderPriceDO::getId));
    }

    default List<IcbcPurchaseOrderPriceDO> selectListByItemId(Long itemId) {
        if (itemId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcPurchaseOrderPriceDO>()
                .eq(IcbcPurchaseOrderPriceDO::getItemId, itemId)
                .orderByAsc(IcbcPurchaseOrderPriceDO::getDeliveryDate)
                .orderByAsc(IcbcPurchaseOrderPriceDO::getId));
    }

    /**
     * 取该明细在给定交货日生效的价：**交货日不晚于 {@code deliveryDate} 的最新一条**。
     */
    default IcbcPurchaseOrderPriceDO selectEffectiveByItemId(Long itemId, LocalDate deliveryDate) {
        if (itemId == null || deliveryDate == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<IcbcPurchaseOrderPriceDO>()
                .eq(IcbcPurchaseOrderPriceDO::getItemId, itemId)
                .le(IcbcPurchaseOrderPriceDO::getDeliveryDate, deliveryDate)
                .orderByDesc(IcbcPurchaseOrderPriceDO::getDeliveryDate)
                .orderByDesc(IcbcPurchaseOrderPriceDO::getId)
                .last("LIMIT 1"));
    }

    default int deleteByOrderId(Long orderId) {
        return delete(IcbcPurchaseOrderPriceDO::getOrderId, orderId);
    }

}
