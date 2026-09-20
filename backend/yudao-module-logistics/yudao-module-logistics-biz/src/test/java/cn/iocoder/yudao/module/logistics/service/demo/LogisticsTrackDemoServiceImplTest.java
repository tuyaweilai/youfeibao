package cn.iocoder.yudao.module.logistics.service.demo;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.api.transport.LogisticsTransportApi;
import cn.iocoder.yudao.module.logistics.api.transport.LogisticsTransportApiImpl;
import cn.iocoder.yudao.module.logistics.controller.admin.demo.vo.LogisticsTransportTrackDemoRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCreateReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transportnode.LogisticsTransportNodeMapper;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportTaskMapper;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl;
import cn.iocoder.yudao.module.logistics.service.vehicle.LogisticsVehicleService;
import cn.iocoder.yudao.module.logistics.service.vehicle.impl.LogisticsVehicleServiceImpl;
import cn.iocoder.yudao.module.logistics.service.demo.impl.LogisticsTrackDemoServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
import cn.iocoder.yudao.module.logistics.service.transportnode.impl.LogisticsTransportNodeServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import cn.iocoder.yudao.module.logistics.service.transporttask.impl.LogisticsTransportTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.TRANSPORT_TASK_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 运输轨迹演示件的单元测试（V9 #76）。
 *
 * <p>这一票的验收不在「线画得好不好看」，而在**隔离**：演示数据一个字节都不能进证据链。
 * 所以下面大半断言都是否定式的——跑完之后节点表没多行、读取面看不到它、关掉就没有点。
 *
 * <p>税总 5 号公告第十七条把「运输发票或凭证」列为业务真实性材料；在给税局与银行看的证据链里
 * 放一条假路径，违反 ADR 0021「只讲可核验的事」。这条是它为什么必须有测试。
 */
