package cn.iocoder.yudao.module.icbc.dal.mysql.inputinvoice;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.inputinvoice.IcbcInputInvoiceLinkDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 进项发票勾稽关联 Mapper（#49 T11）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcInputInvoiceLinkMapper extends BaseMapperX<IcbcInputInvoiceLinkDO> {

    default List<IcbcInputInvoiceLinkDO> selectListByInvoiceId(Long invoiceId) {
        return selectList(new LambdaQueryWrapperX<IcbcInputInvoiceLinkDO>()
                .eq(IcbcInputInvoiceLinkDO::getInvoiceId, invoiceId)
                .orderByAsc(IcbcInputInvoiceLinkDO::getId));
    }

    /**
     * 同一张进项票与同一张单据只允许一行勾稽（唯一键兜底，服务层先查再给可读报错）。
     */
    default IcbcInputInvoiceLinkDO selectByInvoiceAndBiz(Long invoiceId, String bizType, Long bizId) {
        return selectOne(new LambdaQueryWrapperX<IcbcInputInvoiceLinkDO>()
                .eq(IcbcInputInvoiceLinkDO::getInvoiceId, invoiceId)
                .eq(IcbcInputInvoiceLinkDO::getBizType, bizType)
                .eq(IcbcInputInvoiceLinkDO::getBizId, bizId));
    }

    /**
     * 同一张单据上已有哪些勾稽记录（可能来自多张进项票），用于累计金额上限校验。
     */
    default List<IcbcInputInvoiceLinkDO> selectListByBiz(String bizType, Long bizId) {
        return selectList(new LambdaQueryWrapperX<IcbcInputInvoiceLinkDO>()
                .eq(IcbcInputInvoiceLinkDO::getBizType, bizType)
                .eq(IcbcInputInvoiceLinkDO::getBizId, bizId));
    }

    default int deleteByInvoiceId(Long invoiceId) {
        return delete(new LambdaQueryWrapperX<IcbcInputInvoiceLinkDO>()
                .eq(IcbcInputInvoiceLinkDO::getInvoiceId, invoiceId));
    }

}
