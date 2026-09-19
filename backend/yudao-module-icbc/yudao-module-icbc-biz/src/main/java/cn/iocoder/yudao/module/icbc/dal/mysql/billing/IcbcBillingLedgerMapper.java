package cn.iocoder.yudao.module.icbc.dal.mysql.billing;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.billing.vo.IcbcBillingLedgerPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.billing.IcbcBillingLedgerDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 平台计费计量台账 Mapper（#16）。全局表，不按租户过滤。
 */
@Mapper
public interface IcbcBillingLedgerMapper extends BaseMapperX<IcbcBillingLedgerDO> {

    default IcbcBillingLedgerDO selectByTenantAndPeriod(Long tenantId, String periodMonth) {
        return selectOne(new LambdaQueryWrapperX<IcbcBillingLedgerDO>()
                .eq(IcbcBillingLedgerDO::getTenantId, tenantId)
                .eq(IcbcBillingLedgerDO::getPeriodMonth, periodMonth));
    }

    default List<IcbcBillingLedgerDO> selectListByPeriod(String periodMonth) {
        return selectList(new LambdaQueryWrapperX<IcbcBillingLedgerDO>()
                .eqIfPresent(IcbcBillingLedgerDO::getPeriodMonth, periodMonth)
                .orderByAsc(IcbcBillingLedgerDO::getTenantId));
    }

    default PageResult<IcbcBillingLedgerDO> selectPage(IcbcBillingLedgerPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcBillingLedgerDO>()
                .eqIfPresent(IcbcBillingLedgerDO::getTenantId, reqVO.getTenantId())
                .eqIfPresent(IcbcBillingLedgerDO::getPeriodMonth, reqVO.getPeriodMonth())
                .orderByDesc(IcbcBillingLedgerDO::getPeriodMonth)
                .orderByAsc(IcbcBillingLedgerDO::getTenantId));
    }

}
