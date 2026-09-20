package cn.iocoder.yudao.module.icbc.service.stockops;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningImportReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockOpeningDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockOpeningMapper;
import cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum;
import cn.iocoder.yudao.module.icbc.service.stockops.impl.StockOpeningServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link StockOpeningServiceImpl} 的单元测试（#54 T16）。
 *
 * <p>期初是「当前库存」的数据前提：导入即过账（写 {@code OPENING_IN} 流水），同一维度只允许一条
 * 生效期初，作废会冲销（{@code OPENING_IN_CANCEL}）并让该维度可重新导入。
 */
@Import({StockOpeningServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class StockOpeningServiceTest extends BaseDbUnitTest {

    @Resource
    private StockOpeningService stockOpeningService;
    @Resource
    private IcbcStockOpeningMapper stockOpeningMapper;

    @MockBean
    private StockApi stockApi;

    @Test
    public void testImportOpening_postsEveryRow() {
        String openingNo = stockOpeningService.importOpening(buildReq(
                buildItem(1L, 10L, "5000"), buildItem(2L, 10L, "3000")));

        // 每一行写一条 OPENING_IN 流水
        ArgumentCaptor<StockChangeReqDTO> captor = ArgumentCaptor.forClass(StockChangeReqDTO.class);
        verify(stockApi, times(2)).in(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(reqDTO ->
                ErpStockRecordBizTypeEnum.OPENING_IN.getType().equals(reqDTO.getBizType())));
        assertEquals(0, new BigDecimal("5000").compareTo(
                captor.getAllValues().get(0).getCount()));

        // 导入即过账，共享一个批次号
        List<IcbcStockOpeningDO> rows = stockOpeningMapper.selectList();
        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(row -> openingNo.equals(row.getOpeningNo())));
        assertTrue(rows.stream().allMatch(row ->
                StockOpsStatusEnum.POSTED.getStatus().equals(row.getStatus())));
        assertTrue(stockOpeningService.hasActiveOpening());
    }

    @Test
    public void testImportOpening_duplicateDimensionRejectedWholeBatch() {
        stockOpeningService.importOpening(buildReq(buildItem(1L, 10L, "5000")));

        // 同一「品类 + 仓库 + 库位 + 批次」再来一行：整批拒绝，库存不再加
        assertServiceException(() -> stockOpeningService.importOpening(buildReq(buildItem(1L, 10L, "800"))),
                STOCK_OPENING_DIMENSION_DUPLICATED);
        verify(stockApi, times(1)).in(any());
        assertEquals(1L, stockOpeningMapper.selectCount().longValue());
    }

    @Test
    public void testImportOpening_duplicateWithinSameBatchRejected() {
        assertServiceException(() -> stockOpeningService.importOpening(buildReq(
                        buildItem(1L, 10L, "5000"), buildItem(1L, 10L, "3000"))),
                STOCK_OPENING_DIMENSION_DUPLICATED);
        verify(stockApi, never()).in(any());
    }

    @Test
    public void testImportOpening_invalidItemRejected() {
        assertServiceException(() -> stockOpeningService.importOpening(buildReq(
                        buildItem(1L, 10L, "0"))),
                STOCK_OPENING_ITEM_INVALID, "期初数量必须大于 0");

        StockOpeningItemReqVO noGoods = buildItem(1L, 10L, "100");
        noGoods.setGoodsConfigId(null);
        assertServiceException(() -> stockOpeningService.importOpening(buildReq(noGoods)),
                STOCK_OPENING_ITEM_INVALID, "每行期初都要填写品类与仓库");
    }

    @Test
    public void testCancelOpening_reversesAndAllowsReimport() {
        String openingNo = stockOpeningService.importOpening(buildReq(buildItem(1L, 10L, "5000")));
        Long id = stockOpeningMapper.selectList().get(0).getId();

        stockOpeningService.cancelOpening(buildCancelReq(id, "数量录错"));
        verify(stockApi).out(argThat(reqDTO ->
                ErpStockRecordBizTypeEnum.OPENING_IN_CANCEL.getType().equals(reqDTO.getBizType())));

        IcbcStockOpeningDO opening = stockOpeningMapper.selectById(id);
        assertEquals(StockOpsStatusEnum.CANCELLED.getStatus(), opening.getStatus());
        assertEquals("数量录错", opening.getCancelReason());
        assertEquals(openingNo, opening.getOpeningNo());
        assertFalse(stockOpeningService.hasActiveOpening());

        // 作废后同一维度可重新导入
        stockOpeningService.importOpening(buildReq(buildItem(1L, 10L, "4800")));
        assertTrue(stockOpeningService.hasActiveOpening());
        assertEquals(2L, stockOpeningMapper.selectCount().longValue());
    }

    @Test
    public void testCancelOpening_twiceRejected() {
        stockOpeningService.importOpening(buildReq(buildItem(1L, 10L, "5000")));
        Long id = stockOpeningMapper.selectList().get(0).getId();
        stockOpeningService.cancelOpening(buildCancelReq(id, "第一次"));
        assertServiceException(() -> stockOpeningService.cancelOpening(buildCancelReq(id, "第二次")),
                STOCK_OPENING_ALREADY_CANCELLED);
    }

    @Test
    public void testCancelOpening_notExists() {
        assertServiceException(() -> stockOpeningService.cancelOpening(buildCancelReq(9999L, "不存在")),
                STOCK_OPENING_NOT_EXISTS);
    }

    @Test
    public void testHasActiveOpening_falseWhenEmpty() {
        assertFalse(stockOpeningService.hasActiveOpening());
    }

    @Test
    public void testGetOpeningPage_filtersByDimensionAndStatus() {
        stockOpeningService.importOpening(buildReq(buildItem(1L, 10L, "5000"), buildItem(2L, 20L, "3000")));

        StockOpeningPageReqVO pageReqVO = new StockOpeningPageReqVO();
        pageReqVO.setGoodsConfigId(1L);
        PageResult<StockOpeningRespVO> page = stockOpeningService.getOpeningPage(pageReqVO);
        assertEquals(1L, page.getTotal());
        assertEquals("已过账", page.getList().get(0).getStatusName());

        pageReqVO.setGoodsConfigId(null);
        pageReqVO.setWarehouseId(20L);
        assertEquals(1L, stockOpeningService.getOpeningPage(pageReqVO).getTotal());

        pageReqVO.setWarehouseId(null);
        pageReqVO.setStatus(StockOpsStatusEnum.CANCELLED.getStatus());
        assertEquals(0L, stockOpeningService.getOpeningPage(pageReqVO).getTotal());
    }

    // ==================== 构造 ====================

    private static StockOpeningImportReqVO buildReq(StockOpeningItemReqVO... items) {
        StockOpeningImportReqVO req = new StockOpeningImportReqVO();
        req.setItems(items.length == 0 ? Collections.emptyList() : Arrays.asList(items));
        req.setRemark("启用平台前的存量");
        return req;
    }

    private static StockOpeningItemReqVO buildItem(Long goodsConfigId, Long warehouseId, String quantity) {
        StockOpeningItemReqVO item = new StockOpeningItemReqVO();
        item.setGoodsConfigId(goodsConfigId);
        item.setWarehouseId(warehouseId);
        item.setLocationId(0L);
        item.setBatchId(0L);
        item.setQuantity(new BigDecimal(quantity));
        return item;
    }

    private static StockOpeningCancelReqVO buildCancelReq(Long id, String reason) {
        StockOpeningCancelReqVO req = new StockOpeningCancelReqVO();
        req.setId(id);
        req.setReason(reason);
        return req;
    }

}
