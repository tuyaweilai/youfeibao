package cn.iocoder.yudao.module.icbc.dal.mysql.purchasecontract;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract.IcbcPurchaseContractCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 采购合同适用品类 Mapper（#45 / T07）。
 */
@Mapper
public interface IcbcPurchaseContractCategoryMapper extends BaseMapperX<IcbcPurchaseContractCategoryDO> {

    default List<IcbcPurchaseContractCategoryDO> selectListByContractId(Long contractId) {
        if (contractId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcPurchaseContractCategoryDO>()
                .eq(IcbcPurchaseContractCategoryDO::getContractId, contractId)
                .orderByAsc(IcbcPurchaseContractCategoryDO::getId));
    }

    default int deleteByContractId(Long contractId) {
        return delete(IcbcPurchaseContractCategoryDO::getContractId, contractId);
    }

}
