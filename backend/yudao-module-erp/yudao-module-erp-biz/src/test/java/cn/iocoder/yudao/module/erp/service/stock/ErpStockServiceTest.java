package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.UnitTestConfiguration;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_COUNT_NEGATIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * {@link ErpStockServiceImpl} 的单元测试。
 *
 * <p>锁住两件事：余额行按「品类 + 仓库」唯一；库存不足时拦住出库。
 */
@Import({ErpStockServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class ErpStockServiceTest extends BaseDbUnitTest {

    private static final Long GOODS_CONFIG_ID = 100L;
    private static final Long WAREHOUSE_ID = 10L;

    @Resource
    private ErpStockServiceImpl stockService;
    @Resource
    private ErpStockMapper stockMapper;

    @MockBean
    private ErpWarehouseService warehouseService;

    @Test
    public void testUpdateStockCountIncrement_insertThenIncrement() {
        // 首次：建余额行
        assertEquals(0, new BigDecimal("10").compareTo(
                stockService.updateStockCountIncrement(GOODS_CONFIG_ID, WAREHOUSE_ID, new BigDecimal("10"))));
        // 再次：增量更新
        assertEquals(0, new BigDecimal("7").compareTo(
                stockService.updateStockCountIncrement(GOODS_CONFIG_ID, WAREHOUSE_ID, new BigDecimal("-3"))));

        // 断言：同一 (品类, 仓库) 只有一行余额
        assertEquals(1L, stockMapper.selectCount().longValue());
        ErpStockDO stock = stockService.getStock(GOODS_CONFIG_ID, WAREHOUSE_ID);
        assertEquals(0, new BigDecimal("7").compareTo(stock.getCount()));
        assertEquals(0, new BigDecimal("7").compareTo(stockService.getStockCount(GOODS_CONFIG_ID, WAREHOUSE_ID)));
        assertEquals(0, new BigDecimal("7").compareTo(stockService.getStockCount(GOODS_CONFIG_ID)));
    }

    @Test
    public void testUpdateStockCountIncrement_negativeRejected() {
        stockService.updateStockCountIncrement(GOODS_CONFIG_ID, WAREHOUSE_ID, new BigDecimal("5"));
        when(warehouseService.getWarehouse(WAREHOUSE_ID))
                .thenReturn(new ErpWarehouseDO().setId(WAREHOUSE_ID).setName("一号仓"));

        // 调用：5 - 6 < 0，应被拦住
        assertServiceException(
                () -> stockService.updateStockCountIncrement(GOODS_CONFIG_ID, WAREHOUSE_ID, new BigDecimal("-6")),
                STOCK_COUNT_NEGATIVE, String.valueOf(GOODS_CONFIG_ID), "一号仓", new BigDecimal("5.000000"), new BigDecimal("-6"));
        // 库存不变
        assertEquals(0, new BigDecimal("5").compareTo(
                stockService.getStock(GOODS_CONFIG_ID, WAREHOUSE_ID).getCount()));
    }

}
