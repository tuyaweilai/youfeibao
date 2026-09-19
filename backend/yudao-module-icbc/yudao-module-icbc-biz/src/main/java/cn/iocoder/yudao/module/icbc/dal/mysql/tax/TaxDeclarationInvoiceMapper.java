package cn.iocoder.yudao.module.icbc.dal.mysql.tax;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.tax.TaxDeclarationInvoiceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 申报单与发票的关联 Mapper
 */
@Mapper
public interface TaxDeclarationInvoiceMapper extends BaseMapperX<TaxDeclarationInvoiceDO> {

    default List<TaxDeclarationInvoiceDO> selectByDeclarationId(Long declarationId) {
        return selectList(new LambdaQueryWrapperX<TaxDeclarationInvoiceDO>()
                .eq(TaxDeclarationInvoiceDO::getDeclarationId, declarationId)
                .orderByAsc(TaxDeclarationInvoiceDO::getInvoiceOrderId));
    }

    default int deleteByDeclarationId(Long declarationId) {
        return delete(new LambdaQueryWrapperX<TaxDeclarationInvoiceDO>()
                .eq(TaxDeclarationInvoiceDO::getDeclarationId, declarationId));
    }

}
