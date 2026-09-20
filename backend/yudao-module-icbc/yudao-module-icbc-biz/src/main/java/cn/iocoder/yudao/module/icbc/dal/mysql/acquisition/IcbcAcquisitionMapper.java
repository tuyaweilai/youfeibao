package cn.iocoder.yudao.module.icbc.dal.mysql.acquisition;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo.AcquisitionPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 收购登记单 Mapper
 */
@Mapper
public interface IcbcAcquisitionMapper extends BaseMapperX<IcbcAcquisitionDO> {

    default IcbcAcquisitionDO selectByAcquisitionNo(String acquisitionNo) {
        return selectOne(IcbcAcquisitionDO::getAcquisitionNo, acquisitionNo);
    }

    /**
     * 按客户端幂等键查重。离线补传时同一笔会被服务端接到多次，用这个键收敛成一条。
     */
    default IcbcAcquisitionDO selectByClientRequestId(String clientRequestId) {
        return selectOne(IcbcAcquisitionDO::getClientRequestId, clientRequestId);
    }

    default List<IcbcAcquisitionDO> selectListByPayeeId(Long payeeId) {
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .eq(IcbcAcquisitionDO::getPayeeId, payeeId)
                .orderByDesc(IcbcAcquisitionDO::getId));
    }

    /**
     * 按收方档案编号批量查询（自然人端「卖货记录」跨企业聚合用）。
     */
    default List<IcbcAcquisitionDO> selectListByPayeeIds(Collection<Long> payeeIds) {
        if (payeeIds == null || payeeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .in(IcbcAcquisitionDO::getPayeeId, payeeIds)
                .orderByDesc(IcbcAcquisitionDO::getId));
    }

    /**
     * 按开票的合作方订单号反查收购单（一票一档的合同流 / 货物流 / 信息流来自这里）。
     */
    default List<IcbcAcquisitionDO> selectListByInvoicePartnerOrderIds(Collection<String> partnerOrderIds) {
        if (partnerOrderIds == null || partnerOrderIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .in(IcbcAcquisitionDO::getInvoicePartnerOrderId, partnerOrderIds));
    }

    default PageResult<IcbcAcquisitionDO> selectPage(AcquisitionPageReqVO reqVO) {
        LambdaQueryWrapperX<IcbcAcquisitionDO> wrapper = new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .eqIfPresent(IcbcAcquisitionDO::getPayeeId, reqVO.getPayeeId())
                .eqIfPresent(IcbcAcquisitionDO::getStatus, reqVO.getStatus())
                .likeIfPresent(IcbcAcquisitionDO::getAcquisitionNo, reqVO.getAcquisitionNo())
                .likeIfPresent(IcbcAcquisitionDO::getSellerName, reqVO.getSellerName())
                .eqIfPresent(IcbcAcquisitionDO::getVehiclePlateNo, reqVO.getVehiclePlateNo())
                .betweenIfPresent(IcbcAcquisitionDO::getTradeTime, reqVO.getTradeTime());
        // 「直接收购」是报表 / 列表口径，不是失败态：未关联订单（purchase_order_id = 0）即直接收购
        if (Boolean.TRUE.equals(reqVO.getDirectAcquisition())) {
            wrapper.eq(IcbcAcquisitionDO::getPurchaseOrderId, 0L);
        } else if (Boolean.FALSE.equals(reqVO.getDirectAcquisition())) {
            wrapper.ne(IcbcAcquisitionDO::getPurchaseOrderId, 0L);
        }
        return selectPage(reqVO, wrapper.orderByDesc(IcbcAcquisitionDO::getId));
    }

    // ==================== 结算单（#33，ADR 0018） ====================

    /**
     * 某结算单下的全部收购单，按 id 升序（快照与展示顺序固定）。
     */
    default List<IcbcAcquisitionDO> selectListBySettlementId(Long settlementId) {
        if (settlementId == null) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .eq(IcbcAcquisitionDO::getSettlementId, settlementId)
                .orderByAsc(IcbcAcquisitionDO::getId));
    }

    /**
     * 「结束本次收货」时待归组的收购单：同一出售者、同一场站、尚未归入任何结算单、且未作废。
     * 离线批次传了 {@code batchKey} 时只取该批次。
     *
     * <p>场站传空表示不分场站（兼容旧客户端与历史数据）；现场动作仍是唯一可信的批次边界。
     */
    default List<IcbcAcquisitionDO> selectUngroupedByPayeeId(Long payeeId, Long stationId, String batchKey) {
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .eq(IcbcAcquisitionDO::getPayeeId, payeeId)
                .eqIfPresent(IcbcAcquisitionDO::getStationId, stationId)
                .eqIfPresent(IcbcAcquisitionDO::getBatchKey, batchKey)
                .isNull(IcbcAcquisitionDO::getSettlementId)
                .ne(IcbcAcquisitionDO::getStatus, cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum.CANCELLED.getStatus())
                .orderByAsc(IcbcAcquisitionDO::getId));
    }

    /**
     * 本班次建议批次（ADR 0018）：同出售者 + 同场站 + 登记时间在窗口内、尚未归组的收购单。
     * <b>只作建议</b>：系统不自动合并，合并与否由现场动作（「结束本次收货」）决定。
     */
    default List<IcbcAcquisitionDO> selectUngroupedInWindow(Long payeeId, Long stationId,
                                                            java.time.LocalDateTime createdAfter) {
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .eq(IcbcAcquisitionDO::getPayeeId, payeeId)
                .eqIfPresent(IcbcAcquisitionDO::getStationId, stationId)
                .isNull(IcbcAcquisitionDO::getSettlementId)
                .ne(IcbcAcquisitionDO::getStatus, cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum.CANCELLED.getStatus())
                .ge(createdAfter != null, IcbcAcquisitionDO::getCreateTime, createdAfter)
                .orderByAsc(IcbcAcquisitionDO::getId));
    }

    // ==================== 交接批次与有效磅次（#50 T12） ====================

    /**
     * 某交接批次下已产生的收购单（计量结果已引用当时那一版磅次，据此禁止再改有效磅次）。
     *
     * <p>已作废（{@code status = CANCELLED}）的不算：作废后这笔计量不再成立，
     * 有效磅次应该能重新指定（单据本身仍保留，作废原因对人可见）。
     */
    default List<IcbcAcquisitionDO> selectListByHandoverBatchId(Long handoverBatchId) {
        if (handoverBatchId == null) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .eq(IcbcAcquisitionDO::getHandoverBatchId, handoverBatchId)
                .ne(IcbcAcquisitionDO::getStatus,
                        cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum.CANCELLED.getStatus())
                .orderByAsc(IcbcAcquisitionDO::getId));
    }

    // ==================== 工作台待办（#56 T18） ====================

    /**
     * 工作台「待称重」条数：未作废、且净重未录的收购单（登记要件允许重量留空）。
     */
    default long selectCountPendingWeigh() {
        return selectCount(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .ne(IcbcAcquisitionDO::getStatus, AcquisitionStatusEnum.CANCELLED.getStatus())
                .isNull(IcbcAcquisitionDO::getNetWeight));
    }

    /** 工作台「待称重」明细：按登记时间升序，最多 {@code limit} 条。 */
    default List<IcbcAcquisitionDO> selectListPendingWeigh(int limit) {
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .ne(IcbcAcquisitionDO::getStatus, AcquisitionStatusEnum.CANCELLED.getStatus())
                .isNull(IcbcAcquisitionDO::getNetWeight)
                .orderByAsc(IcbcAcquisitionDO::getId)
                .last("LIMIT " + limit));
    }

    /**
     * 工作台「待验收」条数：未作废、已录磅重、尚未归入结算单（现场还没点「结束本次收货」）。
     */
    default long selectCountPendingInspection() {
        return selectCount(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .ne(IcbcAcquisitionDO::getStatus, AcquisitionStatusEnum.CANCELLED.getStatus())
                .isNotNull(IcbcAcquisitionDO::getNetWeight)
                .isNull(IcbcAcquisitionDO::getSettlementId));
    }

    /** 工作台「待验收」明细：按登记时间升序，最多 {@code limit} 条。 */
    default List<IcbcAcquisitionDO> selectListPendingInspection(int limit) {
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .ne(IcbcAcquisitionDO::getStatus, AcquisitionStatusEnum.CANCELLED.getStatus())
                .isNotNull(IcbcAcquisitionDO::getNetWeight)
                .isNull(IcbcAcquisitionDO::getSettlementId)
                .orderByAsc(IcbcAcquisitionDO::getId)
                .last("LIMIT " + limit));
    }

    // ==================== 待入库（#52 T14） ====================

    /**
     * 待入库候选：**已验收**（已归入结算单）、未作废的收购单，倒序；带可选筛选。
     *
     * <p>只做「验收后」这一步粗筛；可入库实物量与剩余可入库在服务层按**唯一取数点**
     * （{@code StockInService#resolveAvailableQuantity}）计算，不在 SQL 里再写一份重量口径。
     */
    default List<IcbcAcquisitionDO> selectListPendingStockIn(
            cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPendingPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .likeIfPresent(IcbcAcquisitionDO::getAcquisitionNo, reqVO.getAcquisitionNo())
                .likeIfPresent(IcbcAcquisitionDO::getSellerName, reqVO.getSellerName())
                .eqIfPresent(IcbcAcquisitionDO::getGoodsConfigId, reqVO.getGoodsConfigId())
                .isNotNull(IcbcAcquisitionDO::getSettlementId)
                .ne(IcbcAcquisitionDO::getStatus, AcquisitionStatusEnum.CANCELLED.getStatus())
                .orderByDesc(IcbcAcquisitionDO::getId));
    }

    /**
     * 锁住收购单行（{@code SELECT ... FOR UPDATE}）。
     *
     * <p>同一收购单的并发入库在此串行化：入库单先落待过账，过账时才写库存，
     * 「读累计入库 → 校验上限 → 写流水」需要一个共同的串行点，就是这行收购单。
     * 上限本身仍由 {@code StockApi} 的 {@code maxCount} 兜底（跨入库单累计）。
     *
     * <p><b>必须作为事务的第一条语句</b>：InnoDB 可重复读的快照建立在第一条普通读上，
     * 先普通读再加锁读会让后续的累计校验用旧快照，白锁一场。
     */
    default IcbcAcquisitionDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<IcbcAcquisitionDO>()
                .eq(IcbcAcquisitionDO::getId, id)
                .last("FOR UPDATE"));
    }

}
