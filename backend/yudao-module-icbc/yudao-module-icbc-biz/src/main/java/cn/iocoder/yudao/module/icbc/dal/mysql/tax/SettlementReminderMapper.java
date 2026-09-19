package cn.iocoder.yudao.module.icbc.dal.mysql.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.SettlementReminderPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.SettlementReminderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 汇算清缴提醒 Mapper
 */
@Mapper
public interface SettlementReminderMapper extends BaseMapperX<SettlementReminderDO> {

    default SettlementReminderDO selectByPayeeIdAndTaxYear(Long payeeId, Integer taxYear) {
        return selectOne(new LambdaQueryWrapperX<SettlementReminderDO>()
                .eq(SettlementReminderDO::getPayeeId, payeeId)
                .eq(SettlementReminderDO::getTaxYear, taxYear));
    }

    default PageResult<SettlementReminderDO> selectPage(SettlementReminderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SettlementReminderDO>()
                .eqIfPresent(SettlementReminderDO::getTaxYear, reqVO.getTaxYear())
                .eqIfPresent(SettlementReminderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SettlementReminderDO::getPayeeId, reqVO.getPayeeId())
                .likeIfPresent(SettlementReminderDO::getSellerName, reqVO.getSellerName())
                .orderByDesc(SettlementReminderDO::getDeadline)
                .orderByDesc(SettlementReminderDO::getId));
    }

    default List<SettlementReminderDO> selectByTaxYear(Integer taxYear) {
        return selectList(new LambdaQueryWrapperX<SettlementReminderDO>()
                .eq(SettlementReminderDO::getTaxYear, taxYear)
                .orderByAsc(SettlementReminderDO::getDeadline));
    }

}
