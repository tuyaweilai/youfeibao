package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.UnitTestConfiguration;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch.ErpStockBatchPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch.ErpStockBatchSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockBatchMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_BATCH_HAS_STOCK;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_BATCH_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_BATCH_NO_DUPLICATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link ErpStockBatchServiceImpl} 的单元测试。
 *
 * <p>锁住三件事：批次可维护；批次号唯一（含自动生成幂等）；还有库存的批次不允许删除。
 */
@Import({ErpStockBatchServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class ErpStockBatchServiceTest extends BaseDbUnitTest {

    @Resource
    private ErpStockBatchServiceImpl batchService;
    @Resource
    private ErpStockBatchMapper batchMapper;
    @Resource
    private ErpStockMapper stockMapper;

    @Test
    public void testGetOrCreateStockBatch_idempotentByBatchNo() {
        Long first = batchService.getOrCreateStockBatch("B20260920-01", 100L, LocalDateTime.now());
        Long second = batchService.getOrCreateStockBatch("B20260920-01", 100L, LocalDateTime.now());

        assertEquals(first, second);
        assertEquals(1L, batchMapper.selectCount().longValue());
        assertEquals("B20260920-01", batchService.getStockBatch(first).getBatchNo());
    }

    @Test
    public void testCreate_duplicateBatchNo() {
        batchService.createStockBatch(buildReq(null, "B1"));

        assertServiceException(() -> batchService.createStockBatch(buildReq(null, "B1")),
                STOCK_BATCH_NO_DUPLICATE, "B1");
    }

    @Test
    public void testDelete_blockedWhenStockExists() {
        Long id = batchService.createStockBatch(buildReq(null, "B1"));
        stockMapper.insert(new ErpStockDO().setGoodsConfigId(100L).setWarehouseId(10L)
                .setLocationId(0L).setBatchId(id).setCount(BigDecimal.ONE));

        assertServiceException(() -> batchService.deleteStockBatch(id), STOCK_BATCH_HAS_STOCK, "B1");
        assertNotNull(batchService.getStockBatch(id));
    }

    @Test
    public void testDelete_notExists() {
        assertServiceException(() -> batchService.deleteStockBatch(999L), STOCK_BATCH_NOT_EXISTS);
    }

    @Test
    public void testPageFilterByGoodsConfig() {
        batchService.createStockBatch(buildReq(null, "B1"));
        ErpStockBatchPageReqVO pageReqVO = new ErpStockBatchPageReqVO();
        pageReqVO.setGoodsConfigId(100L);
        PageResult<ErpStockBatchDO> page = batchService.getStockBatchPage(pageReqVO);
        assertEquals(1L, page.getTotal());
    }

    private static ErpStockBatchSaveReqVO buildReq(Long id, String batchNo) {
        ErpStockBatchSaveReqVO req = new ErpStockBatchSaveReqVO();
        req.setId(id);
        req.setBatchNo(batchNo);
        req.setGoodsConfigId(100L);
        req.setInTime(LocalDateTime.now());
        req.setStatus(0);
        return req;
    }

}
