package cn.iocoder.yudao.module.icbc.dal.mysql.settlement;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo.SettlementPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 结算单 Mapper。
 */
@Mapper
public interface IcbcSettlementMapper extends BaseMapperX<IcbcSettlementDO> {

    default IcbcSettlementDO selectBySettlementNo(String settlementNo) {
        return selectOne(IcbcSettlementDO::getSettlementNo, settlementNo);
    }

    default List<IcbcSettlementDO> selectListByPayeeId(Long payeeId) {
        return selectList(new LambdaQueryWrapperX<IcbcSettlementDO>()
                .eq(IcbcSettlementDO::getPayeeId, payeeId)
                .orderByDesc(IcbcSettlementDO::getId));
    }

    /**
     * 自然人本人可看的结算单（同一登录可代多个主体操作，所以按 naturalPersonId 过滤，不静默推断）。
     */
    default List<IcbcSettlementDO> selectListByNaturalPersonId(Long naturalPersonId) {
        if (naturalPersonId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcSettlementDO>()
                .eq(IcbcSettlementDO::getNaturalPersonId, naturalPersonId)
                .orderByDesc(IcbcSettlementDO::getId));
    }

    default PageResult<IcbcSettlementDO> selectPage(SettlementPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcSettlementDO>()
                .eqIfPresent(IcbcSettlementDO::getPayeeId, reqVO.getPayeeId())
                .eqIfPresent(IcbcSettlementDO::getNaturalPersonId, reqVO.getNaturalPersonId())
                .eqIfPresent(IcbcSettlementDO::getConfirmStatus, reqVO.getConfirmStatus())
                .likeIfPresent(IcbcSettlementDO::getSettlementNo, reqVO.getSettlementNo())
                .likeIfPresent(IcbcSettlementDO::getSellerName, reqVO.getSellerName())
                .betweenIfPresent(IcbcSettlementDO::getGenerateTime, reqVO.getGenerateTime())
                .orderByDesc(IcbcSettlementDO::getId));
    }

    /**
     * 超时扫描：指定确认状态且截止时间已过（供定时任务用，不自动确认，只升级为线下签字 / 待办）。
     */
    default List<IcbcSettlementDO> selectTimeoutList(Collection<Integer> confirmStatuses,
                                                     java.time.LocalDateTime deadline) {
        if (confirmStatuses == null || confirmStatuses.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcSettlementDO>()
                .in(IcbcSettlementDO::getConfirmStatus, confirmStatuses)
                .isNotNull(IcbcSettlementDO::getDeadlineTime)
                .le(IcbcSettlementDO::getDeadlineTime, deadline)
                .orderByAsc(IcbcSettlementDO::getId));
    }

    /**
     * 已确认但长期未开票：按确认时间扫描（确认后 deadlineTime 被清空，所以不能用截止时间跑）。
     * 只提醒，不自动作废。
     */
    default List<IcbcSettlementDO> selectConfirmedBefore(Collection<Integer> confirmStatuses,
                                                         java.time.LocalDateTime confirmBefore) {
        if (confirmStatuses == null || confirmStatuses.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcSettlementDO>()
                .in(IcbcSettlementDO::getConfirmStatus, confirmStatuses)
                .isNotNull(IcbcSettlementDO::getConfirmTime)
                .le(IcbcSettlementDO::getConfirmTime, confirmBefore)
                .orderByAsc(IcbcSettlementDO::getId));
    }

}
