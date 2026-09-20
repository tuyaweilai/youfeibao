package cn.iocoder.yudao.module.logistics.api.transport;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportNodeRespDTO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportTaskMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
import cn.iocoder.yudao.module.logistics.service.transportnode.impl.LogisticsTransportNodeServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import cn.iocoder.yudao.module.logistics.service.transporttask.impl.LogisticsTransportTaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LogisticsTransportApiImpl} 的单元测试，同时是物流模块**读取面契约**的守卫。
 *
 * <p>锁死的是契约，而不是实现：查不到返回**空列表**（不是 null、不抛异常）、按发生时间正序、
 * 凭证里带车牌与司机的**快照**。icbc 的追溯页直接消费这个契约——缺凭证是业务状态
 *（自送的货本来就可能没有运输节点），不是错误。
 */
@Import({LogisticsTransportApiImpl.class, LogisticsTransportTaskServiceImpl.class,
        LogisticsTransportNodeServiceImpl.class,
        cn.iocoder.yudao.module.logistics.service.vehicle.impl.LogisticsVehicleServiceImpl.class,
        cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl.class,
        UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsTransportApiImplTest extends BaseDbUnitTest {

    @Resource
    private LogisticsTransportApi logisticsTransportApi;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;
    @Resource
    private LogisticsTransportTaskMapper logisticsTransportTaskMapper;
    @Resource
    private cn.iocoder.yudao.module.logistics.service.vehicle.LogisticsVehicleService logisticsVehicleService;
    @Resource
    private cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService logisticsDriverService;

    @Test
    public void testGetNodeListByTaskNo_unknown_returnsEmptyListNotNull() {
        List<LogisticsTransportNodeRespDTO> nodes = logisticsTransportApi.getNodeListByTaskNo("NOT-EXISTS-TASK-NO");

        assertNotNull(nodes, "契约要求返回空列表而不是 null");
        assertTrue(nodes.isEmpty());
    }

    @Test
    public void testGetEvidenceListByHandoverBatchId_unknown_returnsEmptyListNotNull() {
        List<LogisticsTransportNodeRespDTO> evidences = logisticsTransportApi.getEvidenceListByHandoverBatchId(-1L);

        assertNotNull(evidences, "契约要求返回空列表而不是 null");
        assertTrue(evidences.isEmpty());
    }

    @Test
    public void testGetNodeListByTaskNo_nullTaskNo_doesNotThrow() {
        // 自送或没有派车的场景，调用方可能什么都没有；契约是「没有就是空」，不是报错
        List<LogisticsTransportNodeRespDTO> nodes = logisticsTransportApi.getNodeListByTaskNo(null);

        assertNotNull(nodes);
        assertTrue(nodes.isEmpty());
    }

    @Test
    public void testGetNodeListByTaskNo_returnsRealNodesWithSnapshots() {
        // 建一个带车与司机的任务，再上报一个节点
        Long vehicleId = createVehicle();
        Long driverId = createDriver();
        LogisticsTransportTaskSaveReqVO taskReqVO = new LogisticsTransportTaskSaveReqVO();
        taskReqVO.setDepartureAddress("城东场站");
        taskReqVO.setPickupAddress("某某路 1 号");
        taskReqVO.setVehicleId(vehicleId);
        taskReqVO.setDriverId(driverId);
        Long taskId = logisticsTransportTaskService.createTask(taskReqVO);
        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);

        LogisticsTransportNodeReportReqVO reportReqVO = new LogisticsTransportNodeReportReqVO();
        reportReqVO.setTaskId(taskId);
        reportReqVO.setNodeType(LogisticsTransportNodeTypeEnum.DEPARTED.getType());
        reportReqVO.setNodeTime(LocalDateTime.now().minusMinutes(15));
        reportReqVO.setLocation("城东场站门口");
        reportReqVO.setPhotos(Arrays.asList("https://file/1.jpg"));
        reportReqVO.setClientRequestId("req-readface-1");
        logisticsTransportNodeService.reportNode(reportReqVO);

        List<LogisticsTransportNodeRespDTO> nodes = logisticsTransportApi.getNodeListByTaskNo(task.getTaskNo());

        assertEquals(1, nodes.size());
        LogisticsTransportNodeRespDTO dto = nodes.get(0);
        assertEquals(task.getTaskNo(), dto.getTaskNo());
        assertEquals(LogisticsTransportNodeTypeEnum.DEPARTED.getType(), dto.getNodeType());
        assertNotNull(dto.getNodeTime());
        assertNotNull(dto.getReportTime(), "两个时间都要给追溯页");
        assertEquals("城东场站门口", dto.getLocation());
        assertEquals(Arrays.asList("https://file/1.jpg"), dto.getPhotos());
    }


    private Long createVehicle() {
        cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO vehicle =
                new cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO();
        vehicle.setPlateNo("浙A" + System.nanoTime() % 100000);
        vehicle.setStatus(cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum.AVAILABLE.getStatus());
        return logisticsVehicleService.createVehicle(vehicle);
    }

    private Long createDriver() {
        cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO driver =
                new cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO();
        driver.setUserId(System.nanoTime() % 100000 + 1);
        driver.setName("张三");
        driver.setSource(cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum.SELF.getSource());
        driver.setStatus(cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum.ACTIVE.getStatus());
        return logisticsDriverService.createDriver(driver);
    }

}
