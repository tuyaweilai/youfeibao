package cn.iocoder.yudao.module.icbc.service.stockops;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockMoveReqDTO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMovePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockMoveDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockMoveItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockMoveMapper;
import cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum;
import cn.iocoder.yudao.module.icbc.service.stockops.impl.StockMoveServiceImpl;
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
 * {@link StockMoveServiceImpl} 的单元测试（#54 T16）。
 *
 * <p>断言外部可观察行为：调拨经 {@code StockApi#move} 走一条「源减目标加」的契约（而不是自己拼两条
 * 库存变更）、同一业务项只移动一次、作废按相反方向调回（源加回、目标减掉）。icbc 不直接碰
 * {@code erp_stock*}，所以这里 Mock {@link StockApi} 断言契约。
 */
@Import({StockMoveServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class StockMoveServiceTest extends BaseDbUnitTest {

    @Resource
    private StockMoveService stockMoveService;
    @Resource
    private IcbcStockMoveMapper stockMoveMapper;
    @Resource
    private IcbcStockMoveItemMapper stockMoveItemMapper;

    @MockBean
    private StockApi stockApi;

    @Test
    public void testCreateStockMove_doesNotTouchStock() {
        Long id = stockMoveService.createStockMove(buildReq("300"));

        verify(stockApi, never()).move(any());
        verify(stockApi, never()).in(any());
        verify(stockApi, never()).out(any());
        IcbcStockMoveDO stockMove = stockMoveMapper.selectById(id);
        assertEquals(StockOpsStatusEnum.PENDING.getStatus(), stockMove.getStatus());
        assertEquals(0, new BigDecimal("300").compareTo(stockMove.getTotalQuantity()));
    }

    @Test
    public void testPostStockMove_writesMoveContract() {
        Long id = stockMoveService.createStockMove(buildReq("300"));

        stockMoveService.postStockMove(id);

        ArgumentCaptor<StockMoveReqDTO> captor = ArgumentCaptor.forClass(StockMoveReqDTO.class);
        verify(stockApi).move(captor.capture());
        StockMoveReqDTO reqDTO = captor.getValue();
        assertEquals(1L, reqDTO.getFromWarehouseId());
        assertEquals(10L, reqDTO.getFromLocationId());
        assertEquals(2L, reqDTO.getToWarehouseId());
        assertEquals(20L, reqDTO.getToLocationId());
        assertEquals(0, new BigDecimal("300").compareTo(reqDTO.getCount()));
        assertEquals(id, reqDTO.getBizId());
        assertEquals(stockMoveItemMapper.selectListByMoveId(id).get(0).getId(), reqDTO.getBizItemId());
        assertEquals(StockOpsStatusEnum.POSTED.getStatus(), stockMoveMapper.selectById(id).getStatus());
    }

    @Test
    public void testPostStockMove_idempotent() {
        Long id = stockMoveService.createStockMove(buildReq("300"));
        stockMoveService.postStockMove(id);
        stockMoveService.postStockMove(id);
        verify(stockApi, times(1)).move(any());
    }

    @Test
    public void testCreateStockMove_samePositionRejected() {
        StockMoveSaveReqVO req = buildReq("300");
        req.getItems().get(0).setToWarehouseId(1L);
        req.getItems().get(0).setToLocationId(10L);
        assertServiceException(() -> stockMoveService.createStockMove(req), STOCK_MOVE_SAME_POSITION);
    }

    @Test
    public void testCreateStockMove_invalidItemRejected() {
        StockMoveSaveReqVO req = buildReq("300");
        req.getItems().get(0).setQuantity(BigDecimal.ZERO);
        assertServiceException(() -> stockMoveService.createStockMove(req),
                STOCK_MOVE_ITEM_INVALID, "调拨数量必须大于 0");

        StockMoveSaveReqVO noTarget = buildReq("300");
        noTarget.getItems().get(0).setToWarehouseId(null);
        assertServiceException(() -> stockMoveService.createStockMove(noTarget),
                STOCK_MOVE_ITEM_INVALID, "每条明细都要选择源仓库与目标仓库");
    }

    @Test
    public void testCancelStockMove_postedMovesBack() {
        Long id = stockMoveService.confirmStockMove(buildReq("300"));

        stockMoveService.cancelStockMove(buildCancelReq(id, "目标仓库选错"));

        // 源加回（MOVE_OUT_CANCEL 的入），目标减掉（MOVE_IN_CANCEL 的出）
        ArgumentCaptor<StockChangeReqDTO> inCaptor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi).in(inCaptor.capture());
        assertEquals(ErpStockRecordBizTypeEnum.MOVE_OUT_CANCEL.getType(), inCaptor.getValue().getBizType());
        assertEquals(1L, inCaptor.getValue().getWarehouseId());

        ArgumentCaptor<StockChangeReqDTO> outCaptor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi).out(outCaptor.capture());
        assertEquals(ErpStockRecordBizTypeEnum.MOVE_IN_CANCEL.getType(), outCaptor.getValue().getBizType());
        assertEquals(2L, outCaptor.getValue().getWarehouseId());

        IcbcStockMoveDO stockMove = stockMoveMapper.selectById(id);
        assertEquals(StockOpsStatusEnum.CANCELLED.getStatus(), stockMove.getStatus());
        assertEquals("目标仓库选错", stockMove.getCancelReason());
    }

    @Test
    public void testCancelStockMove_pendingDoesNotTouchStock() {
        Long id = stockMoveService.createStockMove(buildReq("300"));
        stockMoveService.cancelStockMove(buildCancelReq(id, "登记错单据"));
        verify(stockApi, never()).in(any());
        verify(stockApi, never()).out(any());
        assertEquals(StockOpsStatusEnum.CANCELLED.getStatus(), stockMoveMapper.selectById(id).getStatus());
    }

    @Test
    public void testGetAndPage() {
        Long id = stockMoveService.confirmStockMove(buildReq("300"));
        StockMoveRespVO detail = stockMoveService.getStockMove(id);
        assertEquals("已过账", detail.getStatusName());
        assertEquals(1, detail.getItems().size());

        StockMovePageReqVO pageReqVO = new StockMovePageReqVO();
        pageReqVO.setStatus(StockOpsStatusEnum.POSTED.getStatus());
        PageResult<StockMoveRespVO> page = stockMoveService.getStockMovePage(pageReqVO);
        assertEquals(1L, page.getTotal());
    }

    @Test
    public void testGetStockMove_notExists() {
        assertServiceException(() -> stockMoveService.getStockMove(9999L), STOCK_MOVE_NOT_EXISTS);
    }

    private static StockMoveSaveReqVO buildReq(String quantity) {
        StockMoveItemReqVO item = new StockMoveItemReqVO();
        item.setGoodsConfigId(1L);
        item.setFromWarehouseId(1L);
        item.setFromLocationId(10L);
        item.setFromBatchId(0L);
        item.setToWarehouseId(2L);
        item.setToLocationId(20L);
        item.setToBatchId(0L);
        item.setQuantity(new BigDecimal(quantity));
        StockMoveSaveReqVO req = new StockMoveSaveReqVO();
        req.setItems(List.of(item));
        req.setRemark("测试");
        return req;
    }

    private static StockMoveCancelReqVO buildCancelReq(Long id, String reason) {
        StockMoveCancelReqVO req = new StockMoveCancelReqVO();
        req.setId(id);
        req.setReason(reason);
        return req;
    }

}
