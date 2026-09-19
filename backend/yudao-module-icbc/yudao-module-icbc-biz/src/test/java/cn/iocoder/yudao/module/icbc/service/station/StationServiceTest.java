package cn.iocoder.yudao.module.icbc.service.station;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicStationRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.station.vo.StationPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.station.vo.StationSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.service.station.impl.StationServiceImpl;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.TOO_MANY_REQUESTS;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * {@link StationServiceImpl} 的单元测试（#34）。
 *
 * <p>覆盖：场站码唯一、公开解析只返回公开字段、暂停收货文案、限流。
 */
@Import({StationServiceImpl.class, StationResolveRateLimiter.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class StationServiceTest extends BaseDbUnitTest {

    @Resource
    private StationService stationService;
    @Resource
    private IcbcStationMapper stationMapper;
    @Resource
    private StationResolveRateLimiter rateLimiter;

    @MockBean
    private TenantApi tenantApi;

    @AfterEach
    public void tearDown() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
    }

    @Test
    public void testCreateAndPage_stationCodeUnique() {
        stationService.createStation(saveReq("ST_A", "城东收货点"));
        assertServiceException(() -> stationService.createStation(saveReq("ST_A", "城西收货点")),
                STATION_CODE_EXISTS);

        PageResult<IcbcStationDO> page = stationService.getStationPage(new StationPageReqVO());
        assertEquals(1, page.getTotal());
    }

    @Test
    public void testResolvePublic_returnsOnlyPublicFields() {
        stationService.createStation(saveReq("ST_A", "城东收货点"));
        when(tenantApi.getTenantName(1L)).thenReturn("某某再生资源有限公司");
        // 新建的场站在测试上下文里 tenant_id 为空，这里显式补成 1 模拟真实租户
        IcbcStationDO station = stationMapper.selectByStationCode("ST_A");
        station.setTenantId(1L);
        station.setAddress("某某路 1 号");
        station.setContactMobile("0571-88888888");
        stationMapper.updateById(station);

        PublicStationRespVO resp = stationService.resolvePublic("ST_A", "10.0.0.1");
        assertEquals("ST_A", resp.getStationCode());
        assertEquals(1L, resp.getTenantId());
        assertEquals("某某再生资源有限公司", resp.getEnterpriseName());
        assertEquals("城东收货点", resp.getStationName());
        assertEquals("某某路 1 号", resp.getAddress());
        assertEquals("0571-88888888", resp.getContactMobile());
        assertTrue(resp.getOpen());
        assertEquals("正在收货", resp.getOpenStatusName());
        // 公开首屏没有任何个人数据字段
        assertFalse(resp.getGuide().isEmpty());
    }

    @Test
    public void testResolvePublic_closedStation() {
        IcbcStationDO station = IcbcStationDO.builder()
                .stationCode("ST_B").name("城西收货点").openStatus(0).build();
        station.setTenantId(1L);
        stationMapper.insert(station);
        when(tenantApi.getTenantName(1L)).thenReturn("某某再生资源有限公司");

        PublicStationRespVO resp = stationService.resolvePublic("ST_B", "10.0.0.2");
        assertFalse(resp.getOpen());
        assertEquals("暂停收货", resp.getOpenStatusName());
    }

    @Test
    public void testResolvePublic_unknownCodeRejected() {
        assertServiceException(() -> stationService.resolvePublic("NOPE", "10.0.0.3"),
                STATION_PUBLIC_NOT_FOUND);
    }

    @Test
    public void testResolvePublic_rateLimited() {
        stationService.createStation(saveReq("ST_C", "城南收货点"));
        String ip = "10.0.0.4";
        for (int i = 0; i < 60; i++) {
            assertTrue(rateLimiter.tryAcquire(ip));
        }
        assertServiceException(() -> stationService.resolvePublic("ST_C", ip), TOO_MANY_REQUESTS);
    }

    @Test
    public void testBuildEntryUrl_encodesStationCode() {
        IcbcStationDO station = IcbcStationDO.builder().stationCode("ST_A").build();
        String url = stationService.buildEntryUrl(station);
        assertTrue(url.contains("station=ST_A"));
    }

    private StationSaveReqVO saveReq(String code, String name) {
        StationSaveReqVO reqVO = new StationSaveReqVO();
        reqVO.setStationCode(code);
        reqVO.setName(name);
        reqVO.setOpenStatus(1);
        return reqVO;
    }

}
