package cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDealDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 采购订单成交记录 Mapper（#46 T08）。记录只追加，不改不删。
 */
@Mapper
public interface IcbcPurchaseOrderDealMapper extends BaseMapperX<IcbcPurchaseOrderDealDO> {

    default List<IcbcPurchaseOrderDealDO> selectListByOrderId(Long orderId) {
        if (orderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcPurchaseOrderDealDO>()
                .eq(IcbcPurchaseOrderDealDO::getOrderId, orderId)
                .orderByAsc(IcbcPurchaseOrderDealDO::getId));
    }

    default List<IcbcPurchaseOrderDealDO> selectListByItemId(Long itemId) {
        if (itemId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcPurchaseOrderDealDO>()
                .eq(IcbcPurchaseOrderDealDO::getItemId, itemId)
                .orderByAsc(IcbcPurchaseOrderDealDO::getId));
    }

    /**
     * 按来源单据取成交记录（#58 幂等）：同一张收购单只允许写一条同类型的成交，
     * 重复登记 / 离线补传直接返回既有那条。
     */
    default List<IcbcPurchaseOrderDealDO> selectListBySource(String sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcPurchaseOrderDealDO>()
                .eq(IcbcPurchaseOrderDealDO::getSourceType, sourceType)
                .eq(IcbcPurchaseOrderDealDO::getSourceId, sourceId)
                .orderByAsc(IcbcPurchaseOrderDealDO::getId));
    }

}
