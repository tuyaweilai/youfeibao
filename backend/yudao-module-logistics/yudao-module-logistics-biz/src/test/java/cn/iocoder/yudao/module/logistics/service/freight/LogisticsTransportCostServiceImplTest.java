package cn.iocoder.yudao.module.logistics.service.freight;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.freight.LogisticsTransportCostDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.freight.LogisticsFreightOrderMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBearerEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportCostTypeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierContractService;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierService;
import cn.iocoder.yudao.module.logistics.service.carrier.impl.LogisticsCarrierContractServiceImpl;
import cn.iocoder.yudao.module.logistics.service.carrier.impl.LogisticsCarrierServiceImpl;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl;
import cn.iocoder.yudao.module.logistics.service.freight.impl.LogisticsFreightServiceImpl;
import cn.iocoder.yudao.module.logistics.service.freight.impl.LogisticsTransportCostServiceImpl;
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
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 运输费用（内部成本）的单元测试（V8 #75）。
 *
 * <p>钉住「自有车的路桥 / 燃油按**实际承担方**记，且这些钱**不动承运商运费**」。
 */
@Import({LogisticsTransportCostServiceImpl.class, LogisticsFreightServiceImpl.class,
        LogisticsCarrierContractServiceImpl.class, LogisticsCarrierServiceImpl.class,
        LogisticsDriverServiceImpl.class, LogisticsVehicleServiceImpl.class,
        LogisticsTransportTaskServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsTransportCostServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LogisticsTransportCostService logisticsTransportCostService;
    @Resource
    private LogisticsFreightService logisticsFreightService;
    @Resource
    private LogisticsCarrierContractService logisticsCarrierContractService;
    @Resource
    private LogisticsCarrierService logisticsCarrierService;
    @Resource
    private LogisticsDriverService logisticsDriverService;
    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsFreightOrderMapper logisticsFreightOrderMapper;

    @Test
    public void testCreateTransportCost_recordsInternalCostWithActualBearer() {
        Long taskId = createTask(createSelfDriver());

        Long costId = logisticsTransportCostService.createTransportCost(
                newCost(taskId, LogisticsTransportCostTypeEnum.TOLL, "80.00", LogisticsFreightBearerEnum.COMPANY));

        LogisticsTransportCostDO cost = logisticsTransportCostService.getTransportCost(costId);
        assertNotNull(cost.getTaskNo());
        assertEquals(LogisticsTransportCostTypeEnum.TOLL.getType(), cost.getCostType());
        assertEquals(0, new BigDecimal("80.00").compareTo(cost.getAmount()));
        assertEquals(LogisticsFreightBearerEnum.COMPANY.getBearer(), cost.getBearer());
        LogisticsTransportCostRespVO resp = logisticsTransportCostService.toResp(cost);
        assertEquals("路桥费", resp.getCostTypeName());
        assertEquals("本企业承担", resp.getBearerName());
    }

    @Test
    public void testTransportCost_rejectsNegativeAmountAndUnknownEnums() {
        Long taskId = createTask(createSelfDriver());

        LogisticsTransportCostSaveReqVO negative = newCost(taskId, LogisticsTransportCostTypeEnum.FUEL,
                "-1", LogisticsFreightBearerEnum.COMPANY);
        assertServiceException(() -> logisticsTransportCostService.createTransportCost(negative),
                TRANSPORT_COST_AMOUNT_INVALID);

        LogisticsTransportCostSaveReqVO unknownType = newCost(taskId, LogisticsTransportCostTypeEnum.TOLL,
                "10", LogisticsFreightBearerEnum.COMPANY);
        unknownType.setCostType(99);
        assertServiceException(() -> logisticsTransportCostService.createTransportCost(unknownType),
                TRANSPORT_COST_TYPE_UNKNOWN);

        LogisticsTransportCostSaveReqVO unknownBearer = newCost(taskId, LogisticsTransportCostTypeEnum.TOLL,
                "10", LogisticsFreightBearerEnum.COMPANY);
        unknownBearer.setBearer(99);
        assertServiceException(() -> logisticsTransportCostService.createTransportCost(unknownBearer),
                TRANSPORT_COST_BEARER_UNKNOWN);
    }

    @Test
    public void testTransportCost_doesNotChangeCarrierFreightExpectedAmount() {
        Long carrierId = createCarrier();
        Long contractId = createCarrierContract(carrierId);
        Long taskId = createTask(createCarrierDriver(carrierId));
        Long freightId = logisticsFreightService.createFreight(newFreight(taskId, contractId, "1"));
        BigDecimal expectedBefore = logisticsFreightOrderMapper.selectById(freightId).getExpectedAmount();

        // 这一趟还发生了本企业的燃油费；它记在成本账上，不动承运商运费
        logisticsTransportCostService.createTransportCost(
                newCost(taskId, LogisticsTransportCostTypeEnum.FUEL, "260.00", LogisticsFreightBearerEnum.COMPANY));

        BigDecimal expectedAfter = logisticsFreightOrderMapper.selectById(freightId).getExpectedAmount();
        assertEquals(0, expectedBefore.compareTo(expectedAfter), "内部成本不改变承运商运费");
        assertEquals(1, logisticsTransportCostService.getTransportCostListByTaskId(taskId).size());
    }

    // ==================== 辅助 ====================

    private static LogisticsTransportCostSaveReqVO newCost(Long taskId, LogisticsTransportCostTypeEnum type,
                                                           String amount, LogisticsFreightBearerEnum bearer) {
        LogisticsTransportCostSaveReqVO reqVO = new LogisticsTransportCostSaveReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setCostType(type.getType());
        reqVO.setName(type.getName());
        reqVO.setAmount(new BigDecimal(amount));
        reqVO.setBearer(bearer.getBearer());
        reqVO.setOccurDate(LocalDate.now());
        return reqVO;
    }

    private Long createCarrier() {
        LogisticsCarrierSaveReqVO carrier = new LogisticsCarrierSaveReqVO();
        carrier.setName("承运商" + System.nanoTime());
        carrier.setStatus(0);
        return logisticsCarrierService.createCarrier(carrier);
    }

    private Long createCarrierContract(Long carrierId) {
        LogisticsCarrierContractSaveReqVO contract = new LogisticsCarrierContractSaveReqVO();
        contract.setCarrierId(carrierId);
        contract.setEffectiveFrom(LocalDate.now().minusDays(1));
        contract.setEffectiveTo(LocalDate.now().plusDays(30));
        contract.setRoute("城东场站—临平");
        contract.setBillingMode(1);
        contract.setUnitPrice(new BigDecimal("300.00"));
        contract.setStatus(0);
        return logisticsCarrierContractService.createCarrierContract(contract);
    }

    private static LogisticsFreightCreateReqVO newFreight(Long taskId, Long contractId, String billQuantity) {
        LogisticsFreightCreateReqVO reqVO = new LogisticsFreightCreateReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setContractId(contractId);
        reqVO.setBillQuantity(new BigDecimal(billQuantity));
        return reqVO;
    }

    private Long createVehicle() {
        LogisticsVehicleSaveReqVO vehicle = new LogisticsVehicleSaveReqVO();
        vehicle.setPlateNo("浙A" + System.nanoTime() % 100000);
        vehicle.setStatus(LogisticsVehicleStatusEnum.AVAILABLE.getStatus());
        return logisticsVehicleService.createVehicle(vehicle);
    }

    private Long createCarrierDriver(Long carrierId) {
        LogisticsDriverSaveReqVO driver = new LogisticsDriverSaveReqVO();
        driver.setUserId(System.nanoTime() % 100000 + 1);
        driver.setName("承运司机");
        driver.setMobile("13800138000");
        driver.setSource(LogisticsDriverSourceEnum.CARRIER.getSource());
        driver.setCarrierId(carrierId);
        driver.setStatus(LogisticsDriverStatusEnum.ACTIVE.getStatus());
        return logisticsDriverService.createDriver(driver);
    }

    private Long createSelfDriver() {
        LogisticsDriverSaveReqVO driver = new LogisticsDriverSaveReqVO();
        driver.setUserId(System.nanoTime() % 100000 + 1);
        driver.setName("自有司机");
        driver.setMobile("13800138001");
        driver.setSource(LogisticsDriverSourceEnum.SELF.getSource());
        driver.setStatus(LogisticsDriverStatusEnum.ACTIVE.getStatus());
        return logisticsDriverService.createDriver(driver);
    }

    private Long createTask(Long driverId) {
        LogisticsTransportTaskCreateReqVO task = new LogisticsTransportTaskCreateReqVO();
        task.setDepartureAddress("城东场站");
        task.setPickupAddress("A 家");
        task.setVehicleId(createVehicle());
        task.setDriverId(driverId);
        return logisticsTransportTaskService.createTask(task);
    }

}
