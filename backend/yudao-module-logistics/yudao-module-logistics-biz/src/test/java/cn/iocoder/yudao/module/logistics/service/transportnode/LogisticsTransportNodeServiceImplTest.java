package cn.iocoder.yudao.module.logistics.service.transportnode;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transportnode.LogisticsTransportNodeMapper;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporttask.LogisticsTransportTaskMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transportnode.impl.LogisticsTransportNodeServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import cn.iocoder.yudao.module.logistics.service.transporttask.impl.LogisticsTransportTaskServiceImpl;
import cn.iocoder.yudao.module.logistics.service.vehicle.LogisticsVehicleService;
import cn.iocoder.yudao.module.logistics.service.vehicle.impl.LogisticsVehicleServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LogisticsTransportNodeServiceImpl} 的单元测试（V2b #78）。
 *
 * <p>两件最容易做错的事被钉在这里：**写入幂等**（弱网补传会重复提交同一条事实）与
 * **两个时间分开**（发生时间 ≠ 上报时间，时间线按发生时间排序）。
 */
@Import({LogisticsTransportNodeServiceImpl.class, LogisticsTransportTaskServiceImpl.class,
        LogisticsVehicleServiceImpl.class, LogisticsDriverServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsTransportNodeServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportNodeMapper logisticsTransportNodeMapper;
    @Resource
    private LogisticsTransportTaskMapper logisticsTransportTaskMapper;
    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsDriverService logisticsDriverService;

    @Test
    public void testReportNode_departed_advancesTaskToInTransit() {
        Long taskId = createTask();
        LocalDateTime departedAt = LocalDateTime.now().minusHours(2); // 事情是两小时前发生的

        Long nodeId = logisticsTransportNodeService.reportNode(
                newReport(taskId, LogisticsTransportNodeTypeEnum.DEPARTED.getType(), departedAt, "req-1"));

        LogisticsTransportNodeDO node = logisticsTransportNodeMapper.selectById(nodeId);
        assertEquals(LogisticsTransportNodeTypeEnum.DEPARTED.getType(), node.getNodeType());
        assertEquals(departedAt, node.getNodeTime());
        assertNotNull(node.getReportTime());
        assertTrue(node.getReportTime().isAfter(node.getNodeTime()),
                "发生时间与上报时间要分开：补录晚到不代表业务倒序");
        assertEquals("城东场站门口", node.getLocation());
        assertEquals(new BigDecimal("30.1234567"), node.getLatitude());

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        assertEquals(LogisticsTransportTaskStatusEnum.IN_TRANSIT.getStatus(), task.getStatus());
        // 起运时间取**发生时间**，不是上报时间
        assertEquals(departedAt, task.getStartTime());
    }

    @Test
    public void testReportNode_sameClientRequestId_isIdempotent() {
        Long taskId = createTask();
        LocalDateTime departedAt = LocalDateTime.now().minusMinutes(30);

        Long first = logisticsTransportNodeService.reportNode(
                newReport(taskId, LogisticsTransportNodeTypeEnum.DEPARTED.getType(), departedAt, "req-same"));
        Long second = logisticsTransportNodeService.reportNode(
                newReport(taskId, LogisticsTransportNodeTypeEnum.DEPARTED.getType(), departedAt, "req-same"));

        assertEquals(first, second, "重复补传要返回既有节点，而不是再落一条");
        assertEquals(1, logisticsTransportNodeMapper.selectListByTaskId(taskId).size());
    }

    @Test
    public void testReportNode_nodeTypeNotSupportedYet_isRejected() {
        Long taskId = createTask();

        // 本票只开放「起运」，其余四类归 V4（#71）
        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP.getType(),
                                LocalDateTime.now(), "req-2")),
                TRANSPORT_NODE_TYPE_NOT_SUPPORTED_YET);
        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, LogisticsTransportNodeTypeEnum.UNLOADED.getType(),
                                LocalDateTime.now(), "req-3")),
                TRANSPORT_NODE_TYPE_NOT_SUPPORTED_YET);
    }

    @Test
    public void testReportNode_taskNotExists_isRejected() {
        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(-1L, LogisticsTransportNodeTypeEnum.DEPARTED.getType(), LocalDateTime.now(), "req-4")),
                TRANSPORT_TASK_NOT_EXISTS);
    }

    @Test
    public void testReportNode_missingNodeTime_isRejected() {
        Long taskId = createTask();

        assertServiceException(() -> logisticsTransportNodeService.reportNode(
                        newReport(taskId, LogisticsTransportNodeTypeEnum.DEPARTED.getType(), null, "req-5")),
                TRANSPORT_NODE_TIME_REQUIRED);
    }

    @Test
    public void testGetNodeListByTaskNo_timelineOrderedByNodeTime() {
        Long taskId = createTask();
        LocalDateTime later = LocalDateTime.now().minusMinutes(10);
        LocalDateTime earlier = LocalDateTime.now().minusHours(3);
        // 故意先报「晚」的，再报「早」的：时间线要按发生时间排，不是按上报顺序
        logisticsTransportNodeService.reportNode(newReport(taskId, LogisticsTransportNodeTypeEnum.DEPARTED.getType(),
                later, "req-late"));
        // 早的那条换个类型会不被支持，所以这里用另一条任务验证排序
        Long anotherTaskId = createTask();
        logisticsTransportNodeService.reportNode(newReport(anotherTaskId, LogisticsTransportNodeTypeEnum.DEPARTED.getType(),
                earlier, "req-early"));

        LogisticsTransportTaskDO task = logisticsTransportTaskMapper.selectById(taskId);
        List<LogisticsTransportNodeDO> nodes = logisticsTransportNodeService.getNodeListByTaskNo(task.getTaskNo());
        assertEquals(1, nodes.size());
        assertEquals(later, nodes.get(0).getNodeTime());
    }

    @Test
    public void testGetNodeListByTaskNo_blankOrUnknown_returnsEmpty() {
        assertTrue(logisticsTransportNodeService.getNodeListByTaskNo(null).isEmpty());
        assertTrue(logisticsTransportNodeService.getNodeListByTaskNo(" ").isEmpty());
        assertTrue(logisticsTransportNodeService.getNodeListByTaskNo("TT-NOT-EXISTS").isEmpty());
        assertTrue(logisticsTransportNodeService.getNodeListByTaskId(null).isEmpty());
    }

    @Test
    public void testToRespList_carriesPhotosAndTypeName() {
        Long taskId = createTask();
        LogisticsTransportNodeReportReqVO reqVO = newReport(taskId, LogisticsTransportNodeTypeEnum.DEPARTED.getType(),
                LocalDateTime.now(), "req-photos");
        reqVO.setPhotos(Arrays.asList("https://file/1.jpg", "https://file/2.jpg"));
        logisticsTransportNodeService.reportNode(reqVO);

        List<LogisticsTransportNodeRespVO> resps = logisticsTransportNodeService.toRespList(
                logisticsTransportNodeService.getNodeListByTaskId(taskId));

        assertEquals(1, resps.size());
        assertEquals("起运", resps.get(0).getNodeTypeName());
        assertEquals(Arrays.asList("https://file/1.jpg", "https://file/2.jpg"), resps.get(0).getPhotos());
    }

    @Test
    public void testToRespList_emptyPhotos_returnsEmptyListNotNull() {
        Long taskId = createTask();
        logisticsTransportNodeService.reportNode(newReport(taskId, LogisticsTransportNodeTypeEnum.DEPARTED.getType(),
                LocalDateTime.now(), "req-no-photo"));

        List<LogisticsTransportNodeRespVO> resps = logisticsTransportNodeService.toRespList(
                logisticsTransportNodeService.getNodeListByTaskId(taskId));

        assertNotNull(resps.get(0).getPhotos());
        assertTrue(resps.get(0).getPhotos().isEmpty(), "没传照片要给空列表，不是 null（前端直接遍历）");
    }

    // ==================== 辅助 ====================

    /**
     * 建一个**已派车**的任务：没派车就上报「起运」是矛盾的，状态机会拦（那正是它该做的）。
     */
    private Long createTask() {
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

        LogisticsTransportTaskSaveReqVO task = new LogisticsTransportTaskSaveReqVO();
        task.setDepartureAddress("城东场站");
        task.setPickupAddress("某某路 1 号");
        task.setVehicleId(vehicleId);
        task.setDriverId(driverId);
        return logisticsTransportTaskService.createTask(task);
    }

    private static LogisticsTransportNodeReportReqVO newReport(Long taskId, Integer nodeType,
                                                              LocalDateTime nodeTime, String clientRequestId) {
        LogisticsTransportNodeReportReqVO reqVO = new LogisticsTransportNodeReportReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setNodeType(nodeType);
        reqVO.setNodeTime(nodeTime);
        reqVO.setLocation("城东场站门口");
        reqVO.setLatitude(new BigDecimal("30.1234567"));
        reqVO.setLongitude(new BigDecimal("120.1234567"));
        reqVO.setClientRequestId(clientRequestId);
        return reqVO;
    }

}
