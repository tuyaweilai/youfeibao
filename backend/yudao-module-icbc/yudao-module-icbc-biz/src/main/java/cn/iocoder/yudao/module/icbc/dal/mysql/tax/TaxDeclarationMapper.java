package cn.iocoder.yudao.module.icbc.dal.mysql.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxDeclarationPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.TaxDeclarationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 代办税费申报单 Mapper
 */
@Mapper
public interface TaxDeclarationMapper extends BaseMapperX<TaxDeclarationDO> {

    default TaxDeclarationDO selectByPeriodMonth(String periodMonth) {
        return selectOne(TaxDeclarationDO::getPeriodMonth, periodMonth);
    }

    default PageResult<TaxDeclarationDO> selectPage(TaxDeclarationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxDeclarationDO>()
                .eqIfPresent(TaxDeclarationDO::getPeriodMonth, reqVO.getPeriodMonth())
                .eqIfPresent(TaxDeclarationDO::getStatus, reqVO.getStatus())
                .orderByDesc(TaxDeclarationDO::getPeriodMonth));
    }

    default List<TaxDeclarationDO> selectByPeriodMonths(List<String> periodMonths) {
        return selectList(new LambdaQueryWrapperX<TaxDeclarationDO>()
                .in(TaxDeclarationDO::getPeriodMonth, periodMonths)
                .orderByAsc(TaxDeclarationDO::getPeriodMonth));
    }

    default List<TaxDeclarationDO> selectUnpaid() {
        return selectList(new LambdaQueryWrapperX<TaxDeclarationDO>()
                .ne(TaxDeclarationDO::getStatus,
                        cn.iocoder.yudao.module.icbc.enums.TaxDeclarationStatusEnum.PAID.getStatus())
                .orderByAsc(TaxDeclarationDO::getPeriodMonth));
    }

}
