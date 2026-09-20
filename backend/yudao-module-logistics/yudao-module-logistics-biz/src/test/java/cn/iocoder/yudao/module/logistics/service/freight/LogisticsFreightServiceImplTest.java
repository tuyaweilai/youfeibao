package cn.iocoder.yudao.module.logistics.service.freight;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractSurchargeVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightConfirmReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightPayReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightReconciliationReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightReconciliationRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightUpdateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.freight.LogisticsFreightOrderDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporthandover.LogisticsTransportHandoverDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.freight.LogisticsFreightOrderMapper;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporthandover.LogisticsTransportHandoverMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBillingModeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBearerEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsFreightStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierContractService;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierService;
import cn.iocoder.yudao.module.logistics.service.carrier.impl.LogisticsCarrierContractServiceImpl;
import cn.iocoder.yudao.module.logistics.service.carrier.impl.LogisticsCarrierServiceImpl;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.driver.impl.LogisticsDriverServiceImpl;
import cn.iocoder.yudao.module.logistics.service.freight.impl.LogisticsFreightServiceImpl;
import cn.iocoder.yudao.module.logistics.service.transporthandover.LogisticsTransportHandoverService;
import cn.iocoder.yudao.module.logistics.service.transporthandover.impl.LogisticsTransportHandoverServiceImpl;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 承运商运费与对账的单元测试（V8 #75）。
 *
 * <p>钉住 V8 的五条硬约束：
 * <ol>
 *   <li>运价来自**有效期内的承运合同**并被快照；一趟一张；</li>
 *   <li>**自有车不产生承运商运费**（不是承运商的车就建不出运费单）；</li>
 *   <li>差异**不抹平**：实际与应有不同必须留原因，确认后仍看得见；</li>
 *   <li>付款只**登记外部付款凭证**，且必须先确认应付；</li>
 *   <li>运费**不改变现场参考价**——它是另一笔账，不进收购单金额（收购单金额 = 结算重量 × 单价 + 调整项，
 *       单价来自现场参考价；运费碰它就等于改了收购单金额）。</li>
 * </ol>
 */
