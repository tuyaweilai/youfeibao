package cn.iocoder.yudao.module.icbc.dal.mysql.purchasecontract;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract.IcbcPurchaseContractVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 采购合同版本 Mapper（#45 / T07）。版本只追加、不覆盖。
 */
@Mapper
public interface IcbcPurchaseContractVersionMapper extends BaseMapperX<IcbcPurchaseContractVersionDO> {

    default List<IcbcPurchaseContractVersionDO> selectListByContractId(Long contractId) {
        if (contractId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcPurchaseContractVersionDO>()
                .eq(IcbcPurchaseContractVersionDO::getContractId, contractId)
                .orderByDesc(IcbcPurchaseContractVersionDO::getVersionNo));
    }

    default IcbcPurchaseContractVersionDO selectLatest(Long contractId) {
        return selectOne(new LambdaQueryWrapperX<IcbcPurchaseContractVersionDO>()
                .eq(IcbcPurchaseContractVersionDO::getContractId, contractId)
                .orderByDesc(IcbcPurchaseContractVersionDO::getVersionNo)
                .last("LIMIT 1"));
    }

    default int deleteByContractId(Long contractId) {
        return delete(IcbcPurchaseContractVersionDO::getContractId, contractId);
    }

}
