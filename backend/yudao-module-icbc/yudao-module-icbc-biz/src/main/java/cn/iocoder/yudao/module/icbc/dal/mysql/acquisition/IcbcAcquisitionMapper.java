package cn.iocoder.yudao.module.icbc.dal.mysql.acquisition;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo.AcquisitionPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 收购登记单 Mapper
 */
@Mapper
public interface IcbcAcquisitionMapper extends BaseMapperX<IcbcAcquisitionDO> {

    default IcbcAcquisitionDO selectByAcquisitionNo(String acquisitionNo) {
        return selectOne(IcbcAcquisitionDO::getAcquisitionNo, acquisitionNo);
    }

    /**
     * 按客户端幂等键查重。离线补传时同一笔会被服务端接到多次，用这个键收敛成一条。
     */
    default IcbcAcquisitionDO selectByClientRequestId(String clientRequestId) {
        return selectOne(IcbcAcquisitionDO::getClientRequestId, clientRequestId);
    }

    default List<IcbcAcquisitionDO> selectListByPayeeId(Long payeeId) {
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .eq(IcbcAcquisitionDO::getPayeeId, payeeId)
                .orderByDesc(IcbcAcquisitionDO::getId));
    }

    /**
     * 按开票的合作方订单号反查收购单（一票一档的合同流 / 货物流 / 信息流来自这里）。
     */
    default List<IcbcAcquisitionDO> selectListByInvoicePartnerOrderIds(Collection<String> partnerOrderIds) {
        if (partnerOrderIds == null || partnerOrderIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .in(IcbcAcquisitionDO::getInvoicePartnerOrderId, partnerOrderIds));
    }

    default PageResult<IcbcAcquisitionDO> selectPage(AcquisitionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .eqIfPresent(IcbcAcquisitionDO::getPayeeId, reqVO.getPayeeId())
                .eqIfPresent(IcbcAcquisitionDO::getStatus, reqVO.getStatus())
                .likeIfPresent(IcbcAcquisitionDO::getAcquisitionNo, reqVO.getAcquisitionNo())
                .likeIfPresent(IcbcAcquisitionDO::getSellerName, reqVO.getSellerName())
                .eqIfPresent(IcbcAcquisitionDO::getVehiclePlateNo, reqVO.getVehiclePlateNo())
                .betweenIfPresent(IcbcAcquisitionDO::getTradeTime, reqVO.getTradeTime())
                .orderByDesc(IcbcAcquisitionDO::getId));
    }

}
