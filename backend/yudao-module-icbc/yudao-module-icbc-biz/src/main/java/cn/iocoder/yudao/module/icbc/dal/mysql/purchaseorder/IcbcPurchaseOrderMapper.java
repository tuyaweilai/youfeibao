package cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购订单 Mapper（#46 T08）。
 */
@Mapper
public interface IcbcPurchaseOrderMapper extends BaseMapperX<IcbcPurchaseOrderDO> {

    default IcbcPurchaseOrderDO selectByOrderNo(String orderNo) {
        return selectOne(IcbcPurchaseOrderDO::getOrderNo, orderNo);
    }

    default PageResult<IcbcPurchaseOrderDO> selectPage(PurchaseOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcPurchaseOrderDO>()
                .likeIfPresent(IcbcPurchaseOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(IcbcPurchaseOrderDO::getContractId, reqVO.getContractId())
                .likeIfPresent(IcbcPurchaseOrderDO::getCounterpartyName, reqVO.getCounterpartyName())
                .eqIfPresent(IcbcPurchaseOrderDO::getCounterpartyType, reqVO.getCounterpartyType())
                .eqIfPresent(IcbcPurchaseOrderDO::getStationId, reqVO.getStationId())
                .eqIfPresent(IcbcPurchaseOrderDO::getStatus, reqVO.getStatus())
                .orderByDesc(IcbcPurchaseOrderDO::getId));
    }

}
