package cn.iocoder.yudao.module.icbc.dal.mysql.settlement;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 结算单版本 Mapper。版本只追加、不覆盖，当前生效版本由结算单的指针指。
 */
@Mapper
public interface IcbcSettlementVersionMapper extends BaseMapperX<IcbcSettlementVersionDO> {

    default List<IcbcSettlementVersionDO> selectListBySettlementId(Long settlementId) {
        if (settlementId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcSettlementVersionDO>()
                .eq(IcbcSettlementVersionDO::getSettlementId, settlementId)
                .orderByDesc(IcbcSettlementVersionDO::getVersionNo));
    }

    default IcbcSettlementVersionDO selectBySettlementIdAndVersionNo(Long settlementId, Integer versionNo) {
        return selectOne(new LambdaQueryWrapperX<IcbcSettlementVersionDO>()
                .eq(IcbcSettlementVersionDO::getSettlementId, settlementId)
                .eq(IcbcSettlementVersionDO::getVersionNo, versionNo));
    }

    default int selectMaxVersionNo(Long settlementId) {
        IcbcSettlementVersionDO latest = selectOne(new LambdaQueryWrapperX<IcbcSettlementVersionDO>()
                .eq(IcbcSettlementVersionDO::getSettlementId, settlementId)
                .orderByDesc(IcbcSettlementVersionDO::getVersionNo)
                .last("LIMIT 1"));
        return latest == null ? 0 : latest.getVersionNo();
    }

}
