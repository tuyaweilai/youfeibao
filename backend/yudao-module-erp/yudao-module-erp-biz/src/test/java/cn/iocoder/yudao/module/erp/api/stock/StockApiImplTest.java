package cn.iocoder.yudao.module.erp.api.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.UnitTestConfiguration;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordServiceImpl;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockServiceImpl;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_EXCEED_AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link StockApiImpl} 的单元测试。
 *
 * <p>回收业务模块只通过这个接口碰库存：入库 / 出库 / 余额查询，且同一业务项幂等。
 */
@Import({StockApiImpl.class, ErpStockServiceImpl.class, ErpStockRecordServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class StockApiImplTest extends BaseDbUnitTest {

    @Resource
    private StockApi stockApi;
    @Resource
    private ErpStockRecordMapper stockRecordMapper;

    @MockBean
    private ErpWarehouseService warehouseService;

    @Test
    public void testInAndOutAndBalance() {
        // 入库
        assertEquals(0, new BigDecimal("10").compareTo(stockApi.in(buildReq(
                "10", ErpStockRecordBizTypeEnum.RECEIPT_IN.getType(), 1L, 1L, "R1"))));
        // 出库
        assertEquals(0, new BigDecimal("6").compareTo(stockApi.out(buildReq(
                "4", ErpStockRecordBizTypeEnum.OTHER_OUT.getType(), 2L, 2L, "O1"))));

        // 余额查询
        assertEquals(0, new BigDecimal("6").compareTo(stockApi.getStockCount(1L, 1L)));
        assertEquals(0, new BigDecimal("6").compareTo(stockApi.getStockSum(1L)));
        assertEquals(0, BigDecimal.ZERO.compareTo(stockApi.getStockCount(1L, 2L)));
    }

    @Test
    public void testIn_idempotentByBiz() {
        StockChangeReqDTO req = buildReq("10", ErpStockRecordBizTypeEnum.RECEIPT_IN.getType(), 1L, 1L, "R1");

        // 同一业务项重复提交
        assertEquals(0, new BigDecimal("10").compareTo(stockApi.in(req)));
        assertEquals(0, new BigDecimal("10").compareTo(stockApi.in(req)));

        // 断言：库存只加了一次，流水也只有一条
        assertEquals(0, new BigDecimal("10").compareTo(stockApi.getStockCount(1L, 1L)));
        assertEquals(1L, stockRecordMapper.selectCount().longValue());
    }

    @Test
    public void testIn_splitAcrossLocationsWithCap() {
        // 同一品类的一批货（同一 bizId）拆到两个库位，累计不超过可入库量 10
        assertEquals(0, new BigDecimal("6").compareTo(
                stockApi.in(buildReq("6", 90, 1L, 11L, "R1", 1L, 0L, "10"))));
        assertEquals(0, new BigDecimal("4").compareTo(
                stockApi.in(buildReq("4", 90, 1L, 12L, "R1", 2L, 0L, "10"))));

        // 两个库位各一行，合计 = 10
        assertEquals(0, new BigDecimal("6").compareTo(stockApi.getStockCount(1L, 1L, 1L, 0L)));
        assertEquals(0, new BigDecimal("4").compareTo(stockApi.getStockCount(1L, 1L, 2L, 0L)));
        assertEquals(0, new BigDecimal("10").compareTo(stockApi.getStockCount(1L, 1L)));
        assertEquals(0, new BigDecimal("10").compareTo(stockApi.getStockSum(1L)));

        // 再拆第三个库位就超过可入库量，拦住且不产生任何库存与流水
        assertServiceException(() ->
                        stockApi.in(buildReq("1", 90, 1L, 13L, "R1", 3L, 0L, "10")),
                STOCK_IN_EXCEED_AVAILABLE, new BigDecimal("10"), new BigDecimal("10.000000"), new BigDecimal("1"));
        assertEquals(2L, stockRecordMapper.selectCount().longValue());
    }

    private static StockChangeReqDTO buildReq(String count, Integer bizType, Long bizId, Long bizItemId, String bizNo) {
        return buildReq(count, bizType, bizId, bizItemId, bizNo, null, null, null);
    }

    private static StockChangeReqDTO buildReq(String count, Integer bizType, Long bizId, Long bizItemId, String bizNo,
                                              Long locationId, Long batchId, String maxCount) {
        StockChangeReqDTO req = new StockChangeReqDTO();
        req.setGoodsConfigId(1L);
        req.setWarehouseId(1L);
        req.setLocationId(locationId);
        req.setBatchId(batchId);
        req.setCount(new BigDecimal(count));
        if (maxCount != null) {
            req.setMaxCount(new BigDecimal(maxCount));
        }
        req.setBizType(bizType);
        req.setBizId(bizId);
        req.setBizItemId(bizItemId);
        req.setBizNo(bizNo);
        return req;
    }

}
