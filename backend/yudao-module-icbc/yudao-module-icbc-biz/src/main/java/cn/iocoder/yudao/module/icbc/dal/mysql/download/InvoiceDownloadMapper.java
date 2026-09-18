package cn.iocoder.yudao.module.icbc.dal.mysql.download;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceDownloadDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工行发票下载记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface InvoiceDownloadMapper extends BaseMapperX<InvoiceDownloadDO> {

    /**
     * 根据合作方订单号查询下载记录
     *
     * @param partnerOrderId 合作方订单号
     * @return 下载记录
     */
    default InvoiceDownloadDO selectByPartnerOrderId(String partnerOrderId) {
        return selectOne(InvoiceDownloadDO::getPartnerOrderId, partnerOrderId);
    }

    /**
     * 根据工行订单号查询下载记录
     *
     * @param orderNumber 工行订单号
     * @return 下载记录
     */
    default InvoiceDownloadDO selectByOrderNumber(String orderNumber) {
        return selectOne(InvoiceDownloadDO::getOrderNumber, orderNumber);
    }

    /**
     * 根据发票号码查询下载记录
     *
     * @param invoiceNumber 发票号码
     * @return 下载记录
     */
    default InvoiceDownloadDO selectByInvoiceNumber(String invoiceNumber) {
        return selectOne(InvoiceDownloadDO::getInvoiceNumber, invoiceNumber);
    }

    /**
     * 根据下载状态查询下载记录列表
     *
     * @param downloadStatus 下载状态
     * @return 下载记录列表
     */
    default List<InvoiceDownloadDO> selectListByDownloadStatus(Integer downloadStatus) {
        return selectList(InvoiceDownloadDO::getDownloadStatus, downloadStatus);
    }

    /**
     * 根据发票订单ID查询下载记录
     *
     * @param invoiceOrderId 发票订单ID
     * @return 下载记录
     */
    default InvoiceDownloadDO selectByInvoiceOrderId(Long invoiceOrderId) {
        return selectOne(InvoiceDownloadDO::getInvoiceOrderId, invoiceOrderId);
    }

} 