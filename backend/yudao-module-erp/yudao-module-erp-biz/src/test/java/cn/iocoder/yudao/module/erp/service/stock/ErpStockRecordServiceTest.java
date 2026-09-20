package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.UnitTestConfiguration;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ErpStockRecordServiceImpl} 的单元测试。
 *
 * <p>核心不变式：{@code erp_stock.count} 恒等于同名 (品类, 仓库) 下全部 {@code erp_stock_record.count} 之和。
 */
@Import({ErpStockRecordServiceImpl.class, ErpStockServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class ErpStockRecordServiceTest extends BaseDbUnitTest {

    @Resource
    private ErpStockRecordServiceImpl stockRecordService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockRecordMapper stockRecordMapper;
    @Resource
    private ErpStockMapper stockMapper;

    @MockBean
    private ErpWarehouseService warehouseService;

    @Test
    public void testCreateStockRecord_balanceRecomputableFromRecords() {
        // 准备：两个品类 × 两个仓库的若干进出流水
        createRecord(1L, 1L, "10", 90, 1L, 1L, "R1");
        createRecord(1L, 1L, "-3", 20, 2L, 2L, "O1");
        createRecord(1L, 2L, "5", 90, 3L, 3L, "R2");
        createRecord(2L, 1L, "8", 90, 4L, 4L, "R3");

        // 断言一：逐 key 重算，余额 = 全部流水之和
        for (ErpStockDO stock : stockMapper.selectList()) {
            BigDecimal sum = stockRecordMapper.selectList().stream()
                    .filter(record -> record.getGoodsConfigId().equals(stock.getGoodsConfigId())
                            && record.getWarehouseId().equals(stock.getWarehouseId()))
                    .map(ErpStockRecordDO::getCount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, sum.compareTo(stock.getCount()),
                    "品类 " + stock.getGoodsConfigId() + " 仓库 " + stock.getWarehouseId() + " 余额与流水不一致");
        }
        // 断言二：具体数字
        assertEquals(0, new BigDecimal("7").compareTo(stockService.getStockCount(1L, 1L)));
        assertEquals(0, new BigDecimal("5").compareTo(stockService.getStockCount(1L, 2L)));
        assertEquals(0, new BigDecimal("8").compareTo(stockService.getStockCount(2L, 1L)));
    }

    @Test
    public void testExistsStockRecord() {
        createRecord(1L, 1L, "1", 90, 1L, 1L, "R1");

        assertTrue(stockRecordService.existsStockRecord(90, 1L, 1L));
        assertFalse(stockRecordService.existsStockRecord(90, 1L, 2L)); // 业务项不同
        assertFalse(stockRecordService.existsStockRecord(91, 1L, 1L)); // 业务类型不同
    }

    private void createRecord(Long goodsConfigId, Long warehouseId, String count, Integer bizType,
                              Long bizId, Long bizItemId, String bizNo) {
        stockRecordService.createStockRecord(ErpStockRecordCreateReqBO.builder()
                .goodsConfigId(goodsConfigId).warehouseId(warehouseId).count(new BigDecimal(count))
                .bizType(bizType).bizId(bizId).bizItemId(bizItemId).bizNo(bizNo).build());
    }

}
