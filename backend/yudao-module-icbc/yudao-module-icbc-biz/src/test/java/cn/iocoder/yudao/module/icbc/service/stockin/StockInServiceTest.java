package cn.iocoder.yudao.module.icbc.service.stockin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPendingPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInPendingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo.StockInSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockin.IcbcStockInDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockin.IcbcStockInItemDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockin.IcbcStockInItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockin.IcbcStockInMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.icbc.enums.StockInStatusEnum;
import cn.iocoder.yudao.module.icbc.service.stockin.impl.StockInServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link StockInServiceImpl} 的单元测试（#52 T14）。
 *
 * <p>断言的是外部可观察行为：待入库列表给谁看、建单是否动库存、过账写了什么流水、
 * 重复确认是否幂等、累计上限怎么传、作废是否按相反方向冲销。库存写入口是跨模块的
 * {@link StockApi}，按规格 #38 的测试决策用 Mock，断言的是与它的契约。
 */
@Import({StockInServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class StockInServiceTest extends BaseDbUnitTest {

    @Resource
    private StockInService stockInService;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private IcbcStockInMapper stockInMapper;
    @Resource
    private IcbcStockInItemMapper stockInItemMapper;

    @MockBean
    private StockApi stockApi;

    // ==================== 可入库实物量：唯一取数点（AC2） ====================

    @Test
    public void testResolveAvailableQuantity_prefersAcceptedThenNetWeight() {
        // 实物口径 = 净重（毛重 − 皮重）；结算重量只作计价基准，不参与库存（ADR 0028）
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_QTY", new BigDecimal("12500"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        assertEquals(0, new BigDecimal("12500").compareTo(stockInService.resolveAvailableQuantity(acquisition)));

        // 有接收结论时取接收量（#53）：拒收 / 退回的部分不可入库
        IcbcAcquisitionDO partial = insertAcquisition("ACQ_PARTIAL_RECEIPT", new BigDecimal("12500"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        IcbcAcquisitionDO update = new IcbcAcquisitionDO();
        update.setId(partial.getId());
        update.setAcceptedWeight(new BigDecimal("12000"));
        update.setRejectedWeight(new BigDecimal("500"));
        acquisitionMapper.updateById(update);
        assertEquals(0, new BigDecimal("12000").compareTo(
                stockInService.resolveAvailableQuantity(acquisitionMapper.selectById(partial.getId()))));

        // 没录重量的单没有可入库量（不是 0 库存，是不能入库）
        IcbcAcquisitionDO noWeight = insertAcquisition("ACQ_NO_WEIGHT", null, 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(stockInService.resolveAvailableQuantity(noWeight)));
    }

    // ==================== 待入库列表（AC1） ====================

    @Test
    public void testPendingList_onlyAcceptedNotCancelledAndNotFullyStocked() {
        IcbcAcquisitionDO pending = insertAcquisition("ACQ_PENDING", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        IcbcAcquisitionDO partial = insertAcquisition("ACQ_PARTIAL", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        IcbcAcquisitionDO full = insertAcquisition("ACQ_FULL", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        IcbcAcquisitionDO notAccepted = insertAcquisition("ACQ_NOT_ACCEPTED", new BigDecimal("100"), null,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        IcbcAcquisitionDO cancelled = insertAcquisition("ACQ_CANCELLED", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.CANCELLED.getStatus());
        IcbcAcquisitionDO noWeight = insertAcquisition("ACQ_NO_WEIGHT", null, 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());

        confirm(partial.getId(), "60");
        confirm(full.getId(), "100");

        PageResult<StockInPendingRespVO> page = stockInService.getPendingPage(new StockInPendingPageReqVO());
        assertEquals(2, page.getTotal());
        StockInPendingRespVO pendingRow = page.getList().stream()
                .filter(row -> row.getAcquisitionId().equals(pending.getId())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("100").compareTo(pendingRow.getAvailableQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(pendingRow.getStockedQuantity()));
        assertEquals(0, new BigDecimal("100").compareTo(pendingRow.getRemainingQuantity()));
        assertEquals("张三", pendingRow.getSellerName());
        assertEquals("废钢", pendingRow.getCategoryName());

        StockInPendingRespVO partialRow = page.getList().stream()
                .filter(row -> row.getAcquisitionId().equals(partial.getId())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("60").compareTo(partialRow.getStockedQuantity()));
        assertEquals(0, new BigDecimal("40").compareTo(partialRow.getRemainingQuantity()));

        // 未验收 / 已作废 / 无重量 / 已全部入库：都不在待办里
        assertTrue(page.getList().stream().noneMatch(row -> row.getAcquisitionId().equals(full.getId())));
        assertTrue(page.getList().stream().noneMatch(row -> row.getAcquisitionId().equals(notAccepted.getId())));
        assertTrue(page.getList().stream().noneMatch(row -> row.getAcquisitionId().equals(cancelled.getId())));
        assertTrue(page.getList().stream().noneMatch(row -> row.getAcquisitionId().equals(noWeight.getId())));
    }

    // ==================== 建单：不过账不动库存（AC5） ====================

    @Test
    public void testCreateStockIn_isPendingAndDoesNotTouchStock() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_CREATE", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());

        Long id = stockInService.createStockIn(saveReq(acquisition.getId(), item(1L, 11L, 22L, "40")));

        IcbcStockInDO saved = stockInMapper.selectById(id);
        assertEquals(StockInStatusEnum.PENDING.getStatus(), saved.getStatus());
        assertNotNull(saved.getStockInNo());
        assertTrue(saved.getStockInNo().startsWith("SI"));
        assertEquals(0, new BigDecimal("100").compareTo(saved.getAvailableQuantity()));
        assertEquals(0, new BigDecimal("40").compareTo(saved.getTotalQuantity()));
        assertNull(saved.getPostedTime());
        // 待过账：没有写任何库存流水
        verify(stockApi, never()).in(any());
        verify(stockApi, never()).out(any());
    }

    // ==================== 确认入库：拆库位、过账（AC1 / AC2） ====================

    @Test
    public void testConfirmStockIn_splitsAcrossLocationsAndPostsOncePerItem() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_SPLIT", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());

        StockInSaveReqVO reqVO = saveReq(acquisition.getId(),
                item(1L, 11L, 21L, "40"), item(2L, 12L, 0L, "35"), item(1L, 0L, 22L, "25"));
        Long id = stockInService.confirmStockIn(reqVO);

        IcbcStockInDO saved = stockInMapper.selectById(id);
        assertEquals(StockInStatusEnum.POSTED.getStatus(), saved.getStatus());
        assertNotNull(saved.getPostedTime());
        assertEquals(0, new BigDecimal("100").compareTo(saved.getTotalQuantity()));

        List<IcbcStockInItemDO> items = stockInItemMapper.selectListByStockInId(id);
        assertEquals(3, items.size());

        ArgumentCaptor<StockChangeReqDTO> captor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi, times(3)).in(captor.capture());
        List<StockChangeReqDTO> calls = captor.getAllValues();
        // 每条明细一条流水：业务类型是收货入库，业务编号是收购单（跨入库单累计），业务项是明细（幂等）
        for (int i = 0; i < 3; i++) {
            assertEquals(ErpStockRecordBizTypeEnum.RECEIPT_IN.getType(), calls.get(i).getBizType());
            assertEquals(acquisition.getId(), calls.get(i).getBizId());
            assertEquals(items.get(i).getId(), calls.get(i).getBizItemId());
            assertEquals(saved.getStockInNo(), calls.get(i).getBizNo());
            assertEquals(acquisition.getGoodsConfigId(), calls.get(i).getGoodsConfigId());
            // maxCount = 可入库实物量 + 已冲销量（此刻为 0）
            assertEquals(0, new BigDecimal("100").compareTo(calls.get(i).getMaxCount()));
        }
        assertEquals(1L, calls.get(0).getWarehouseId().longValue());
        assertEquals(11L, calls.get(0).getLocationId().longValue());
        assertEquals(21L, calls.get(0).getBatchId().longValue());
        assertEquals(0, new BigDecimal("40").compareTo(calls.get(0).getCount()));
        // 未指定批次落 0，不是 null（唯一索引里 null 互不相等）
        assertEquals(12L, calls.get(1).getLocationId().longValue());
        assertEquals(0L, calls.get(1).getBatchId().longValue());
        assertEquals(0L, calls.get(2).getLocationId().longValue());
        assertEquals(22L, calls.get(2).getBatchId().longValue());
    }

    @Test
    public void testConfirmStockIn_accumulatesAcrossStockInsByAcquisitionId() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_MULTI", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());

        confirm(acquisition.getId(), "60");
        confirm(acquisition.getId(), "40");

        // 两张入库单：业务编号同为收购单，累计上限跨入库单由 StockApi 的 maxCount 兜底
        ArgumentCaptor<StockChangeReqDTO> captor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi, times(2)).in(captor.capture());
        assertEquals(acquisition.getId(), captor.getAllValues().get(0).getBizId());
        assertEquals(acquisition.getId(), captor.getAllValues().get(1).getBizId());
        assertEquals(2, stockInMapper.selectListByAcquisitionId(acquisition.getId()).size());
        assertEquals(0, new BigDecimal("100").compareTo(stockInService.getStockedQuantity(acquisition.getId())));
    }

    // ==================== 累计上限（AC2） ====================

    @Test
    public void testCreateStockIn_exceedAvailableAcrossStockIns_rejected() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_OVER", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        confirm(acquisition.getId(), "60");

        // 已入库 60，再入 50 超过剩余 40：建单时就拒（权威校验在过账时由 maxCount 做）
        assertError(() -> stockInService.createStockIn(saveReq(acquisition.getId(), item(1L, 0L, 0L, "50"))),
                STOCK_IN_EXCEED_AVAILABLE);
        assertEquals(1, stockInMapper.selectListByAcquisitionId(acquisition.getId()).size());
        // 只入 40 可以
        stockInService.createStockIn(saveReq(acquisition.getId(), item(1L, 0L, 0L, "40")));
        assertEquals(2, stockInMapper.selectListByAcquisitionId(acquisition.getId()).size());
    }

    @Test
    public void testPostStockIn_capGrowsByReversedQuantityAfterCancel() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_REVERSED", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        Long first = confirm(acquisition.getId(), "60");
        cancel(first, "堆位选错");
        // 已冲销 60：再次入库时上限是 100 + 60，净效果仍是「累计入库（60−60+新）≤ 100」
        Long second = confirm(acquisition.getId(), "100");

        ArgumentCaptor<StockChangeReqDTO> captor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi, times(2)).in(captor.capture());
        assertEquals(0, new BigDecimal("100").compareTo(captor.getAllValues().get(0).getMaxCount()));
        assertEquals(0, new BigDecimal("160").compareTo(captor.getAllValues().get(1).getMaxCount()));
        // 累计入库 = 已过账（60 + 100）− 已冲销（60）= 100
        assertEquals(0, new BigDecimal("100").compareTo(stockInService.getStockedQuantity(acquisition.getId())));
        assertNotNull(second);
    }

    @Test
    public void testCreateStockIn_emptyOrNegativeQuantityOrMissingWarehouse_rejected() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_BAD_ITEM", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());

        assertError(() -> stockInService.createStockIn(saveReq(acquisition.getId(),
                item(1L, 0L, 0L, "0"))), STOCK_IN_ITEM_INVALID);
        assertError(() -> stockInService.createStockIn(saveReq(acquisition.getId(),
                item(null, 0L, 0L, "10"))), STOCK_IN_ITEM_INVALID);
        assertTrue(stockInMapper.selectList().isEmpty());
    }

    // ==================== 幂等：重复确认不重复加库存（AC3） ====================

    @Test
    public void testPostStockIn_twice_isIdempotent() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_IDEMPOTENT", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        Long id = stockInService.createStockIn(saveReq(acquisition.getId(), item(1L, 0L, 0L, "30")));

        stockInService.postStockIn(id);
        stockInService.postStockIn(id);
        stockInService.postStockIn(id);

        // 明细只写一次流水：重复点击不再加库存
        verify(stockApi, times(1)).in(any());
        assertEquals(0, new BigDecimal("30").compareTo(stockInService.getStockedQuantity(acquisition.getId())));
    }

    // ==================== 作废：待过账直接作废，已过账冲销（AC5） ====================

    @Test
    public void testCancelStockIn_pendingDoesNotTouchStock_postedReverses() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_CANCEL", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        Long pending = stockInService.createStockIn(saveReq(acquisition.getId(), item(1L, 3L, 0L, "20")));
        cancel(pending, "改主意了");
        verify(stockApi, never()).out(any());
        assertEquals(0, BigDecimal.ZERO.compareTo(stockInService.getStockedQuantity(acquisition.getId())));

        Long posted = confirm(acquisition.getId(), "40");
        cancel(posted, "堆位选错");
        ArgumentCaptor<StockChangeReqDTO> captor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi, times(1)).out(captor.capture());
        assertEquals(ErpStockRecordBizTypeEnum.RECEIPT_IN_CANCEL.getType(), captor.getValue().getBizType());
        assertEquals(acquisition.getId(), captor.getValue().getBizId());
        assertEquals(0, new BigDecimal("40").compareTo(captor.getValue().getCount()));
        // 累计入库 = 已过账 40 − 已冲销 40 = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(stockInService.getStockedQuantity(acquisition.getId())));
        assertEquals(StockInStatusEnum.CANCELLED.getStatus(), stockInMapper.selectById(posted).getStatus());
    }

    @Test
    public void testCancelStockIn_twice_rejected() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_CANCEL_TWICE", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        Long id = confirm(acquisition.getId(), "10");
        cancel(id, "第一次");
        assertError(() -> cancel(id, "第二次"), STOCK_IN_STATUS_NOT_ALLOW);
    }

    // ==================== 门禁：未验收 / 已作废 / 无重量 ====================

    @Test
    public void testCreateStockIn_guards() {
        IcbcAcquisitionDO notAccepted = insertAcquisition("ACQ_NOT_ACC", new BigDecimal("100"), null,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        assertServiceException(() -> stockInService.createStockIn(
                saveReq(notAccepted.getId(), item(1L, 0L, 0L, "10"))), STOCK_IN_ACQUISITION_NOT_ACCEPTED);

        IcbcAcquisitionDO cancelled = insertAcquisition("ACQ_CAN", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.CANCELLED.getStatus());
        assertServiceException(() -> stockInService.createStockIn(
                saveReq(cancelled.getId(), item(1L, 0L, 0L, "10"))), STOCK_IN_ACQUISITION_CANCELLED);

        IcbcAcquisitionDO noWeight = insertAcquisition("ACQ_NW", null, 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        assertError(() -> stockInService.createStockIn(
                saveReq(noWeight.getId(), item(1L, 0L, 0L, "10"))), STOCK_IN_AVAILABLE_QUANTITY_EMPTY);

        assertError(() -> stockInService.createStockIn(saveReq(9999L, item(1L, 0L, 0L, "10"))),
                ACQUISITION_NOT_EXISTS);
        assertTrue(stockInMapper.selectList().isEmpty());
    }

    // ==================== 查询 ====================

    @Test
    public void testGetStockIn_detailHasItems_andPageFiltersByStatus() {
        IcbcAcquisitionDO acquisition = insertAcquisition("ACQ_QUERY", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        Long posted = confirm(acquisition.getId(), "30");
        Long pending = stockInService.createStockIn(saveReq(acquisition.getId(), item(1L, 0L, 0L, "20")));

        StockInRespVO detail = stockInService.getStockIn(posted);
        assertEquals("已过账", detail.getStatusName());
        assertEquals(1, detail.getItems().size());
        assertEquals(1L, detail.getItems().get(0).getWarehouseId().longValue());

        StockInPageReqVO pageReqVO = new StockInPageReqVO();
        pageReqVO.setStatus(StockInStatusEnum.PENDING.getStatus());
        PageResult<StockInRespVO> page = stockInService.getStockInPage(pageReqVO);
        assertEquals(1, page.getTotal());
        assertEquals(pending, page.getList().get(0).getId());
        assertEquals("待过账", page.getList().get(0).getStatusName());
    }

    @Test
    public void testGetStockIn_notExists_rejected() {
        assertServiceException(() -> stockInService.getStockIn(9999L), ErrorCodeConstants.STOCK_IN_NOT_EXISTS);
    }

    // ==================== 辅助方法 ====================

    private Long confirm(Long acquisitionId, String quantity) {
        return stockInService.confirmStockIn(saveReq(acquisitionId, item(1L, 0L, 0L, quantity)));
    }

    private void cancel(Long id, String reason) {
        StockInCancelReqVO reqVO = new StockInCancelReqVO();
        reqVO.setId(id);
        reqVO.setReason(reason);
        stockInService.cancelStockIn(reqVO);
    }

    private StockInSaveReqVO saveReq(Long acquisitionId, StockInItemReqVO... items) {
        StockInSaveReqVO reqVO = new StockInSaveReqVO();
        reqVO.setAcquisitionId(acquisitionId);
        reqVO.setItems(List.of(items));
        return reqVO;
    }

    private StockInItemReqVO item(Long warehouseId, Long locationId, Long batchId, String quantity) {
        StockInItemReqVO item = new StockInItemReqVO();
        item.setWarehouseId(warehouseId);
        item.setLocationId(locationId);
        item.setBatchId(batchId);
        item.setQuantity(new BigDecimal(quantity));
        return item;
    }

    @Test
    public void testGetStockedQuantityByOrderItems_sumsPostedByOrderItem() {
        IcbcAcquisitionDO a1 = insertAcquisition("ACQ_PO_1", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        linkToOrderItem(a1, 700L, 71L);
        IcbcAcquisitionDO a2 = insertAcquisition("ACQ_PO_2", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        linkToOrderItem(a2, 700L, 71L);
        IcbcAcquisitionDO a3 = insertAcquisition("ACQ_PO_3", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());
        linkToOrderItem(a3, 700L, 72L);
        // 未关联订单（直接收购）的不算
        IcbcAcquisitionDO direct = insertAcquisition("ACQ_DIRECT", new BigDecimal("100"), 5L,
                AcquisitionStatusEnum.REGISTERED.getStatus());

        stockInService.confirmStockIn(saveReq(a1.getId(), item(1L, 11L, 0L, "40")));
        // 只建单不过账：不算「已入库」
        stockInService.createStockIn(saveReq(a2.getId(), item(1L, 11L, 0L, "30")));
        stockInService.confirmStockIn(saveReq(a3.getId(), item(1L, 11L, 0L, "50")));
        stockInService.confirmStockIn(saveReq(direct.getId(), item(1L, 11L, 0L, "60")));

        Map<Long, BigDecimal> stocked = stockInService.getStockedQuantityByOrderItems(700L);

        // 只汇总已过账、挂在订单明细上的：ITEM_A（71）= 40（第二张只建单不过账），ITEM_B（72）= 50；直接收购不入
        assertEquals(2, stocked.size());
        assertEquals(0, new BigDecimal("40").compareTo(stocked.get(71L)));
        assertEquals(0, new BigDecimal("50").compareTo(stocked.get(72L)));
        assertFalse(stocked.containsKey(0L));
    }

    private void linkToOrderItem(IcbcAcquisitionDO acquisition, Long orderId, Long itemId) {
        IcbcAcquisitionDO update = new IcbcAcquisitionDO();
        update.setId(acquisition.getId());
        update.setPurchaseOrderId(orderId);
        update.setPurchaseOrderItemId(itemId);
        acquisitionMapper.updateById(update);
    }

    private IcbcAcquisitionDO insertAcquisition(String no, BigDecimal netWeight, Long settlementId,
                                                Integer status) {
        IcbcAcquisitionDO acquisition = IcbcAcquisitionDO.builder()
                .acquisitionNo(no)
                .payeeId(1L)
                .sellerName("张三")
                .goodsConfigId(100L)
                .categoryName("废钢")
                .unit("吨")
                .netWeight(netWeight)
                .settlementId(settlementId)
                .status(status)
                .build();
        acquisitionMapper.insert(acquisition);
        return acquisition;
    }

    private void assertError(org.junit.jupiter.api.function.Executable executable,
                             cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        ServiceException exception = assertThrows(ServiceException.class, executable);
        assertEquals(errorCode.getCode(), exception.getCode());
    }

}
