package cn.iocoder.yudao.module.icbc.service.stockops;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockAdjustReqDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockCheckDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockCheckItemDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockCheckItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockCheckMapper;
import cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum;
import cn.iocoder.yudao.module.icbc.service.stockops.impl.StockCheckServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link StockCheckServiceImpl} 的单元测试（#54 T16）。
 *
 * <p>盘点与调拨的区别是「对齐到实盘数」：差额由 ERP 在 {@code StockApi#adjustTo} 里算并写流水，
 * 本类把实盘数递过去、把返回的差额落明细。作废按**记录的差额**冲销，不重算——这里两条都断言。
 */
@Import({StockCheckServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class StockCheckServiceTest extends BaseDbUnitTest {

    @Resource
    private StockCheckService stockCheckService;
    @Resource
    private IcbcStockCheckMapper stockCheckMapper;
    @Resource
    private IcbcStockCheckItemMapper stockCheckItemMapper;

    @MockBean
    private StockApi stockApi;

    @Test
    public void testCreateStockCheck_doesNotTouchStock() {
        Long id = stockCheckService.createStockCheck(buildReq("4800"));

        verify(stockApi, never()).adjustTo(any());
        IcbcStockCheckDO stockCheck = stockCheckMapper.selectById(id);
        assertEquals(StockOpsStatusEnum.PENDING.getStatus(), stockCheck.getStatus());

        // 待过账时只有实盘数；账面与差额要等过账时算
        IcbcStockCheckItemDO item = stockCheckItemMapper.selectListByCheckId(id).get(0);
        assertEquals(0, new BigDecimal("4800").compareTo(item.getActualQuantity()));
        assertNull(item.getBookQuantity());
        assertNull(item.getDifferenceQuantity());
    }

    @Test
    public void testPostStockCheck_adjustsToActualAndPersistsDifference() {
        Long id = stockCheckService.createStockCheck(buildReq("4800"));
        when(stockApi.adjustTo(any())).thenReturn(new BigDecimal("-200"));

        stockCheckService.postStockCheck(id);

        // 契约：把实盘数交给 ERP，差额由 ERP 算
        ArgumentCaptor<StockAdjustReqDTO> captor = ArgumentCaptor.forClass(StockAdjustReqDTO.class);
        verify(stockApi).adjustTo(captor.capture());
        StockAdjustReqDTO reqDTO = captor.getValue();
        assertEquals(0, new BigDecimal("4800").compareTo(reqDTO.getTargetCount()));
        assertEquals(1L, reqDTO.getWarehouseId());
        assertEquals(id, reqDTO.getBizId());

        // 账面 = 实盘 − 差额；差额落明细，供作废与追溯使用
        IcbcStockCheckItemDO item = stockCheckItemMapper.selectListByCheckId(id).get(0);
        assertEquals(0, new BigDecimal("-200").compareTo(item.getDifferenceQuantity()));
        assertEquals(0, new BigDecimal("5000").compareTo(item.getBookQuantity()));
        assertEquals(StockOpsStatusEnum.POSTED.getStatus(), stockCheckMapper.selectById(id).getStatus());
    }

    @Test
    public void testPostStockCheck_idempotent() {
        Long id = stockCheckService.createStockCheck(buildReq("4800"));
        when(stockApi.adjustTo(any())).thenReturn(new BigDecimal("-200"));

        stockCheckService.postStockCheck(id);
        stockCheckService.postStockCheck(id);

        verify(stockApi, times(1)).adjustTo(any());
    }

    @Test
    public void testCancelStockCheck_lessAdjustmentGoesBackIn() {
        Long id = stockCheckService.createStockCheck(buildReq("4800"));
        when(stockApi.adjustTo(any())).thenReturn(new BigDecimal("-200")); // 盘亏
        stockCheckService.postStockCheck(id);

        stockCheckService.cancelStockCheck(buildCancelReq(id, "实盘抄错"));

        // 盘亏的冲销是「加回」，业务类型 CHECK_LESS_OUT_CANCEL
        ArgumentCaptor<StockChangeReqDTO> captor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi).in(captor.capture());
        assertEquals(ErpStockRecordBizTypeEnum.CHECK_LESS_OUT_CANCEL.getType(), captor.getValue().getBizType());
        assertEquals(0, new BigDecimal("200").compareTo(captor.getValue().getCount()));
        verify(stockApi, never()).out(any());
        assertEquals(StockOpsStatusEnum.CANCELLED.getStatus(), stockCheckMapper.selectById(id).getStatus());
    }

    @Test
    public void testCancelStockCheck_moreAdjustmentGoesBackOut() {
        Long id = stockCheckService.createStockCheck(buildReq("5200"));
        when(stockApi.adjustTo(any())).thenReturn(new BigDecimal("200")); // 盘盈
        stockCheckService.postStockCheck(id);

        stockCheckService.cancelStockCheck(buildCancelReq(id, "实盘抄错"));

        ArgumentCaptor<StockChangeReqDTO> captor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi).out(captor.capture());
        assertEquals(ErpStockRecordBizTypeEnum.CHECK_MORE_IN_CANCEL.getType(), captor.getValue().getBizType());
        assertEquals(0, new BigDecimal("200").compareTo(captor.getValue().getCount()));
        verify(stockApi, never()).in(any());
    }

    @Test
    public void testCancelStockCheck_pendingDoesNotTouchStock() {
        Long id = stockCheckService.createStockCheck(buildReq("4800"));
        stockCheckService.cancelStockCheck(buildCancelReq(id, "登记错单据"));
        verify(stockApi, never()).in(any());
        verify(stockApi, never()).out(any());
        assertEquals(StockOpsStatusEnum.CANCELLED.getStatus(), stockCheckMapper.selectById(id).getStatus());
    }

    @Test
    public void testCreateStockCheck_negativeActualRejected() {
        assertServiceException(() -> stockCheckService.createStockCheck(buildReq("-1")),
                STOCK_CHECK_ACTUAL_NEGATIVE);
    }

    @Test
    public void testCreateStockCheck_invalidItemRejected() {
        StockCheckSaveReqVO req = buildReq("10");
        req.getItems().get(0).setWarehouseId(null);
        assertServiceException(() -> stockCheckService.createStockCheck(req),
                STOCK_CHECK_ITEM_INVALID, "每条明细都要选择品类与仓库");
    }

    @Test
    public void testGetStockCheck_detailKeepsDifference() {
        Long id = stockCheckService.createStockCheck(buildReq("4800"));
        when(stockApi.adjustTo(any())).thenReturn(new BigDecimal("-200"));
        stockCheckService.postStockCheck(id);

        StockCheckRespVO detail = stockCheckService.getStockCheck(id);
        assertEquals("已过账", detail.getStatusName());
        assertEquals(1, detail.getItems().size());
        assertEquals(0, new BigDecimal("-200").compareTo(detail.getItems().get(0).getDifferenceQuantity()));
    }

    @Test
    public void testGetStockCheck_notExists() {
        assertServiceException(() -> stockCheckService.getStockCheck(9999L), STOCK_CHECK_NOT_EXISTS);
    }

    private static StockCheckSaveReqVO buildReq(String actualQuantity) {
        StockCheckItemReqVO item = new StockCheckItemReqVO();
        item.setGoodsConfigId(1L);
        item.setWarehouseId(1L);
        item.setLocationId(0L);
        item.setBatchId(0L);
        item.setActualQuantity(new BigDecimal(actualQuantity));
        StockCheckSaveReqVO req = new StockCheckSaveReqVO();
        req.setItems(List.of(item));
        req.setRemark("测试");
        return req;
    }

    private static StockCheckCancelReqVO buildCancelReq(Long id, String reason) {
        StockCheckCancelReqVO req = new StockCheckCancelReqVO();
        req.setId(id);
        req.setReason(reason);
        return req;
    }

}
