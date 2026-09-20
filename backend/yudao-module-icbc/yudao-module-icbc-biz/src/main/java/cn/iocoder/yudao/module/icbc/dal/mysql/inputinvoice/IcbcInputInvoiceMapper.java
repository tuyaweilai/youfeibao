package cn.iocoder.yudao.module.icbc.dal.mysql.inputinvoice;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoicePageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.inputinvoice.IcbcInputInvoiceDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 进项发票 Mapper（#49 T11）。租户表，不做跨租户读取。
 */
@Mapper
public interface IcbcInputInvoiceMapper extends BaseMapperX<IcbcInputInvoiceDO> {

    /**
     * 按「销方 + 发票号码」查已有登记，用于登记时的唯一性校验。
     *
     * @param sellerKey 销方唯一标识（有税号用税号，否则用名称）
     * @param invoiceNo 发票号码
     * @return 已登记的进项发票，没有则返回 null
     */
    default IcbcInputInvoiceDO selectBySellerKeyAndInvoiceNo(String sellerKey, String invoiceNo) {
        return selectOne(new LambdaQueryWrapperX<IcbcInputInvoiceDO>()
                .eq(IcbcInputInvoiceDO::getSellerKey, sellerKey)
                .eq(IcbcInputInvoiceDO::getInvoiceNo, invoiceNo));
    }

    default PageResult<IcbcInputInvoiceDO> selectPage(InputInvoicePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcInputInvoiceDO>()
                .likeIfPresent(IcbcInputInvoiceDO::getInvoiceNo, reqVO.getInvoiceNo())
                .likeIfPresent(IcbcInputInvoiceDO::getSellerName, reqVO.getSellerName())
                .eqIfPresent(IcbcInputInvoiceDO::getSellerTaxNo, reqVO.getSellerTaxNo())
                .eqIfPresent(IcbcInputInvoiceDO::getInvoiceType, reqVO.getInvoiceType())
                .eqIfPresent(IcbcInputInvoiceDO::getStatus, reqVO.getStatus())
                .geIfPresent(IcbcInputInvoiceDO::getInvoiceDate, reqVO.getInvoiceDateStart())
                .leIfPresent(IcbcInputInvoiceDO::getInvoiceDate, reqVO.getInvoiceDateEnd())
                .orderByDesc(IcbcInputInvoiceDO::getId));
    }

}