@Import({LogisticsFreightServiceImpl.class, LogisticsCarrierContractServiceImpl.class,
        LogisticsCarrierServiceImpl.class, LogisticsDriverServiceImpl.class,
        LogisticsVehicleServiceImpl.class, LogisticsTransportTaskServiceImpl.class,
        LogisticsTransportHandoverServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsFreightServiceImplTest extends BaseDbUnitTest {

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
    private LogisticsTransportHandoverService logisticsTransportHandoverService;
    @Resource
    private LogisticsFreightOrderMapper logisticsFreightOrderMapper;
    @Resource
    private LogisticsTransportHandoverMapper logisticsTransportHandoverMapper;

    @Test
    public void testCreateFreight_computesExpectedFromContractIncludingSurchargeBearer() {
        Long carrierId = createCarrier();
        // 按车 300/趟；附加费：本企业承担 100（加进应付）、承运商承担 50（从应付净掉）
        Long contractId = createContract(carrierId, LogisticsFreightBillingModeEnum.BY_TRIP, "300.00",
                surcharge("路桥附加费", "100.00", LogisticsFreightBearerEnum.COMPANY),
                surcharge("回程让利", "50.00", LogisticsFreightBearerEnum.CARRIER));
        Long taskId = createTask(createVehicle(), createCarrierDriver(carrierId));

        Long freightId = logisticsFreightService.createFreight(newCreate(taskId, contractId, "1"));

        LogisticsFreightOrderDO order = logisticsFreightOrderMapper.selectById(freightId);
        assertNotNull(order.getFreightNo());
        assertEquals(carrierId, order.getCarrierId());
        assertEquals(contractId, order.getContractId());
        // 应有应付 = 基础运费 300 + 100 − 50 = 350
        assertEquals(0, new BigDecimal("300.00").compareTo(order.getBaseAmount()));
        assertEquals(0, new BigDecimal("50.00").compareTo(order.getSurchargeAmount()));
        assertEquals(0, new BigDecimal("350.00").compareTo(order.getExpectedAmount()));
        assertEquals(LogisticsFreightStatusEnum.PENDING_CONFIRM.getStatus(), order.getStatus());
        assertNull(order.getActualAmount(), "确认前不该有实际应付");
        assertNull(order.getVarianceAmount());
    }

    @Test
    public void testCreateFreight_selfVehicle_isRejected() {
        Long selfDriver = createSelfDriver();
        Long taskId = createTask(createVehicle(), selfDriver);
        // 自有车没有承运商，合同随便给一份别的承运商的也不行
        Long contractId = createContract(createCarrier(), LogisticsFreightBillingModeEnum.BY_TRIP, "300.00");

        assertServiceException(() -> logisticsFreightService.createFreight(newCreate(taskId, contractId, "1")),
                FREIGHT_CARRIER_REQUIRED);
    }

    @Test
    public void testCreateFreight_taskWithoutDriver_isRejected() {
        LogisticsTransportTaskCreateReqVO pending = new LogisticsTransportTaskCreateReqVO();
        pending.setDepartureAddress("城东场站");
        pending.setPickupAddress("A 家");
        Long taskId = logisticsTransportTaskService.createTask(pending);
        Long contractId = createContract(createCarrier(), LogisticsFreightBillingModeEnum.BY_TRIP, "300.00");

        assertServiceException(() -> logisticsFreightService.createFreight(newCreate(taskId, contractId, "1")),
                FREIGHT_CARRIER_REQUIRED);
    }

    @Test
    public void testCreateFreight_sameTaskTwice_isRejected() {
        Long carrierId = createCarrier();
        Long contractId = createContract(carrierId, LogisticsFreightBillingModeEnum.BY_TRIP, "300.00");
        Long taskId = createTask(createVehicle(), createCarrierDriver(carrierId));
        logisticsFreightService.createFreight(newCreate(taskId, contractId, "1"));

        assertServiceException(() -> logisticsFreightService.createFreight(newCreate(taskId, contractId, "1")),
                FREIGHT_TASK_ALREADY_GATHERED);
    }

    @Test
    public void testCreateFreight_contractOfAnotherCarrier_isRejected() {
        Long carrierA = createCarrier();
        Long carrierB = createCarrier();
        Long contractOfB = createContract(carrierB, LogisticsFreightBillingModeEnum.BY_TRIP, "300.00");
        Long taskId = createTask(createVehicle(), createCarrierDriver(carrierA));

        assertServiceException(() -> logisticsFreightService.createFreight(newCreate(taskId, contractOfB, "1")),
                CARRIER_CONTRACT_CARRIER_MISMATCH);
    }

    @Test
    public void testCreateFreight_disabledContract_isRejected() {
        Long carrierId = createCarrier();
        LogisticsCarrierContractSaveReqVO contract = newContract(carrierId,
                LogisticsFreightBillingModeEnum.BY_TRIP, "300.00");
        contract.setStatus(1);
        Long contractId = logisticsCarrierContractService.createCarrierContract(contract);
        Long taskId = createTask(createVehicle(), createCarrierDriver(carrierId));

        assertServiceException(() -> logisticsFreightService.createFreight(newCreate(taskId, contractId, "1")),
                CARRIER_CONTRACT_NOT_EFFECTIVE);
    }

    @Test
    public void testCreateFreight_nonPositiveBillQuantity_isRejected() {
        Long carrierId = createCarrier();
        Long contractId = createContract(carrierId, LogisticsFreightBillingModeEnum.BY_TRIP, "300.00");
        Long taskId = createTask(createVehicle(), createCarrierDriver(carrierId));

        assertServiceException(() -> logisticsFreightService.createFreight(newCreate(taskId, contractId, "0")),
                FREIGHT_BILL_QUANTITY_INVALID);
    }

    @Test
    public void testUpdateFreight_recomputesExpectedAndRequiresVarianceReason() {
        Long carrierId = createCarrier();
        // 按吨 2600/吨
        Long contractId = createContract(carrierId, LogisticsFreightBillingModeEnum.BY_WEIGHT, "2600.00");
        Long taskId = createTask(createVehicle(), createCarrierDriver(carrierId));
        Long freightId = logisticsFreightService.createFreight(newCreate(taskId, contractId, "10"));

        LogisticsFreightUpdateReqVO update = new LogisticsFreightUpdateReqVO();
        update.setId(freightId);
        update.setBillQuantity(new BigDecimal("12"));
        logisticsFreightService.updateFreight(update);
        // 应有应付随计费量重算：12 × 2600 = 31200
        assertEquals(0, new BigDecimal("31200.00")
                .compareTo(logisticsFreightOrderMapper.selectById(freightId).getExpectedAmount()));

        // 实际与应有有差异却不说原因 → 拒
        LogisticsFreightUpdateReqVO withVariance = new LogisticsFreightUpdateReqVO();
        withVariance.setId(freightId);
        withVariance.setBillQuantity(new BigDecimal("12"));
        withVariance.setActualAmount(new BigDecimal("31500.00"));
        assertServiceException(() -> logisticsFreightService.updateFreight(withVariance),
                FREIGHT_VARIANCE_REASON_REQUIRED);

        // 说了原因 → 差异留在账上，不抹平
        withVariance.setVarianceReason("多跑了 2 公里短驳");
        logisticsFreightService.updateFreight(withVariance);
        LogisticsFreightOrderDO saved = logisticsFreightOrderMapper.selectById(freightId);
        assertEquals(0, new BigDecimal("300.00").compareTo(saved.getVarianceAmount()));
        assertEquals("多跑了 2 公里短驳", saved.getVarianceReason());
    }

    @Test
    public void testConfirmPayable_thenRegisterExternalVoucher() {
        Long carrierId = createCarrier();
        Long contractId = createContract(carrierId, LogisticsFreightBillingModeEnum.BY_TRIP, "300.00");
        Long taskId = createTask(createVehicle(), createCarrierDriver(carrierId));
        Long freightId = logisticsFreightService.createFreight(newCreate(taskId, contractId, "1"));

        // 确认应付：实际 = 应有，无需差异原因
        LogisticsFreightConfirmReqVO confirm = new LogisticsFreightConfirmReqVO();
        confirm.setId(freightId);
        confirm.setActualAmount(new BigDecimal("300.00"));
        logisticsFreightService.confirmPayable(confirm);
        LogisticsFreightOrderDO confirmed = logisticsFreightOrderMapper.selectById(freightId);
        assertEquals(LogisticsFreightStatusEnum.CONFIRMED.getStatus(), confirmed.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(confirmed.getVarianceAmount()), "无差异时差异为 0");

        // 已确认后不能再改计费量
        LogisticsFreightUpdateReqVO update = new LogisticsFreightUpdateReqVO();
        update.setId(freightId);
        update.setBillQuantity(new BigDecimal("2"));
        assertServiceException(() -> logisticsFreightService.updateFreight(update),
                FREIGHT_STATUS_NOT_ALLOW_UPDATE);

        // 登记付款凭证必须有凭证号或附件
        LogisticsFreightPayReqVO payWithoutVoucher = new LogisticsFreightPayReqVO();
        payWithoutVoucher.setId(freightId);
        assertServiceException(() -> logisticsFreightService.registerPaymentVoucher(payWithoutVoucher),
                FREIGHT_VOUCHER_REQUIRED);

        LogisticsFreightPayReqVO pay = new LogisticsFreightPayReqVO();
        pay.setId(freightId);
        pay.setPaymentVoucherNo("PAY20260920001");
        logisticsFreightService.registerPaymentVoucher(pay);
        LogisticsFreightOrderDO paid = logisticsFreightOrderMapper.selectById(freightId);
        assertEquals(LogisticsFreightStatusEnum.VOUCHER_REGISTERED.getStatus(), paid.getStatus());
        assertEquals(0, new BigDecimal("300.00").compareTo(paid.getPaymentAmount()),
                "未填实付时按确认的实际应付记");
        assertNotNull(paid.getPaidAt());

        // 已登记凭证后不能再登记
        assertServiceException(() -> logisticsFreightService.registerPaymentVoucher(pay),
                FREIGHT_STATUS_NOT_ALLOW_PAY);
    }

    @Test
    public void testConfirmPayable_varianceRequiresReason() {
        Long carrierId = createCarrier();
        Long contractId = createContract(carrierId, LogisticsFreightBillingModeEnum.BY_TRIP, "300.00");
        Long taskId = createTask(createVehicle(), createCarrierDriver(carrierId));
        Long freightId = logisticsFreightService.createFreight(newCreate(taskId, contractId, "1"));

        LogisticsFreightConfirmReqVO confirm = new LogisticsFreightConfirmReqVO();
        confirm.setId(freightId);
        confirm.setActualAmount(new BigDecimal("320.00"));
        assertServiceException(() -> logisticsFreightService.confirmPayable(confirm),
                FREIGHT_VARIANCE_REASON_REQUIRED);

        confirm.setVarianceReason("临时加价 20");
        logisticsFreightService.confirmPayable(confirm);
        LogisticsFreightOrderDO saved = logisticsFreightOrderMapper.selectById(freightId);
        assertEquals(0, new BigDecimal("20.00").compareTo(saved.getVarianceAmount()));
        assertEquals("临时加价 20", saved.getVarianceReason());
    }

    @Test
    public void testReconciliation_groupsByCarrierAndContract() {
        Long carrierA = createCarrier();
        Long carrierB = createCarrier();
        Long contractA = createContract(carrierA, LogisticsFreightBillingModeEnum.BY_TRIP, "300.00");
        Long contractB = createContract(carrierB, LogisticsFreightBillingModeEnum.BY_TRIP, "500.00");

        // A：两趟，一趟确认（实际 320）一趟待确认
        Long freightA1 = logisticsFreightService.createFreight(
                newCreate(createTask(createVehicle(), createCarrierDriver(carrierA)), contractA, "1"));
        logisticsFreightService.createFreight(
                newCreate(createTask(createVehicle(), createCarrierDriver(carrierA)), contractA, "1"));
        LogisticsFreightConfirmReqVO confirm = new LogisticsFreightConfirmReqVO();
        confirm.setId(freightA1);
        confirm.setActualAmount(new BigDecimal("320.00"));
        confirm.setVarianceReason("加价");
        logisticsFreightService.confirmPayable(confirm);
        // B：一趟，已登记付款凭证
        Long freightB1 = logisticsFreightService.createFreight(
                newCreate(createTask(createVehicle(), createCarrierDriver(carrierB)), contractB, "1"));
        LogisticsFreightConfirmReqVO confirmB = new LogisticsFreightConfirmReqVO();
        confirmB.setId(freightB1);
        confirmB.setActualAmount(new BigDecimal("500.00"));
        logisticsFreightService.confirmPayable(confirmB);
        LogisticsFreightPayReqVO pay = new LogisticsFreightPayReqVO();
        pay.setId(freightB1);
        pay.setPaymentVoucherUrl("https://file/pay-b.jpg");
        logisticsFreightService.registerPaymentVoucher(pay);

        List<LogisticsFreightReconciliationRespVO> rows =
                logisticsFreightService.getReconciliation(new LogisticsFreightReconciliationReqVO());

        assertEquals(2, rows.size(), "按承运商 + 合同各一行");
        LogisticsFreightReconciliationRespVO rowA = rows.stream()
                .filter(row -> carrierA.equals(row.getCarrierId())).findFirst().orElseThrow();
        assertEquals(2, rowA.getTripCount());
        // 应有 600，实际 = 320 + 300（未对账按应有）= 620，差异 +20
        assertEquals(0, new BigDecimal("600.00").compareTo(rowA.getExpectedTotal()));
        assertEquals(0, new BigDecimal("620.00").compareTo(rowA.getActualTotal()));
        assertEquals(0, new BigDecimal("20.00").compareTo(rowA.getVarianceTotal()));
        assertEquals(1, rowA.getPendingConfirmCount());
        assertEquals(0, rowA.getVoucherRegisteredCount());

        LogisticsFreightReconciliationRespVO rowB = rows.stream()
                .filter(row -> carrierB.equals(row.getCarrierId())).findFirst().orElseThrow();
        assertEquals(1, rowB.getTripCount());
        assertEquals(1, rowB.getVoucherRegisteredCount());
    }

    @Test
    public void testFreight_doesNotChangeHandoverReferencePrice() {
        Long carrierId = createCarrier();
        Long contractId = createContract(carrierId, LogisticsFreightBillingModeEnum.BY_TRIP, "300.00");
        Long taskId = createTask(createVehicle(), createCarrierDriver(carrierId));
        Long handoverId = createHandover(taskId);

        LogisticsFreightOrderDO before =
                logisticsFreightOrderMapper.selectById(
                        logisticsFreightService.createFreight(newCreate(taskId, contractId, "1")));
        LogisticsFreightConfirmReqVO confirm = new LogisticsFreightConfirmReqVO();
        confirm.setId(before.getId());
        confirm.setActualAmount(new BigDecimal("999.00"));
        confirm.setVarianceReason("对账差异");
        logisticsFreightService.confirmPayable(confirm);
        LogisticsFreightPayReqVO pay = new LogisticsFreightPayReqVO();
        pay.setId(before.getId());
        pay.setPaymentVoucherNo("PAY-1");
        logisticsFreightService.registerPaymentVoucher(pay);

        // 现场参考价是收购单单价的来源，运费**一个字都不能改它**
        LogisticsTransportHandoverDO handover = logisticsTransportHandoverMapper.selectById(handoverId);
        assertEquals(0, new BigDecimal("2600.00").compareTo(handover.getReferenceUnitPrice()));
        assertEquals(0, new BigDecimal("12.5").compareTo(handover.getReferenceQuantity()));
    }

    // ==================== 辅助 ====================

    private static LogisticsFreightCreateReqVO newCreate(Long taskId, Long contractId, String billQuantity) {
        LogisticsFreightCreateReqVO reqVO = new LogisticsFreightCreateReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setContractId(contractId);
        reqVO.setBillQuantity(new BigDecimal(billQuantity));
        return reqVO;
    }

    private Long createCarrier() {
        LogisticsCarrierSaveReqVO carrier = new LogisticsCarrierSaveReqVO();
        carrier.setName("承运商" + System.nanoTime());
        carrier.setStatus(0);
        return logisticsCarrierService.createCarrier(carrier);
    }

    private Long createContract(Long carrierId, LogisticsFreightBillingModeEnum mode, String unitPrice,
                                LogisticsCarrierContractSurchargeVO... surcharges) {
        LogisticsCarrierContractSaveReqVO contract = newContract(carrierId, mode, unitPrice);
        contract.setSurcharges(surcharges.length == 0 ? null : Arrays.asList(surcharges));
        return logisticsCarrierContractService.createCarrierContract(contract);
    }

    private static LogisticsCarrierContractSaveReqVO newContract(Long carrierId,
                                                                 LogisticsFreightBillingModeEnum mode,
                                                                 String unitPrice) {
        LogisticsCarrierContractSaveReqVO contract = new LogisticsCarrierContractSaveReqVO();
        contract.setCarrierId(carrierId);
        contract.setEffectiveFrom(LocalDate.now().minusDays(1));
        contract.setEffectiveTo(LocalDate.now().plusDays(30));
        contract.setRoute("城东场站—临平");
        contract.setBillingMode(mode.getMode());
        contract.setUnitPrice(new BigDecimal(unitPrice));
        contract.setStatus(0);
        return contract;
    }

    private static LogisticsCarrierContractSurchargeVO surcharge(String name, String amount,
                                                                 LogisticsFreightBearerEnum bearer) {
        LogisticsCarrierContractSurchargeVO vo = new LogisticsCarrierContractSurchargeVO();
        vo.setName(name);
        vo.setAmount(new BigDecimal(amount));
        vo.setBearer(bearer.getBearer());
        return vo;
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

    private Long createTask(Long vehicleId, Long driverId) {
        LogisticsTransportTaskCreateReqVO task = new LogisticsTransportTaskCreateReqVO();
        task.setDepartureAddress("城东场站");
        task.setPickupAddress("A 家");
        task.setVehicleId(vehicleId);
        task.setDriverId(driverId);
        return logisticsTransportTaskService.createTask(task);
    }

    private Long createHandover(Long taskId) {
        LogisticsTransportHandoverCreateReqVO reqVO = new LogisticsTransportHandoverCreateReqVO();
        reqVO.setTaskId(taskId);
        reqVO.setPayeeId(1024L);
        reqVO.setPayeeName("甲家");
        reqVO.setPayeeMobile("13800138000");
        reqVO.setGoodsConfigId(2048L);
        reqVO.setCategoryName("废钢铁");
        reqVO.setUnit("吨");
        reqVO.setReferenceQuantity(new BigDecimal("12.5"));
        reqVO.setReferenceUnitPrice(new BigDecimal("2600.00"));
        reqVO.setPhotos(List.of("https://file/handover.jpg"));
        reqVO.setOccurTime(LocalDateTime.now());
        return logisticsTransportHandoverService.createHandover(reqVO);
    }

}
