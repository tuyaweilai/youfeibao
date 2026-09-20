package cn.iocoder.yudao.module.icbc.dal.mysql.invoice;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 工行反向开票订单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface InvoiceOrderMapper extends BaseMapperX<InvoiceOrderDO> {

    /**
     * 根据合作方订单ID查询订单
     *
     * @param partnerOrderId 合作方订单ID
     * @return 订单信息
     */
    default InvoiceOrderDO selectByPartnerOrderId(String partnerOrderId) {
        return selectOne(InvoiceOrderDO::getPartnerOrderId, partnerOrderId);
    }

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    default InvoiceOrderDO selectByOrderNo(String orderNo) {
        return selectOne(InvoiceOrderDO::getOrderNo, orderNo);
    }

    /**
     * 根据发票号码查询订单
     *
     * @param invoiceNo 发票号码
     * @return 订单信息
     */
    default InvoiceOrderDO selectByInvoiceNo(String invoiceNo) {
        return selectOne(InvoiceOrderDO::getInvoiceNo, invoiceNo);
    }

    /**
     * 计费计量取数：某租户（{@code tenantId} 为空则跨租户）在 {@code [start, end)} 内成功开具的
     * 报废产品收购发票。以开票日期归属期间，与额度台账、代办税费申报同一口径。
     */
    default List<InvoiceOrderDO> selectIssuedScrapInPeriod(Long tenantId, LocalDateTime start, LocalDateTime end) {
        return selectList(new LambdaQueryWrapperX<InvoiceOrderDO>()
                .eqIfPresent(InvoiceOrderDO::getTenantId, tenantId)
                .eq(InvoiceOrderDO::getBusinessType, "SCRAP")
                .eq(InvoiceOrderDO::getInvoiceStatus, InvoiceIssueStatusEnum.ISSUED.getStatus())
                .isNotNull(InvoiceOrderDO::getInvoiceNo)
                .ge(InvoiceOrderDO::getInvoiceDate, start)
                .lt(InvoiceOrderDO::getInvoiceDate, end)
                .orderByAsc(InvoiceOrderDO::getId));
    }

    /**
     * 按收方档案编号批量查询开票订单（自然人端「发票与税费」按他名下的收方档案聚合）。
     */
    default List<InvoiceOrderDO> selectListByPayeeIds(Collection<Long> payeeIds) {
        if (payeeIds == null || payeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<InvoiceOrderDO>()
                .in(InvoiceOrderDO::getPayeeId, payeeIds)
                .orderByDesc(InvoiceOrderDO::getId));
    }

    /**
     * 按收购单编号批量查询开票订单（#55 T17 关联单据查询用）。
     */
    default List<InvoiceOrderDO> selectListByAcquisitionIds(Collection<Long> acquisitionIds) {
        if (acquisitionIds == null || acquisitionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<InvoiceOrderDO>()
                .in(InvoiceOrderDO::getAcquisitionId, acquisitionIds)
                .orderByDesc(InvoiceOrderDO::getId));
    }

    // ==================== 工作台待办（#56 T18） ====================

    /**
     * 工作台「票务失败」条数：预开票失败、开票失败、缴税失败 / 异常、上传失败四条状态线任一异常。
     * 同一张票只算一条待办。
     */
    default long selectCountException() {
        return selectCount(new LambdaQueryWrapperX<InvoiceOrderDO>()
                .and(wrapper -> wrapper
                        .in(InvoiceOrderDO::getPreInvoiceStatus, PreInvoiceStatusEnum.exceptionStatuses())
                        .or().eq(InvoiceOrderDO::getInvoiceStatus, InvoiceIssueStatusEnum.FAILED.getStatus())
                        .or().in(InvoiceOrderDO::getTaxStatus, TaxStatusEnum.exceptionStatuses())
                        .or().eq(InvoiceOrderDO::getUploadStatus, UploadStatusEnum.FAILED.getStatus())));
    }

    /** 工作台「票务失败」明细：最近发生的排在前面，最多 {@code limit} 条。 */
    default List<InvoiceOrderDO> selectListException(int limit) {
        return selectList(new LambdaQueryWrapperX<InvoiceOrderDO>()
                .and(wrapper -> wrapper
                        .in(InvoiceOrderDO::getPreInvoiceStatus, PreInvoiceStatusEnum.exceptionStatuses())
                        .or().eq(InvoiceOrderDO::getInvoiceStatus, InvoiceIssueStatusEnum.FAILED.getStatus())
                        .or().in(InvoiceOrderDO::getTaxStatus, TaxStatusEnum.exceptionStatuses())
                        .or().eq(InvoiceOrderDO::getUploadStatus, UploadStatusEnum.FAILED.getStatus()))
                .orderByDesc(InvoiceOrderDO::getId)
                .last("LIMIT " + limit));
    }

} 