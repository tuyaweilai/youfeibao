package cn.iocoder.yudao.module.icbc.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 依赖方向与可注入性：回收业务模块（icbc）依赖 {@code erp-api} 暴露的 {@link StockApi}，
 * 可以把它当普通 Bean 注入调用。
 *
 * <p>反向依赖（erp → icbc）在 POM 上不存在；{@code yudao-module-erp-biz} 只依赖 {@code erp-api} 与
 * {@code system-api}。
 */
public class StockApiDependencyTest extends BaseMockitoUnitTest {

    @Mock
    private StockApi stockApi;

    @Test
    public void testStockApiInjectable() {
        when(stockApi.getStockCount(anyLong(), anyLong())).thenReturn(new BigDecimal("3"));

        // 调用：icbc 侧拿到的就是 erp 的 StockApi
        assertEquals(new BigDecimal("3"), stockApi.getStockCount(1L, 2L));
    }

}
