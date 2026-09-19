package cn.iocoder.yudao.module.icbc.dal.mysql.tax;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxSupplementPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.TaxSupplementDO;
import cn.iocoder.yudao.module.icbc.enums.TaxSupplementStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 需补缴税费 Mapper
 */
@Mapper
public interface TaxSupplementMapper extends BaseMapperX<TaxSupplementDO> {

    default PageResult<TaxSupplementDO> selectPage(TaxSupplementPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TaxSupplementDO>()
                .eqIfPresent(TaxSupplementDO::getPeriodMonth, reqVO.getPeriodMonth())
                .eqIfPresent(TaxSupplementDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TaxSupplementDO::getPayeeId, reqVO.getPayeeId())
                .likeIfPresent(TaxSupplementDO::getSellerName, reqVO.getSellerName())
                .orderByDesc(TaxSupplementDO::getPeriodMonth)
                .orderByDesc(TaxSupplementDO::getId));
    }

    default List<TaxSupplementDO> selectPending() {
        return selectList(new LambdaQueryWrapperX<TaxSupplementDO>()
                .eq(TaxSupplementDO::getStatus, TaxSupplementStatusEnum.PENDING.getStatus())
                .orderByAsc(TaxSupplementDO::getPeriodMonth));
    }

    default List<TaxSupplementDO> selectPendingByDeclarationId(Long declarationId) {
        return selectList(new LambdaQueryWrapperX<TaxSupplementDO>()
                .eq(TaxSupplementDO::getDeclarationId, declarationId)
                .eq(TaxSupplementDO::getStatus, TaxSupplementStatusEnum.PENDING.getStatus())
                .orderByAsc(TaxSupplementDO::getId));
    }

}
