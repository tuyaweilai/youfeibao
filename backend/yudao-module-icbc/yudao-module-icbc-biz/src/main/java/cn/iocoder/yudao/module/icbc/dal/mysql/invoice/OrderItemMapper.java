package cn.iocoder.yudao.module.icbc.dal.mysql.invoice;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.OrderItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工行订单商品明细 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OrderItemMapper extends BaseMapperX<OrderItemDO> {

    /**
     * 根据订单ID查询商品明细列表
     *
     * @param orderId 订单ID
     * @return 商品明细列表
     */
    default List<OrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(OrderItemDO::getOrderId, orderId);
    }

    /**
     * 根据订单号查询商品明细列表
     *
     * @param orderNo 订单号
     * @return 商品明细列表
     */
    default List<OrderItemDO> selectListByOrderNo(String orderNo) {
        return selectList(OrderItemDO::getOrderNo, orderNo);
    }

    /**
     * 根据订单ID删除商品明细
     *
     * @param orderId 订单ID
     */
    default void deleteByOrderId(Long orderId) {
        delete(OrderItemDO::getOrderId, orderId);
    }

} 