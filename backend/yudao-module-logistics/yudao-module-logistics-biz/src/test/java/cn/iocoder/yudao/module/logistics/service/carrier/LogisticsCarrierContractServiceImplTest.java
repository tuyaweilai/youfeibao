package cn.iocoder.yudao.module.logistics.service.carrier;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.logistics.UnitTestConfiguration;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractSurchargeVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierContractDO;
import cn.iocoder.yudao.module.logistics.service.carrier.impl.LogisticsCarrierContractServiceImpl;
import cn.iocoder.yudao.module.logistics.service.carrier.impl.LogisticsCarrierServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 承运合同的单元测试（V8 #75）。
 *
 * <p>钉住「一份不限定范围、没写运价、或早已停用的合同，不能拿来汇集运费」。
 */
@Import({LogisticsCarrierContractServiceImpl.class, LogisticsCarrierServiceImpl.class,
        UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class LogisticsCarrierContractServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LogisticsCarrierContractService logisticsCarrierContractService;
    @Resource
    private LogisticsCarrierService logisticsCarrierService;

    @Test
    public void testCreateContract_requiresScopePriceAndValidSurcharges() {
        Long carrierId = createCarrier(0);

        LogisticsCarrierContractSaveReqVO noScope = newContract(carrierId, "300.00");
        noScope.setRoute(null);
        assertServiceException(() -> logisticsCarrierContractService.createCarrierContract(noScope),
                CARRIER_CONTRACT_SCOPE_REQUIRED);

        LogisticsCarrierContractSaveReqVO badRange = newContract(carrierId, "300.00");
        badRange.setEffectiveFrom(LocalDate.now().plusDays(10));
        badRange.setEffectiveTo(LocalDate.now());
        assertServiceException(() -> logisticsCarrierContractService.createCarrierContract(badRange),
                CARRIER_CONTRACT_EFFECTIVE_RANGE_INVALID);

        LogisticsCarrierContractSaveReqVO negativePrice = newContract(carrierId, "-1");
        assertServiceException(() -> logisticsCarrierContractService.createCarrierContract(negativePrice),
                CARRIER_CONTRACT_PRICE_INVALID);

        LogisticsCarrierContractSaveReqVO badSurcharge = newContract(carrierId, "300.00");
        LogisticsCarrierContractSurchargeVO surcharge = new LogisticsCarrierContractSurchargeVO();
        surcharge.setName("路桥附加费");
        surcharge.setAmount(new BigDecimal("100.00"));
        surcharge.setBearer(null); // 没写承担方
        badSurcharge.setSurcharges(java.util.List.of(surcharge));
        assertServiceException(() -> logisticsCarrierContractService.createCarrierContract(badSurcharge),
                CARRIER_CONTRACT_SURCHARGE_INVALID);

        // 承运商停用时不能建合同
        Long disabledCarrier = createCarrier(1);
        assertServiceException(() -> logisticsCarrierContractService.createCarrierContract(
                newContract(disabledCarrier, "300.00")), CARRIER_NOT_ACTIVE);
    }

    @Test
    public void testCreateContract_duplicateContractNo_isRejected() {
        Long carrierId = createCarrier(0);
        LogisticsCarrierContractSaveReqVO first = newContract(carrierId, "300.00");
        first.setContractNo("CC-FIXED-1");
        logisticsCarrierContractService.createCarrierContract(first);

        LogisticsCarrierContractSaveReqVO second = newContract(carrierId, "300.00");
        second.setContractNo("CC-FIXED-1");
        assertServiceException(() -> logisticsCarrierContractService.createCarrierContract(second),
                CARRIER_CONTRACT_NO_DUPLICATE);
    }

    @Test
    public void testGetEffectiveContract_rejectsDisabledAndOutOfRange() {
        Long carrierId = createCarrier(0);

        LogisticsCarrierContractSaveReqVO disabled = newContract(carrierId, "300.00");
        disabled.setStatus(1);
        Long disabledId = logisticsCarrierContractService.createCarrierContract(disabled);
        assertServiceException(() -> logisticsCarrierContractService.getEffectiveContract(disabledId),
                CARRIER_CONTRACT_NOT_EFFECTIVE);

        // 尚未生效
        LogisticsCarrierContractSaveReqVO future = newContract(carrierId, "300.00");
        future.setEffectiveFrom(LocalDate.now().plusDays(1));
        future.setEffectiveTo(LocalDate.now().plusDays(10));
        Long futureId = logisticsCarrierContractService.createCarrierContract(future);
        assertServiceException(() -> logisticsCarrierContractService.getEffectiveContract(futureId),
                CARRIER_CONTRACT_NOT_EFFECTIVE);

        // 生效中
        Long activeId = logisticsCarrierContractService.createCarrierContract(newContract(carrierId, "300.00"));
        LogisticsCarrierContractDO active = logisticsCarrierContractService.getEffectiveContract(activeId);
        assertNotNull(active.getContractNo());
    }

    // ==================== 辅助 ====================

    private Long createCarrier(int status) {
        LogisticsCarrierSaveReqVO carrier = new LogisticsCarrierSaveReqVO();
        carrier.setName("承运商" + System.nanoTime());
        carrier.setStatus(status);
        return logisticsCarrierService.createCarrier(carrier);
    }

    private static LogisticsCarrierContractSaveReqVO newContract(Long carrierId, String unitPrice) {
        LogisticsCarrierContractSaveReqVO contract = new LogisticsCarrierContractSaveReqVO();
        contract.setCarrierId(carrierId);
        contract.setEffectiveFrom(LocalDate.now().minusDays(1));
        contract.setEffectiveTo(LocalDate.now().plusDays(30));
        contract.setRoute("城东场站—临平");
        contract.setBillingMode(1);
        contract.setUnitPrice(new BigDecimal(unitPrice));
        contract.setStatus(0);
        return contract;
    }

}
