package cn.iocoder.yudao.module.icbc.dal.mysql.purchasecontract;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract.IcbcPurchaseContractDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购合同 Mapper（#45 / T07）。
 */
@Mapper
public interface IcbcPurchaseContractMapper extends BaseMapperX<IcbcPurchaseContractDO> {

    default IcbcPurchaseContractDO selectByContractNo(String contractNo) {
        return selectOne(IcbcPurchaseContractDO::getContractNo, contractNo);
    }

    default PageResult<IcbcPurchaseContractDO> selectPage(PurchaseContractPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcPurchaseContractDO>()
                .likeIfPresent(IcbcPurchaseContractDO::getContractNo, reqVO.getContractNo())
                .likeIfPresent(IcbcPurchaseContractDO::getName, reqVO.getName())
                .likeIfPresent(IcbcPurchaseContractDO::getCounterpartyName, reqVO.getCounterpartyName())
                .eqIfPresent(IcbcPurchaseContractDO::getCounterpartyType, reqVO.getCounterpartyType())
                .eqIfPresent(IcbcPurchaseContractDO::getStatus, reqVO.getStatus())
                .orderByDesc(IcbcPurchaseContractDO::getId));
    }

}
