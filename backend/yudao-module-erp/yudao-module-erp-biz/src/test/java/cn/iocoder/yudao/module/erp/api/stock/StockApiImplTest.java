package cn.iocoder.yudao.module.erp.api.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.UnitTestConfiguration;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockAdjustReqDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockMoveReqDTO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
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
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_COUNT_NEGATIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_EXCEED_AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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

    // ==================== 跨仓调拨（#54） ====================

    @Test
    public void testMove_betweenWarehouses() {
        // 先给 1 号仓备 10
        stockApi.in(buildReq("10", ErpStockRecordBizTypeEnum.RECEIPT_IN.getType(), 1L, 1L, "R1"));

        // 调 4 到 2 号仓
        assertEquals(0, new BigDecimal("4").compareTo(
                stockApi.move(buildMoveReq("4", 1L, 2L, 2L, "SM1"))));

        // 余额与流水：1 号仓 6、2 号仓 4；一进一出两条调拨流水
        assertEquals(0, new BigDecimal("6").compareTo(stockApi.getStockCount(1L, 1L)));
        assertEquals(0, new BigDecimal("4").compareTo(stockApi.getStockCount(1L, 2L)));
        assertEquals(0, new BigDecimal("10").compareTo(stockApi.getStockSum(1L)));
        List<ErpStockRecordDO> records = stockRecordMapper.selectList();
        assertEquals(3L, records.size());
        assertEquals(1L, records.stream().filter(r ->
                ErpStockRecordBizTypeEnum.MOVE_OUT.getType().equals(r.getBizType())).count());
        assertEquals(1L, records.stream().filter(r ->
                ErpStockRecordBizTypeEnum.MOVE_IN.getType().equals(r.getBizType())).count());
        // 余额 = 该维度全部流水重算（余额与流水始终一致）
        BigDecimal fromRecords = records.stream()
                .filter(r -> Long.valueOf(1L).equals(r.getWarehouseId()))
                .map(ErpStockRecordDO::getCount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, fromRecords.compareTo(stockApi.getStockCount(1L, 1L)));
        BigDecimal toRecords = records.stream()
                .filter(r -> Long.valueOf(2L).equals(r.getWarehouseId()))
                .map(ErpStockRecordDO::getCount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, toRecords.compareTo(stockApi.getStockCount(1L, 2L)));
    }

    @Test
    public void testMove_idempotentByBizItem() {
        stockApi.in(buildReq("10", ErpStockRecordBizTypeEnum.RECEIPT_IN.getType(), 1L, 1L, "R1"));
        StockMoveReqDTO req = buildMoveReq("4", 1L, 2L, 2L, "SM1");

        // 同一业务项重复调拨：不再搬一次
        assertEquals(0, new BigDecimal("4").compareTo(stockApi.move(req)));
        assertEquals(0, new BigDecimal("4").compareTo(stockApi.move(req)));
        assertEquals(0, new BigDecimal("6").compareTo(stockApi.getStockCount(1L, 1L)));
        assertEquals(0, new BigDecimal("4").compareTo(stockApi.getStockCount(1L, 2L)));
        assertEquals(3L, stockRecordMapper.selectCount().longValue()); // 1 入库 + 2 调拨
    }

    @Test
    public void testMove_insufficientSourceRejectsWholeMove() {
        stockApi.in(buildReq("3", ErpStockRecordBizTypeEnum.RECEIPT_IN.getType(), 1L, 1L, "R1"));
        when(warehouseService.getWarehouse(1L))
                .thenReturn(new ErpWarehouseDO().setId(1L).setName("一号仓"));

        // 源仓只有 3，搬 5 应被拦住，且目标仓不进账
        assertServiceException(() -> stockApi.move(buildMoveReq("5", 1L, 2L, 2L, "SM2")),
                STOCK_COUNT_NEGATIVE, "1", "一号仓", new BigDecimal("3.000000"), new BigDecimal("-5"));
        assertEquals(0, new BigDecimal("3").compareTo(stockApi.getStockCount(1L, 1L)));
        assertEquals(0, BigDecimal.ZERO.compareTo(stockApi.getStockCount(1L, 2L)));
    }

    // ==================== 盘点调整（#54） ====================

    @Test
    public void testAdjustTo_moreAndLess() {
        stockApi.in(buildReq("10", ErpStockRecordBizTypeEnum.RECEIPT_IN.getType(), 1L, 1L, "R1"));

        // 盘亏：账面 10、实盘 7 → 差额 -3
        assertEquals(0, new BigDecimal("-3").compareTo(
                stockApi.adjustTo(buildAdjustReq("7", 2L, 2L, "SC1"))));
        assertEquals(0, new BigDecimal("7").compareTo(stockApi.getStockCount(1L, 1L)));

        // 盘盈：实盘 12 → 差额 +5
        assertEquals(0, new BigDecimal("5").compareTo(
                stockApi.adjustTo(buildAdjustReq("12", 3L, 3L, "SC2"))));
        assertEquals(0, new BigDecimal("12").compareTo(stockApi.getStockCount(1L, 1L)));

        // 流水：1 入库 + 1 盘亏 + 1 盘盈；余额 = 全部流水重算（余额与流水始终一致）
        List<ErpStockRecordDO> records = stockRecordMapper.selectList();
        assertEquals(3L, records.size());
        assertEquals(0, new BigDecimal("12").compareTo(records.stream()
                .map(ErpStockRecordDO::getCount).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    @Test
    public void testAdjustTo_noDifferenceWritesNothing() {
        stockApi.in(buildReq("10", ErpStockRecordBizTypeEnum.RECEIPT_IN.getType(), 1L, 1L, "R1"));

        // 账实相符：差额 0，不写流水
        assertEquals(0, BigDecimal.ZERO.compareTo(
                stockApi.adjustTo(buildAdjustReq("10", 2L, 2L, "SC1"))));
        assertEquals(1L, stockRecordMapper.selectCount().longValue());
    }

    @Test
    public void testAdjustTo_idempotentByBizItem() {
        stockApi.in(buildReq("10", ErpStockRecordBizTypeEnum.RECEIPT_IN.getType(), 1L, 1L, "R1"));
        StockAdjustReqDTO req = buildAdjustReq("6", 2L, 2L, "SC1");

        // 同一业务项重复过账：第二次返回 0，余额不再变
        assertEquals(0, new BigDecimal("-4").compareTo(stockApi.adjustTo(req)));
        assertEquals(0, BigDecimal.ZERO.compareTo(stockApi.adjustTo(req)));
        assertEquals(0, new BigDecimal("6").compareTo(stockApi.getStockCount(1L, 1L)));
        assertEquals(2L, stockRecordMapper.selectCount().longValue());
    }

    private static StockMoveReqDTO buildMoveReq(String count, Long fromWarehouseId, Long toWarehouseId,
                                                Long bizItemId, String bizNo) {
        StockMoveReqDTO req = new StockMoveReqDTO();
        req.setGoodsConfigId(1L);
        req.setFromWarehouseId(fromWarehouseId);
        req.setToWarehouseId(toWarehouseId);
        req.setCount(new BigDecimal(count));
        req.setBizId(9L);
        req.setBizItemId(bizItemId);
        req.setBizNo(bizNo);
        return req;
    }

    private static StockAdjustReqDTO buildAdjustReq(String targetCount, Long bizId, Long bizItemId, String bizNo) {
        StockAdjustReqDTO req = new StockAdjustReqDTO();
        req.setGoodsConfigId(1L);
        req.setWarehouseId(1L);
        req.setTargetCount(new BigDecimal(targetCount));
        req.setBizId(bizId);
        req.setBizItemId(bizItemId);
        req.setBizNo(bizNo);
        return req;
    }

}
