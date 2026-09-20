package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.UnitTestConfiguration;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location.ErpStockLocationPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location.ErpStockLocationSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLocationDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockLocationMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * {@link ErpStockLocationServiceImpl} 的单元测试。
 *
 * <p>锁住四件事：库位可增改查；同一仓库下名称唯一；还有库存的库位不允许删除；停用库位不能参与入库。
 */
@Import({ErpStockLocationServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class ErpStockLocationServiceTest extends BaseDbUnitTest {

    private static final Long WAREHOUSE_ID = 10L;

    @Resource
    private ErpStockLocationServiceImpl locationService;
    @Resource
    private ErpStockLocationMapper locationMapper;
    @Resource
    private ErpStockMapper stockMapper;

    @MockBean
    private ErpWarehouseService warehouseService;

    @Test
    public void testCreateUpdateGetAndPage() {
        when(warehouseService.getWarehouse(WAREHOUSE_ID))
                .thenReturn(new ErpWarehouseDO().setId(WAREHOUSE_ID).setName("一号仓"));

        // 创建
        Long id = locationService.createStockLocation(buildReq(null, "A 区 1 号堆", 1L));
        ErpStockLocationDO location = locationService.getStockLocation(id);
        assertEquals("A 区 1 号堆", location.getName());
        assertEquals(WAREHOUSE_ID, location.getWarehouseId());

        // 更新
        locationService.updateStockLocation(buildReq(id, "A 区 2 号堆", 2L));
        assertEquals("A 区 2 号堆", locationService.getStockLocation(id).getName());

        // 分页按仓库筛选
        ErpStockLocationPageReqVO pageReqVO = new ErpStockLocationPageReqVO();
        pageReqVO.setWarehouseId(WAREHOUSE_ID);
        PageResult<ErpStockLocationDO> page = locationService.getStockLocationPage(pageReqVO);
        assertEquals(1L, page.getTotal());
    }

    @Test
    public void testCreate_duplicateNameInWarehouse() {
        when(warehouseService.getWarehouse(WAREHOUSE_ID))
                .thenReturn(new ErpWarehouseDO().setId(WAREHOUSE_ID).setName("一号仓"));
        locationService.createStockLocation(buildReq(null, "A 区", 1L));

        assertServiceException(() -> locationService.createStockLocation(buildReq(null, "A 区", 2L)),
                STOCK_LOCATION_NAME_DUPLICATE, "A 区");
    }

    @Test
    public void testDelete_blockedWhenStockExists() {
        when(warehouseService.getWarehouse(WAREHOUSE_ID))
                .thenReturn(new ErpWarehouseDO().setId(WAREHOUSE_ID).setName("一号仓"));
        Long id = locationService.createStockLocation(buildReq(null, "A 区", 1L));
        // 该库位上有一行库存余额
        stockMapper.insert(new ErpStockDO().setGoodsConfigId(100L).setWarehouseId(WAREHOUSE_ID)
                .setLocationId(id).setBatchId(0L).setCount(BigDecimal.TEN));

        assertServiceException(() -> locationService.deleteStockLocation(id),
                STOCK_LOCATION_HAS_STOCK, "A 区");
        assertNotNull(locationService.getStockLocation(id));
    }

    @Test
    public void testDelete_ok() {
        when(warehouseService.getWarehouse(WAREHOUSE_ID))
                .thenReturn(new ErpWarehouseDO().setId(WAREHOUSE_ID).setName("一号仓"));
        Long id = locationService.createStockLocation(buildReq(null, "A 区", 1L));

        locationService.deleteStockLocation(id);
        assertNull(locationService.getStockLocation(id));
    }

    @Test
    public void testValidStockLocationList_disabledRejected() {
        when(warehouseService.getWarehouse(WAREHOUSE_ID))
                .thenReturn(new ErpWarehouseDO().setId(WAREHOUSE_ID).setName("一号仓"));
        ErpStockLocationSaveReqVO req = buildReq(null, "A 区", 1L);
        req.setStatus(1); // 停用
        Long id = locationService.createStockLocation(req);

        assertServiceException(() -> locationService.validStockLocationList(Collections.singletonList(id)),
                STOCK_LOCATION_NOT_ENABLE, "A 区");
    }

    private static ErpStockLocationSaveReqVO buildReq(Long id, String name, Long sort) {
        ErpStockLocationSaveReqVO req = new ErpStockLocationSaveReqVO();
        req.setId(id);
        req.setWarehouseId(WAREHOUSE_ID);
        req.setName(name);
        req.setSort(sort);
        req.setStatus(0);
        return req;
    }

}
