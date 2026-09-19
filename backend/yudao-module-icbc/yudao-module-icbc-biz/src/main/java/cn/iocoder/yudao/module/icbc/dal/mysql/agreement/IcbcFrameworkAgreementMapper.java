package cn.iocoder.yudao.module.icbc.dal.mysql.agreement;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 框架收购协议 Mapper
 */
@Mapper
public interface IcbcFrameworkAgreementMapper extends BaseMapperX<IcbcFrameworkAgreementDO> {

    default IcbcFrameworkAgreementDO selectActiveByPayeeId(Long payeeId) {
        return selectOne(new LambdaQueryWrapperX<IcbcFrameworkAgreementDO>()
                .eq(IcbcFrameworkAgreementDO::getPayeeId, payeeId)
                .eq(IcbcFrameworkAgreementDO::getStatus, 1)
                .orderByDesc(IcbcFrameworkAgreementDO::getId)
                .last("LIMIT 1"));
    }

    default List<IcbcFrameworkAgreementDO> selectListByPayeeId(Long payeeId) {
        return selectList(new LambdaQueryWrapperX<IcbcFrameworkAgreementDO>()
                .eq(IcbcFrameworkAgreementDO::getPayeeId, payeeId)
                .orderByDesc(IcbcFrameworkAgreementDO::getId));
    }

    /**
     * 批量查询待签署（status=0）的协议：自然人端首页「待我确认」用。
     */
    default List<IcbcFrameworkAgreementDO> selectPendingByPayeeIds(Collection<Long> payeeIds) {
        if (payeeIds == null || payeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcFrameworkAgreementDO>()
                .in(IcbcFrameworkAgreementDO::getPayeeId, payeeIds)
                .eq(IcbcFrameworkAgreementDO::getStatus, 0)
                .orderByDesc(IcbcFrameworkAgreementDO::getId));
    }

}
