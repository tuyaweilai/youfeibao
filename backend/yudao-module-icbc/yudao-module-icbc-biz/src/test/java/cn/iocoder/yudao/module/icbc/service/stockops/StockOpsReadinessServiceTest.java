package cn.iocoder.yudao.module.icbc.service.stockops;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpsReadinessRespVO;
import cn.iocoder.yudao.module.icbc.service.stockops.impl.StockOpsReadinessServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * {@link StockOpsReadinessServiceImpl} 的单元测试（#54 T16，AC4）。
 *
 * <p>「当前库存」这个称呼的两层条件：四项能力（期初 / 出库 / 调拨 / 盘点）齐备，且**已导入期初**。
 * 只有入库记录时只能说「累计入库」，否则数字会被误读（规格 #38 user story 37）。
 */
public class StockOpsReadinessServiceTest extends BaseMockitoUnitTest {

    @Mock
    private StockOpeningService stockOpeningService;
    @InjectMocks
    private StockOpsReadinessServiceImpl readinessService;

    @Test
    public void testReadiness_withoutOpening_stillCallsItCumulative() {
        when(stockOpeningService.hasActiveOpening()).thenReturn(false);

        StockOpsReadinessRespVO resp = readinessService.getReadiness();

        // 四项能力都具备（#54 已落地）
        assertTrue(resp.getOpeningSupported());
        assertTrue(resp.getOutboundSupported());
        assertTrue(resp.getMoveSupported());
        assertTrue(resp.getCheckSupported());
        assertTrue(resp.getCapabilitiesReady());
        // 但没导期初 → 页面只能说「累计入库」
        assertFalse(resp.getOpeningImported());
        assertFalse(resp.getCurrentStockReady());
        assertEquals("累计入库", resp.getLabel());
        assertTrue(resp.getMissingItems().contains("尚未导入期初"));
        assertTrue(resp.getNotice().contains("累计入库"));
    }

    @Test
    public void testReadiness_withOpening_callsItCurrentStock() {
        when(stockOpeningService.hasActiveOpening()).thenReturn(true);

        StockOpsReadinessRespVO resp = readinessService.getReadiness();

        assertTrue(resp.getOpeningImported());
        assertTrue(resp.getCurrentStockReady());
        assertEquals("当前库存", resp.getLabel());
        assertTrue(resp.getMissingItems().isEmpty());
        assertTrue(resp.getNotice().contains("当前库存"));
    }

}
