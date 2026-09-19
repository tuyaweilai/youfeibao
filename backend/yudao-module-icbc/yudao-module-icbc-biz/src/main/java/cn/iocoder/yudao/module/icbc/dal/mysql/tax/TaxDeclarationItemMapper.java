package cn.iocoder.yudao.module.icbc.dal.mysql.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxDeclarationItemPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.TaxDeclarationItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 代办税费申报明细 Mapper
 */
@Mapper
public interface TaxDeclarationItemMapper extends BaseMapperX<TaxDeclarationItemDO> {

    default List<TaxDeclarationItemDO> selectByDeclarationId(Long declarationId) {
        return selectList(new LambdaQueryWrapperX<TaxDeclarationItemDO>()
                .eq(TaxDeclarationItemDO::getDeclarationId, declarationId)
                .orderByDesc(TaxDeclarationItemDO::getSalesAmount));
    }

    default int deleteByDeclarationId(Long declarationId) {
        return delete(new LambdaQueryWrapperX<TaxDeclarationItemDO>()
                .eq(TaxDeclarationItemDO::getDeclarationId, declarationId));
    }

    default PageResult<TaxDeclarationItemDO> selectPage(TaxDeclarationItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxDeclarationItemDO>()
                .eqIfPresent(TaxDeclarationItemDO::getDeclarationId, reqVO.getDeclarationId())
                .eqIfPresent(TaxDeclarationItemDO::getPeriodMonth, reqVO.getPeriodMonth())
                .eqIfPresent(TaxDeclarationItemDO::getPayeeId, reqVO.getPayeeId())
                .eqIfPresent(TaxDeclarationItemDO::getOverExempt, reqVO.getOverExempt())
                .likeIfPresent(TaxDeclarationItemDO::getSellerName, reqVO.getSellerName())
                .orderByDesc(TaxDeclarationItemDO::getSalesAmount));
    }

}
