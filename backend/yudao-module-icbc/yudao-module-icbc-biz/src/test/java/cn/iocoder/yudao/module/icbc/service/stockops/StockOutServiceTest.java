package cn.iocoder.yudao.module.icbc.service.stockops;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockOutDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockOutItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockOutMapper;
import cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.StockOutTypeEnum;
import cn.iocoder.yudao.module.icbc.service.stockops.impl.StockOutServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link StockOutServiceImpl} 的单元测试（#54 T16）。
 *
 * <p>断言外部可观察行为：登记是否动库存、过账写了什么业务类型的流水、不同出库类型映射到哪个业务类型、
 * 重复过账幂等、作废是否按相反方向冲销、非法入参是否拦住。库存写入口是跨模块的 {@link StockApi}，
 * 按规格 #38 的测试决策用 Mock，断言的是与它的契约。
 */
@Import({StockOutServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class StockOutServiceTest extends BaseDbUnitTest {

    @Resource
    private StockOutService stockOutService;
    @Resource
    private IcbcStockOutMapper stockOutMapper;
    @Resource
    private IcbcStockOutItemMapper stockOutItemMapper;

    @MockBean
    private StockApi stockApi;

    // ==================== 登记：不动库存 ====================

    @Test
    public void testCreateStockOut_doesNotTouchStock() {
        Long id = stockOutService.createStockOut(buildReq(StockOutTypeEnum.SCRAP.getType(), "120"));

        // 待过账：不动库存
        verify(stockApi, never()).out(any());
        verify(stockApi, never()).in(any());
        IcbcStockOutDO stockOut = stockOutMapper.selectById(id);
        assertEquals(StockOpsStatusEnum.PENDING.getStatus(), stockOut.getStatus());
        assertNull(stockOut.getPostedTime());
        assertEquals(0, new BigDecimal("120").compareTo(stockOut.getTotalQuantity()));
        assertEquals(1, stockOutItemMapper.selectListByStockOutId(id).size());
    }

    @Test
    public void testCreateStockOut_invalidTypeRejected() {
        assertServiceException(() -> stockOutService.createStockOut(buildReq(99, "10")),
                STOCK_OUT_TYPE_INVALID, 99);
    }

    @Test
    public void testCreateStockOut_invalidItemRejected() {
        StockOutSaveReqVO req = buildReq(StockOutTypeEnum.SCRAP.getType(), "10");
        req.getItems().get(0).setQuantity(BigDecimal.ZERO);
        assertServiceException(() -> stockOutService.createStockOut(req),
                STOCK_OUT_ITEM_INVALID, "出库数量必须大于 0");

        StockOutSaveReqVO noWarehouse = buildReq(StockOutTypeEnum.SCRAP.getType(), "10");
        noWarehouse.getItems().get(0).setWarehouseId(null);
        assertServiceException(() -> stockOutService.createStockOut(noWarehouse),
                STOCK_OUT_ITEM_INVALID, "每条明细都要选择品类与仓库");
    }

    // ==================== 过账：写流水，业务类型按出库类型 ====================

    @Test
    public void testPostStockOut_writesScrapOutBizType() {
        Long id = stockOutService.createStockOut(buildReq(StockOutTypeEnum.SCRAP.getType(), "120"));

        stockOutService.postStockOut(id);

        ArgumentCaptor<StockChangeReqDTO> captor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi).out(captor.capture());
        StockChangeReqDTO reqDTO = captor.getValue();
        assertEquals(ErpStockRecordBizTypeEnum.SCRAP_OUT.getType(), reqDTO.getBizType());
        assertEquals(id, reqDTO.getBizId());
        assertEquals(stockOutItemMapper.selectListByStockOutId(id).get(0).getId(), reqDTO.getBizItemId());
        assertEquals(0, new BigDecimal("120").compareTo(reqDTO.getCount()));

        IcbcStockOutDO stockOut = stockOutMapper.selectById(id);
        assertEquals(StockOpsStatusEnum.POSTED.getStatus(), stockOut.getStatus());
        assertNotNull(stockOut.getPostedTime());
    }

    @Test
    public void testPostStockOut_mapsEveryOutType() {
        Long returnId = stockOutService.confirmStockOut(buildReq(StockOutTypeEnum.RETURN.getType(), "5"));
        Long internalId = stockOutService.confirmStockOut(buildReq(StockOutTypeEnum.INTERNAL_USE.getType(), "6"));

        ArgumentCaptor<StockChangeReqDTO> captor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi, times(2)).out(captor.capture());
        List<Integer> bizTypes = captor.getAllValues().stream().map(StockChangeReqDTO::getBizType)
                .collect(Collectors.toList());
        assertTrue(bizTypes.contains(ErpStockRecordBizTypeEnum.RETURN_OUT.getType()));
        assertTrue(bizTypes.contains(ErpStockRecordBizTypeEnum.INTERNAL_USE_OUT.getType()));
        assertEquals(StockOpsStatusEnum.POSTED.getStatus(), stockOutMapper.selectById(returnId).getStatus());
        assertEquals(StockOpsStatusEnum.POSTED.getStatus(), stockOutMapper.selectById(internalId).getStatus());
    }

    @Test
    public void testPostStockOut_idempotent() {
        Long id = stockOutService.createStockOut(buildReq(StockOutTypeEnum.SCRAP.getType(), "120"));

        stockOutService.postStockOut(id);
        stockOutService.postStockOut(id);

        // 同一明细只写一次流水（StockApi 侧也按业务项幂等），重复过账不重复减库存
        verify(stockApi, times(1)).out(any());
    }

    // ==================== 作废：已过账的按相反方向冲销 ====================

    @Test
    public void testCancelStockOut_postedWritesReverseIn() {
        Long id = stockOutService.confirmStockOut(buildReq(StockOutTypeEnum.SCRAP.getType(), "120"));

        stockOutService.cancelStockOut(buildCancelReq(id, "数量录错"));

        ArgumentCaptor<StockChangeReqDTO> captor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi).in(captor.capture());
        assertEquals(ErpStockRecordBizTypeEnum.SCRAP_OUT_CANCEL.getType(), captor.getValue().getBizType());

        IcbcStockOutDO stockOut = stockOutMapper.selectById(id);
        assertEquals(StockOpsStatusEnum.CANCELLED.getStatus(), stockOut.getStatus());
        assertEquals("数量录错", stockOut.getCancelReason());
    }

    @Test
    public void testCancelStockOut_pendingDoesNotTouchStock() {
        Long id = stockOutService.createStockOut(buildReq(StockOutTypeEnum.SCRAP.getType(), "120"));

        stockOutService.cancelStockOut(buildCancelReq(id, "登记错单据"));

        // 从未过账 → 从未写流水 → 作废不冲销
        verify(stockApi, never()).in(any());
        verify(stockApi, never()).out(any());
        assertEquals(StockOpsStatusEnum.CANCELLED.getStatus(), stockOutMapper.selectById(id).getStatus());
    }

    @Test
    public void testCancelStockOut_twiceRejected() {
        Long id = stockOutService.createStockOut(buildReq(StockOutTypeEnum.SCRAP.getType(), "120"));
        stockOutService.cancelStockOut(buildCancelReq(id, "第一次"));
        assertServiceException(() -> stockOutService.cancelStockOut(buildCancelReq(id, "第二次")),
                STOCK_OUT_STATUS_NOT_ALLOW, StockOpsStatusEnum.CANCELLED.getName());
    }

    @Test
    public void testPostStockOut_cancelledRejected() {
        Long id = stockOutService.createStockOut(buildReq(StockOutTypeEnum.SCRAP.getType(), "120"));
        stockOutService.cancelStockOut(buildCancelReq(id, "作废了"));
        assertServiceException(() -> stockOutService.postStockOut(id),
                STOCK_OUT_STATUS_NOT_ALLOW, StockOpsStatusEnum.CANCELLED.getName());
    }

    // ==================== 查询 ====================

    @Test
    public void testGetAndPage() {
        Long id = stockOutService.confirmStockOut(buildReq(StockOutTypeEnum.RETURN.getType(), "30"));

        StockOutRespVO detail = stockOutService.getStockOut(id);
        assertEquals("退货出库", detail.getOutTypeName());
        assertEquals("已过账", detail.getStatusName());
        assertEquals(1, detail.getItems().size());

        StockOutPageReqVO pageReqVO = new StockOutPageReqVO();
        pageReqVO.setOutType(StockOutTypeEnum.RETURN.getType());
        PageResult<StockOutRespVO> page = stockOutService.getStockOutPage(pageReqVO);
        assertEquals(1L, page.getTotal());
        pageReqVO.setOutType(StockOutTypeEnum.SCRAP.getType());
        assertEquals(0L, stockOutService.getStockOutPage(pageReqVO).getTotal());
    }

    @Test
    public void testGetStockOut_notExists() {
        assertServiceException(() -> stockOutService.getStockOut(9999L), STOCK_OUT_NOT_EXISTS);
    }

    // ==================== 构造 ====================

    private static StockOutSaveReqVO buildReq(Integer outType, String quantity) {
        StockOutItemReqVO item = new StockOutItemReqVO();
        item.setGoodsConfigId(1L);
        item.setWarehouseId(2L);
        item.setLocationId(3L);
        item.setBatchId(0L);
        item.setQuantity(new BigDecimal(quantity));
        StockOutSaveReqVO req = new StockOutSaveReqVO();
        req.setOutType(outType);
        req.setItems(List.of(item));
        req.setRemark("测试");
        return req;
    }

    private static StockOutCancelReqVO buildCancelReq(Long id, String reason) {
        StockOutCancelReqVO req = new StockOutCancelReqVO();
        req.setId(id);
        req.setReason(reason);
        return req;
    }

}
