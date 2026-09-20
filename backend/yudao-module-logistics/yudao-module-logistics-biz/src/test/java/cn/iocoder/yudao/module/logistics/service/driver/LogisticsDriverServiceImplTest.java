package cn.iocoder.yudao.module.logistics.service.driver;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.driver.LogisticsDriverMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LogisticsDriverServiceImpl} 的单元测试（V2a #77）。
 *
 * <p>断言的是外部可观察行为：一个租户内一个用户只能建一份司机档案、查不到时的错、
 * 以及司机端登录要靠的「按用户查司机」。
 */
@Import({LogisticsDriverServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsDriverServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LogisticsDriverService logisticsDriverService;
    @Resource
    private LogisticsDriverMapper logisticsDriverMapper;

    @Test
    public void testCreateDriver_success() {
        Long id = logisticsDriverService.createDriver(newDriver(1024L, "张三", LogisticsDriverSourceEnum.SELF.getSource()));

        LogisticsDriverDO driver = logisticsDriverMapper.selectById(id);
        assertNotNull(driver);
        assertEquals(1024L, driver.getUserId());
        assertEquals("张三", driver.getName());
        assertEquals(LogisticsDriverSourceEnum.SELF.getSource(), driver.getSource());
        assertEquals(LogisticsDriverStatusEnum.ACTIVE.getStatus(), driver.getStatus());
    }

    @Test
    public void testCreateDriver_sameUserTwice_isRejected() {
        logisticsDriverService.createDriver(newDriver(1024L, "张三", LogisticsDriverSourceEnum.SELF.getSource()));

        assertServiceException(
                () -> logisticsDriverService.createDriver(newDriver(1024L, "李四", LogisticsDriverSourceEnum.CARRIER.getSource())),
                DRIVER_USER_DUPLICATE);
    }

    @Test
    public void testUpdateDriver_sameUserKeepsItsOwnProfile() {
        Long id = logisticsDriverService.createDriver(newDriver(1024L, "张三", LogisticsDriverSourceEnum.SELF.getSource()));

        LogisticsDriverSaveReqVO updateReqVO = newDriver(1024L, "张三丰", LogisticsDriverSourceEnum.CARRIER.getSource());
        updateReqVO.setId(id);
        logisticsDriverService.updateDriver(updateReqVO);

        LogisticsDriverDO driver = logisticsDriverMapper.selectById(id);
        assertEquals("张三丰", driver.getName());
        assertEquals(LogisticsDriverSourceEnum.CARRIER.getSource(), driver.getSource());
    }

    @Test
    public void testGetDriverByUserId_foundAndNotFound() {
        logisticsDriverService.createDriver(newDriver(1024L, "张三", LogisticsDriverSourceEnum.SELF.getSource()));

        LogisticsDriverDO found = logisticsDriverService.getDriverByUserId(1024L);
        assertNotNull(found);
        assertEquals("张三", found.getName());
        // 司机端登录后认不出司机（还没建档）与「查不到」是两种情形，前者要能给页面判空
        assertNull(logisticsDriverService.getDriverByUserId(2048L));
        assertNull(logisticsDriverService.getDriverByUserId(null));
    }

    @Test
    public void testGetDriver_notExists() {
        assertServiceException(() -> logisticsDriverService.getDriver(-1L), DRIVER_NOT_EXISTS);
    }

    @Test
    public void testDeleteDriver_thenUserCanBeReused() {
        Long id = logisticsDriverService.createDriver(newDriver(1024L, "张三", LogisticsDriverSourceEnum.SELF.getSource()));
        logisticsDriverService.deleteDriver(id);

        Long newId = logisticsDriverService.createDriver(newDriver(1024L, "张三", LogisticsDriverSourceEnum.SELF.getSource()));
        assertNotEquals(id, newId);
    }

    @Test
    public void testGetDriverPage_filterBySource() {
        logisticsDriverService.createDriver(newDriver(1024L, "张三", LogisticsDriverSourceEnum.SELF.getSource()));
        logisticsDriverService.createDriver(newDriver(2048L, "李四", LogisticsDriverSourceEnum.CARRIER.getSource()));

        LogisticsDriverPageReqVO pageReqVO = new LogisticsDriverPageReqVO();
        pageReqVO.setSource(LogisticsDriverSourceEnum.CARRIER.getSource());
        PageResult<LogisticsDriverDO> page = logisticsDriverService.getDriverPage(pageReqVO);

        assertEquals(1, page.getTotal());
        assertEquals("李四", page.getList().get(0).getName());
    }

    private static LogisticsDriverSaveReqVO newDriver(Long userId, String name, Integer source) {
        LogisticsDriverSaveReqVO reqVO = new LogisticsDriverSaveReqVO();
        reqVO.setUserId(userId);
        reqVO.setName(name);
        reqVO.setMobile("13800138000");
        reqVO.setSource(source);
        reqVO.setStatus(LogisticsDriverStatusEnum.ACTIVE.getStatus());
        return reqVO;
    }

}