@Import({LogisticsTrackDemoServiceImpl.class, LogisticsTransportTaskServiceImpl.class,
        LogisticsTransportNodeServiceImpl.class, LogisticsTransportApiImpl.class,
        LogisticsVehicleServiceImpl.class, LogisticsDriverServiceImpl.class,
        UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsTrackDemoServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LogisticsTrackDemoService logisticsTrackDemoService;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;
    @Resource
    private LogisticsTransportApi logisticsTransportApi;
    @Resource
    private LogisticsTransportTaskMapper logisticsTransportTaskMapper;
    @Resource
    private LogisticsTransportNodeMapper logisticsTransportNodeMapper;
    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsDriverService logisticsDriverService;

    @BeforeEach
    public void enableDemo() {
        // 默认是关的（演示能力不该在不该出现的地方自己出现），测试里显式打开
        ReflectionTestUtils.setField(logisticsTrackDemoService, "demoEnabled", true);
    }

    @Test
    public void testGetTrackDemo_neverWritesAnything() {
        Long taskId = createAssignedTask();
        // 先上报一个带经纬度的真实节点
        reportNode(taskId, "req-1");

        int nodesBefore = logisticsTransportNodeMapper.selectListByTaskId(taskId).size();
        LogisticsTransportTaskDO taskBefore = logisticsTransportTaskMapper.selectById(taskId);

        LogisticsTransportTrackDemoRespVO demo = logisticsTrackDemoService.getTransportTrackDemo(taskId);
        assertFalse(demo.getPoints().isEmpty(), "开着的时候要能给出演示轨迹");

        // 跑完之后：节点没多、任务也没被改
        assertEquals(nodesBefore, logisticsTransportNodeMapper.selectListByTaskId(taskId).size(),
                "演示数据绝不能写进节点表——那是证据链");
        LogisticsTransportTaskDO taskAfter = logisticsTransportTaskMapper.selectById(taskId);
        assertEquals(taskBefore.getStatus(), taskAfter.getStatus());
        assertEquals(taskBefore.getStartTime(), taskAfter.getStartTime());
    }

    @Test
    public void testGetTrackDemo_doesNotLeakIntoReadFace() {
        Long taskId = createAssignedTask();
        reportNode(taskId, "req-2");
        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);

        logisticsTrackDemoService.getTransportTrackDemo(taskId);

        // 读取面（icbc 一票一档消费的就是它）只该看到真实节点：一个
        List<?> fromReadFace = logisticsTransportApi.getNodeListByTaskNo(task.getTaskNo());
        assertEquals(1, fromReadFace.size(), "模拟轨迹不能出现在读取面里");
    }

    @Test
    public void testGetTrackDemo_marksItselfAsSimulated() {
        Long taskId = createAssignedTask();

        LogisticsTransportTrackDemoRespVO demo = logisticsTrackDemoService.getTransportTrackDemo(taskId);

        assertEquals("SIMULATED", demo.getSource());
        assertTrue(demo.getEnabled());
        assertNotNull(demo.getNote());
        assertTrue(demo.getNote().contains("模拟"), "响应要自带说明，页面不必另写一份文案");
        assertTrue(demo.getNote().contains("不代表真实行驶路径"));
        assertTrue(demo.getPoints().stream().allMatch(p -> Boolean.TRUE.equals(p.getSimulated())),
                "每个点都要自带模拟标记，别让前端去猜");
    }

    @Test
    public void testGetTrackDemo_anchorsUseRealReportedCoordinates() {
        Long taskId = createAssignedTask();
        reportNode(taskId, "req-3");
        reportNode(taskId, "req-4");

        LogisticsTransportTrackDemoRespVO demo = logisticsTrackDemoService.getTransportTrackDemo(taskId);

        // 两个真实节点都有经纬度 → 两个锚点，且坐标就是上报时的那两个
        assertEquals(2, demo.getAnchors().size());
        assertEquals(new BigDecimal("30.1234567"), demo.getAnchors().get(0).getLatitude());
        assertEquals("起运", demo.getAnchors().get(0).getNodeTypeName());
        // 锚点之间插了若干点
        assertTrue(demo.getPoints().size() > demo.getAnchors().size());
    }

    @Test
    public void testGetTrackDemo_isDeterministic() {
        Long taskId = createAssignedTask();
        reportNode(taskId, "req-5");
        reportNode(taskId, "req-6");

        LogisticsTransportTrackDemoRespVO first = logisticsTrackDemoService.getTransportTrackDemo(taskId);
        LogisticsTransportTrackDemoRespVO second = logisticsTrackDemoService.getTransportTrackDemo(taskId);

        // 同一个任务每次算出来必须是同一条线，否则演示时「每刷新一次换一条路」，看着就像坏了
        assertEquals(first.getPoints().size(), second.getPoints().size());
        for (int i = 0; i < first.getPoints().size(); i++) {
            assertEquals(first.getPoints().get(i).getLatitude(), second.getPoints().get(i).getLatitude());
            assertEquals(first.getPoints().get(i).getLongitude(), second.getPoints().get(i).getLongitude());
        }
    }

    @Test
    public void testGetTrackDemo_disabled_returnsNothing() {
        ReflectionTestUtils.setField(logisticsTrackDemoService, "demoEnabled", false);
        Long taskId = createAssignedTask();
        reportNode(taskId, "req-7");

        LogisticsTransportTrackDemoRespVO demo = logisticsTrackDemoService.getTransportTrackDemo(taskId);

        // 关掉之后页面回到「只有真实节点与凭证」的状态：连点都不给
        assertFalse(demo.getEnabled());
        assertTrue(demo.getPoints().isEmpty());
        assertTrue(demo.getAnchors().isEmpty());
        assertEquals("SIMULATED", demo.getSource(), "关掉也要如实标来源，免得页面误当作真实轨迹");
    }

    @Test
    public void testGetTrackDemo_taskNotExists_isRejected() {
        assertServiceException(() -> logisticsTrackDemoService.getTransportTrackDemo(-1L), TRANSPORT_TASK_NOT_EXISTS);
    }

    // ==================== 辅助 ====================

    /**
     * 建一个**已派车**的任务：没派车就上报「起运」会被状态机拦住（那正是它该做的），
     * 而演示轨迹要锚在真实上报的节点上，所以这里必须先派车。
     */
    private Long createAssignedTask() {
        LogisticsVehicleSaveReqVO vehicle = new LogisticsVehicleSaveReqVO();
        vehicle.setPlateNo("浙A" + System.nanoTime() % 100000);
        vehicle.setStatus(LogisticsVehicleStatusEnum.AVAILABLE.getStatus());
        Long vehicleId = logisticsVehicleService.createVehicle(vehicle);

        LogisticsDriverSaveReqVO driver = new LogisticsDriverSaveReqVO();
        driver.setUserId(System.nanoTime() % 100000 + 1);
        driver.setName("张三");
        driver.setSource(LogisticsDriverSourceEnum.SELF.getSource());
        driver.setStatus(LogisticsDriverStatusEnum.ACTIVE.getStatus());
        Long driverId = logisticsDriverService.createDriver(driver);

        LogisticsTransportTaskCreateReqVO task = new LogisticsTransportTaskCreateReqVO();
        task.setDepartureAddress("城东场站");
        task.setPickupAddress("某某路 1 号");
        task.setVehicleId(vehicleId);
        task.setDriverId(driverId);
        return logisticsTransportTaskService.createTask(task);
    }

    private void reportNode(Long taskId, String clientRequestId) {
        LogisticsTransportNodeReportReqVO reqVO = new LogisticsTransportNodeReportReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setNodeType(LogisticsTransportNodeTypeEnum.DEPARTED.getType());
        reqVO.setNodeTime(LocalDateTime.now().minusMinutes(20));
        reqVO.setLocation("客户门口");
        reqVO.setLatitude(new BigDecimal("30.1234567"));
        reqVO.setLongitude(new BigDecimal("120.1234567"));
        reqVO.setClientRequestId(clientRequestId);
        logisticsTransportNodeService.reportNode(reqVO);
    }

}
