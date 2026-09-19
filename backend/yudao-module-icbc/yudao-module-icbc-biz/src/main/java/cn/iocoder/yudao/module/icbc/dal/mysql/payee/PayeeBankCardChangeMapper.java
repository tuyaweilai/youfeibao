package cn.iocoder.yudao.module.icbc.dal.mysql.payee;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.IcbcPayeeBankCardChangeDO;
import cn.iocoder.yudao.module.icbc.enums.PayeeBankCardChangeStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 出售者换银行卡 Mapper。
 *
 * <p>「同一收方同一时刻最多一条在途」由服务层保证；查询侧只提供按在途状态取的那一个入口，
 * 免得调用方各写一套条件。
 */
@Mapper
public interface PayeeBankCardChangeMapper extends BaseMapperX<IcbcPayeeBankCardChangeDO> {

    /**
     * 取某收方正在「银行审核中」的那条变更（没有则 null）。
     *
     * <p>这是付款挂起与「换卡走收方入驻」两条链路共用的唯一判据。
     */
    default IcbcPayeeBankCardChangeDO selectPendingByPayeeId(Long payeeId) {
        if (payeeId == null) {
            return null;
        }
        // 服务层保证最多一条在途，这里取最新的一条即可（不用 selectOne，免得历史脏数据把它变成异常）
        return selectList(new LambdaQueryWrapperX<IcbcPayeeBankCardChangeDO>()
                .eq(IcbcPayeeBankCardChangeDO::getPayeeId, payeeId)
                .eq(IcbcPayeeBankCardChangeDO::getStatus, PayeeBankCardChangeStatusEnum.PENDING_REVIEW.getStatus())
                .orderByDesc(IcbcPayeeBankCardChangeDO::getId))
                .stream().findFirst().orElse(null);
    }

    /**
     * 批量取多个收方正在审核中的变更，key = 收方编号。
     *
     * <p>自然人端「我的资料」要一次列出他在各家企业的收款账户，不能一人一条 SQL。
     */
    default List<IcbcPayeeBankCardChangeDO> selectPendingListByPayeeIds(Collection<Long> payeeIds) {
        if (payeeIds == null || payeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcPayeeBankCardChangeDO>()
                .in(IcbcPayeeBankCardChangeDO::getPayeeId, payeeIds)
                .eq(IcbcPayeeBankCardChangeDO::getStatus, PayeeBankCardChangeStatusEnum.PENDING_REVIEW.getStatus()));
    }

    default List<IcbcPayeeBankCardChangeDO> selectListByPayeeId(Long payeeId) {
        if (payeeId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcPayeeBankCardChangeDO>()
                .eq(IcbcPayeeBankCardChangeDO::getPayeeId, payeeId)
                .orderByDesc(IcbcPayeeBankCardChangeDO::getId));
    }

}
