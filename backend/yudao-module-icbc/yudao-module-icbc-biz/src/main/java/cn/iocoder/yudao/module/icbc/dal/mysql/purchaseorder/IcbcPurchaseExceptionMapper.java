package cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseExceptionDO;
import cn.iocoder.yudao.module.icbc.enums.PurchaseExceptionStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 履约异常授权单 Mapper（#47 T09）。
 *
 * <p>交货门禁（{@code PurchaseOrderServiceImpl#checkDelivery}）只读这里：找「已通过」的授权单，
 * 再按授权范围（追加量 / 有效期 / 场站）判断是否覆盖本次交货。
 */
@Mapper
public interface IcbcPurchaseExceptionMapper extends BaseMapperX<IcbcPurchaseExceptionDO> {

    default IcbcPurchaseExceptionDO selectByExceptionNo(String exceptionNo) {
        return selectOne(IcbcPurchaseExceptionDO::getExceptionNo, exceptionNo);
    }

    default List<IcbcPurchaseExceptionDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<IcbcPurchaseExceptionDO>()
                .eq(IcbcPurchaseExceptionDO::getOrderId, orderId)
                .orderByDesc(IcbcPurchaseExceptionDO::getId));
    }

    /**
     * 某订单某异常类型下、处于指定状态的全部授权单。
     */
    default List<IcbcPurchaseExceptionDO> selectListByOrderIdAndTypeAndStatus(Long orderId, String exceptionType,
                                                                             Integer status) {
        return selectList(new LambdaQueryWrapperX<IcbcPurchaseExceptionDO>()
                .eq(IcbcPurchaseExceptionDO::getOrderId, orderId)
                .eq(IcbcPurchaseExceptionDO::getExceptionType, exceptionType)
                .eq(IcbcPurchaseExceptionDO::getStatus, status)
                .orderByAsc(IcbcPurchaseExceptionDO::getId));
    }

    /**
     * 某订单是否有待审核的授权申请（同一异常不重复提交）。
     */
    default IcbcPurchaseExceptionDO selectPending(Long orderId, String exceptionType, Long itemId) {
        List<IcbcPurchaseExceptionDO> list = selectList(new LambdaQueryWrapperX<IcbcPurchaseExceptionDO>()
                .eq(IcbcPurchaseExceptionDO::getOrderId, orderId)
                .eq(IcbcPurchaseExceptionDO::getExceptionType, exceptionType)
                .eq(IcbcPurchaseExceptionDO::getStatus, PurchaseExceptionStatusEnum.PENDING.getStatus())
                .orderByAsc(IcbcPurchaseExceptionDO::getId));
        // 明细为空按「订单级申请」看；有明细则只看同一明细
        return list.stream()
                .filter(item -> item.getItemId() == null
                        || (itemId != null && item.getItemId().equals(itemId)))
                .findFirst().orElse(null);
    }

    default PageResult<IcbcPurchaseExceptionDO> selectPage(PurchaseOrderExceptionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcPurchaseExceptionDO>()
                .eqIfPresent(IcbcPurchaseExceptionDO::getOrderId, reqVO.getOrderId())
                .likeIfPresent(IcbcPurchaseExceptionDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(IcbcPurchaseExceptionDO::getExceptionType, reqVO.getExceptionType())
                .eqIfPresent(IcbcPurchaseExceptionDO::getStatus, reqVO.getStatus())
                .orderByDesc(IcbcPurchaseExceptionDO::getId));
    }

}
