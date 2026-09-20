package cn.iocoder.yudao.module.icbc.service.purchaseorder;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractAuditReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractSubmitReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseArrangementRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPriceReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderProgressRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderStatusUpdateReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.enums.PurchaseOrderPriceModeEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.icbc.service.purchasecontract.PurchaseContractService;
import cn.iocoder.yudao.module.icbc.service.purchasecontract.impl.PurchaseContractServiceImpl;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.impl.PurchaseOrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PurchaseOrderServiceImpl} 的单元测试（#46 T08）。
 *
 * <p>覆盖验收：一单多条品类明细且一条明细可分多次收货、可选关联合同与执行场站、对手方恰好一个非空、
 * 固定单价与按交货日价格表两种定价、每次成交留价格快照与调整原因、状态流转与「执行中才可作为采购依据」、
 * 只允许删除草稿。
 */
@Import({PurchaseOrderServiceImpl.class, PurchaseContractServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class PurchaseOrderServiceTest extends BaseDbUnitTest {

    @Resource
    private PurchaseOrderService purchaseOrderService;
    @Resource
    private PurchaseContractService purchaseContractService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;
    @Resource
    private IcbcStationMapper stationMapper;

    // ==================== 建单与明细 ====================

    @Test
    public void testCreate_draftWithMultipleItemsAndTotals() {
        PayeeInfoDO payee = insertPayee("张三");
        Long steel = insertGoodsConfig("废钢", "吨");
        Long copper = insertGoodsConfig("废铜", "吨");

        Long orderId = purchaseOrderService.createOrder(naturalReq(payee.getId(), List.of(
                fixedItem(steel, "100", "2000"),
                fixedItem(copper, "20", "50000"))));

        IcbcPurchaseOrderDO order = purchaseOrderService.getOrder(orderId);
        assertEquals(PurchaseOrderStatusEnum.DRAFT.getStatus(), order.getStatus());
        assertNotNull(order.getOrderNo());
        assertEquals("张三", order.getCounterpartyName());
        assertEquals(new BigDecimal("120.0000"), order.getTotalQuantity().setScale(4));
        // 100 × 2000 + 20 × 50000 = 200000 + 1000000
        assertEquals(0, new BigDecimal("1200000.00").compareTo(order.getTotalAmount()));

        PurchaseOrderRespVO detail = purchaseOrderService.getDetail(orderId);
        assertEquals(2, detail.getItems().size());
        assertEquals("废钢", detail.getItems().get(0).getCategoryName());
        assertEquals("吨", detail.getItems().get(0).getUnit());
        assertEquals(0, new BigDecimal("200000.00").compareTo(detail.getItems().get(0).getAmount()));
        // 未开始执行：不得作为采购依据
        assertFalse(detail.getUsableAsPurchaseBasis());
        assertServiceException(() -> purchaseOrderService.assertUsableAsPurchaseBasis(orderId),
                PURCHASE_ORDER_NOT_EFFECTIVE, order.getOrderNo());
    }

    @Test
    public void testCreate_contractMustBeEffectiveBeforeOrderStatusNotUsable() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        // 草稿合同：不得作为采购依据，订单不能挂它
        Long draftContractId = createDraftContract(payee.getId(), goodsId);
        PurchaseOrderSaveReqVO req = naturalReq(payee.getId(), List.of(fixedItem(goodsId, "100", "2000")));
        req.setContractId(draftContractId);
        assertServiceException(() -> purchaseOrderService.createOrder(req),
                PURCHASE_CONTRACT_NOT_EFFECTIVE,
                purchaseContractService.getContract(draftContractId).getContractNo());

        // 审核生效后可以挂，订单快照合同号
        Long effectiveContractId = submitAndAudit(draftContractId);
        req.setContractId(effectiveContractId);
        Long orderId = purchaseOrderService.createOrder(req);
        IcbcPurchaseOrderDO order = purchaseOrderService.getOrder(orderId);
        assertEquals(effectiveContractId, order.getContractId());
        assertEquals(purchaseContractService.getContract(effectiveContractId).getContractNo(),
                order.getContractNo());
    }

    @Test
    public void testCounterparty_naturalAndSupplierExactlyOne() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");

        PurchaseOrderSaveReqVO both = naturalReq(payee.getId(), List.of(fixedItem(goodsId, "1", "10")));
        both.setSupplierId(1001L);
        assertServiceException(() -> purchaseOrderService.createOrder(both),
                PURCHASE_ORDER_COUNTERPARTY_REQUIRED);

        PurchaseOrderSaveReqVO none = naturalReq(payee.getId(), List.of(fixedItem(goodsId, "1", "10")));
        none.setPayeeId(null);
        assertServiceException(() -> purchaseOrderService.createOrder(none),
                PURCHASE_ORDER_COUNTERPARTY_REQUIRED);

        PurchaseOrderSaveReqVO missing = naturalReq(999999L, List.of(fixedItem(goodsId, "1", "10")));
        assertServiceException(() -> purchaseOrderService.createOrder(missing),
                PURCHASE_ORDER_PAYEE_NOT_EXISTS);

        // 单位供货方：恰一个非空，且名称必填
        PurchaseOrderSaveReqVO supplierReq = supplierReq(2002L, "某某再生资源有限公司",
                List.of(fixedItem(goodsId, "1", "10")));
        supplierReq.setCounterpartyName("  ");
        assertServiceException(() -> purchaseOrderService.createOrder(supplierReq),
                PURCHASE_ORDER_SUPPLIER_NAME_REQUIRED);
        supplierReq.setCounterpartyName("某某再生资源有限公司");
        Long supplierOrderId = purchaseOrderService.createOrder(supplierReq);
        IcbcPurchaseOrderDO supplierOrder = purchaseOrderService.getOrder(supplierOrderId);
        assertEquals(2002L, supplierOrder.getSupplierId());
        assertNull(supplierOrder.getPayeeId());
        assertEquals(5, supplierOrder.getCounterpartyType());
    }

    @Test
    public void testCreate_stationSnapshotAndInvalid() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        IcbcStationDO station = insertStation("城东场站");

        PurchaseOrderSaveReqVO req = naturalReq(payee.getId(), List.of(fixedItem(goodsId, "1", "10")));
        req.setStationId(station.getId());
        Long orderId = purchaseOrderService.createOrder(req);
        assertEquals("城东场站", purchaseOrderService.getOrder(orderId).getStationName());

        req.setStationId(888888L);
        assertServiceException(() -> purchaseOrderService.createOrder(req),
                PURCHASE_ORDER_STATION_NOT_EXISTS);
    }

    @Test
    public void testCreate_dateRangeInvalid() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        PurchaseOrderSaveReqVO req = naturalReq(payee.getId(), List.of(fixedItem(goodsId, "1", "10")));
        req.setStartDate(LocalDate.now().plusDays(1));
        req.setEndDate(LocalDate.now());
        assertServiceException(() -> purchaseOrderService.createOrder(req),
                PURCHASE_ORDER_DATE_INVALID);
    }

    @Test
    public void testCreate_itemValidation() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");

        PurchaseOrderSaveReqVO empty = naturalReq(payee.getId(), List.of());
        assertServiceException(() -> purchaseOrderService.createOrder(empty),
                PURCHASE_ORDER_ITEM_REQUIRED);

        PurchaseOrderSaveReqVO missingGoods = naturalReq(payee.getId(),
                List.of(fixedItem(888888L, "1", "10")));
        assertServiceException(() -> purchaseOrderService.createOrder(missingGoods),
                PURCHASE_ORDER_CATEGORY_NOT_EXISTS, 888888L);

        PurchaseOrderSaveReqVO zeroQuantity = naturalReq(payee.getId(),
                List.of(fixedItem(goodsId, "0", "10")));
        assertServiceException(() -> purchaseOrderService.createOrder(zeroQuantity),
                PURCHASE_ORDER_QUANTITY_INVALID, "第 1 条");

        PurchaseOrderSaveReqVO negativePrice = naturalReq(payee.getId(),
                List.of(fixedItem(goodsId, "1", "-1")));
        assertServiceException(() -> purchaseOrderService.createOrder(negativePrice),
                PURCHASE_ORDER_PRICE_INVALID, "第 1 条");

        // 参考单价是所有方式的必填项
        PurchaseOrderItemReqVO noPrice = fixedItem(goodsId, "1", "10");
        noPrice.setUnitPrice(null);
        assertServiceException(() -> purchaseOrderService.createOrder(
                naturalReq(payee.getId(), List.of(noPrice))),
                PURCHASE_ORDER_REFERENCE_PRICE_REQUIRED, "第 1 条");

        // 定价方式不合法
        PurchaseOrderItemReqVO badMode = fixedItem(goodsId, "1", "10");
        badMode.setPriceMode(99);
        assertServiceException(() -> purchaseOrderService.createOrder(
                naturalReq(payee.getId(), List.of(badMode))),
                PURCHASE_ORDER_PRICE_MODE_INVALID, "第 1 条");

        // 按交货日价格表：给了参考单价即可（价格表可留空，成交时回退参考价）
        PurchaseOrderItemReqVO byDateReferenceOnly = new PurchaseOrderItemReqVO();
        byDateReferenceOnly.setGoodsConfigId(goodsId);
        byDateReferenceOnly.setQuantity(new BigDecimal("1"));
        byDateReferenceOnly.setPriceMode(PurchaseOrderPriceModeEnum.BY_DELIVERY_DATE.getMode());
        byDateReferenceOnly.setUnitPrice(new BigDecimal("1000"));
        assertNotNull(purchaseOrderService.createOrder(
                naturalReq(payee.getId(), List.of(byDateReferenceOnly))));

        // 价格表条目缺日期 / 单价 → 拒
        PurchaseOrderItemReqVO badTable = new PurchaseOrderItemReqVO();
        badTable.setGoodsConfigId(goodsId);
        badTable.setQuantity(new BigDecimal("1"));
        badTable.setPriceMode(PurchaseOrderPriceModeEnum.BY_DELIVERY_DATE.getMode());
        badTable.setUnitPrice(new BigDecimal("1000"));
        PurchaseOrderPriceReqVO price = new PurchaseOrderPriceReqVO();
        price.setUnitPrice(new BigDecimal("10"));
        badTable.setPrices(List.of(price));
        assertServiceException(() -> purchaseOrderService.createOrder(
                naturalReq(payee.getId(), List.of(badTable))),
                PURCHASE_ORDER_PRICE_TABLE_INVALID);
    }

    // ==================== 状态流转与门禁 ====================

    @Test
    public void testStatusFlow_startSuspendResumeCompleteClose() {
        Long orderId = createDefaultOrder().orderId;

        // 草稿直接完成：不允许
        assertServiceException(() -> purchaseOrderService.updateStatus(statusReq(orderId,
                PurchaseOrderStatusEnum.COMPLETED.getStatus(), null)),
                PURCHASE_ORDER_STATUS_TRANSITION_INVALID,
                PurchaseOrderStatusEnum.DRAFT.getName(), PurchaseOrderStatusEnum.COMPLETED.getName());

        // 开始执行
        purchaseOrderService.updateStatus(statusReq(orderId, PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));
        assertEquals(PurchaseOrderStatusEnum.EXECUTING.getStatus(),
                purchaseOrderService.getOrder(orderId).getStatus());
        assertNotNull(purchaseOrderService.assertUsableAsPurchaseBasis(orderId));

        // 执行中的订单不能再改明细
        PurchaseOrderSaveReqVO update = naturalReq(purchaseOrderService.getOrder(orderId).getPayeeId(),
                List.of(fixedItem(insertGoodsConfig("废钢2", "吨"), "1", "10")));
        update.setId(orderId);
        assertServiceException(() -> purchaseOrderService.updateOrder(update),
                PURCHASE_ORDER_STATUS_NOT_ALLOW, PurchaseOrderStatusEnum.EXECUTING.getName());

        // 暂停必须写原因
        assertServiceException(() -> purchaseOrderService.updateStatus(statusReq(orderId,
                PurchaseOrderStatusEnum.SUSPENDED.getStatus(), "  ")),
                PURCHASE_ORDER_SUSPEND_REASON_REQUIRED);
        purchaseOrderService.updateStatus(statusReq(orderId, PurchaseOrderStatusEnum.SUSPENDED.getStatus(), "缺货暂停"));
        IcbcPurchaseOrderDO suspended = purchaseOrderService.getOrder(orderId);
        assertEquals(PurchaseOrderStatusEnum.SUSPENDED.getStatus(), suspended.getStatus());
        assertEquals("缺货暂停", suspended.getSuspendReason());
        // 暂停不再作为采购依据
        assertServiceException(() -> purchaseOrderService.assertUsableAsPurchaseBasis(orderId),
                PURCHASE_ORDER_NOT_EFFECTIVE, suspended.getOrderNo());

        // 恢复：清掉暂停痕迹
        purchaseOrderService.updateStatus(statusReq(orderId, PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));
        assertNull(purchaseOrderService.getOrder(orderId).getSuspendReason());

        // 完成 → 关闭
        purchaseOrderService.updateStatus(statusReq(orderId, PurchaseOrderStatusEnum.COMPLETED.getStatus(), null));
        assertNotNull(purchaseOrderService.getOrder(orderId).getCompletedTime());
        purchaseOrderService.updateStatus(statusReq(orderId, PurchaseOrderStatusEnum.CLOSED.getStatus(), "结清关闭"));
        assertEquals(PurchaseOrderStatusEnum.CLOSED.getStatus(), purchaseOrderService.getOrder(orderId).getStatus());

        // 关闭是终态
        assertServiceException(() -> purchaseOrderService.updateStatus(statusReq(orderId,
                PurchaseOrderStatusEnum.EXECUTING.getStatus(), null)),
                PURCHASE_ORDER_STATUS_TRANSITION_INVALID,
                PurchaseOrderStatusEnum.CLOSED.getName(), PurchaseOrderStatusEnum.EXECUTING.getName());
        // 关闭后不再是采购依据，但订单与已发生的业务仍在
        assertServiceException(() -> purchaseOrderService.assertUsableAsPurchaseBasis(orderId),
                PURCHASE_ORDER_NOT_EFFECTIVE, purchaseOrderService.getOrder(orderId).getOrderNo());
        assertNotNull(purchaseOrderService.getOrder(orderId));
    }

    @Test
    public void testExpired_executingNotUsable() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        PurchaseOrderSaveReqVO req = naturalReq(payee.getId(), List.of(fixedItem(goodsId, "1", "10")));
        req.setStartDate(LocalDate.now().minusDays(10));
        req.setEndDate(LocalDate.now().minusDays(1));
        Long orderId = purchaseOrderService.createOrder(req);
        purchaseOrderService.updateStatus(statusReq(orderId, PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));

        PurchaseOrderRespVO detail = purchaseOrderService.getDetail(orderId);
        assertTrue(detail.getExpired());
        assertEquals("过期", detail.getStatusName());
        assertFalse(detail.getUsableAsPurchaseBasis());
        assertServiceException(() -> purchaseOrderService.assertUsableAsPurchaseBasis(orderId),
                PURCHASE_ORDER_NOT_EFFECTIVE, detail.getOrderNo());
    }

    // ==================== 可选采购安排（#51 T13） ====================

    @Test
    public void testGetUsableArrangements_onlyExecutingNotExpiredForThatPayee() {
        PayeeInfoDO payee = insertPayee("张三");
        PayeeInfoDO other = insertPayee("李四");
        Long goodsId = insertGoodsConfig("废钢", "吨");

        // 可用：执行中且未过期
        Long usable = purchaseOrderService.createOrder(naturalReq(payee.getId(),
                List.of(fixedItem(goodsId, "100", "2000"))));
        purchaseOrderService.updateStatus(statusReq(usable, PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));
        // 不可用：草稿
        purchaseOrderService.createOrder(naturalReq(payee.getId(), List.of(fixedItem(goodsId, "1", "10"))));
        // 不可用：执行中但已过期
        PurchaseOrderSaveReqVO expired = naturalReq(payee.getId(), List.of(fixedItem(goodsId, "1", "10")));
        expired.setStartDate(LocalDate.now().minusDays(10));
        expired.setEndDate(LocalDate.now().minusDays(1));
        Long expiredId = purchaseOrderService.createOrder(expired);
        purchaseOrderService.updateStatus(statusReq(expiredId, PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));
        // 不可用：别的交易对方
        Long otherOrder = purchaseOrderService.createOrder(naturalReq(other.getId(),
                List.of(fixedItem(goodsId, "1", "10"))));
        purchaseOrderService.updateStatus(statusReq(otherOrder, PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));

        List<PurchaseArrangementRespVO> arrangements = purchaseOrderService.getUsableArrangements(payee.getId());

        assertEquals(1, arrangements.size());
        PurchaseArrangementRespVO arrangement = arrangements.get(0);
        assertEquals(usable, arrangement.getOrderId());
        assertEquals(purchaseOrderService.getOrder(usable).getOrderNo(), arrangement.getOrderNo());
        assertEquals("张三", arrangement.getCounterpartyName());
        assertEquals(1, arrangement.getItems().size());
        assertEquals(goodsId, arrangement.getItems().get(0).getGoodsConfigId());
        assertEquals("废钢", arrangement.getItems().get(0).getCategoryName());
        assertEquals(0, new BigDecimal("2000").compareTo(arrangement.getItems().get(0).getUnitPrice()));
    }

    @Test
    public void testGetUsableArrangements_withoutPayeeIsEmpty() {
        Long goodsId = insertGoodsConfig("废钢", "吨");
        PayeeInfoDO payee = insertPayee("张三");
        Long orderId = purchaseOrderService.createOrder(naturalReq(payee.getId(),
                List.of(fixedItem(goodsId, "1", "10"))));
        purchaseOrderService.updateStatus(statusReq(orderId, PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));

        assertTrue(purchaseOrderService.getUsableArrangements(null).isEmpty());
    }

    @Test
    public void testGetOrderItem_rejectsItemOfAnotherOrder() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        Long first = purchaseOrderService.createOrder(naturalReq(payee.getId(),
                List.of(fixedItem(goodsId, "1", "10"))));
        Long second = purchaseOrderService.createOrder(naturalReq(payee.getId(),
                List.of(fixedItem(goodsId, "2", "20"))));
        Long firstItem = purchaseOrderService.getDetail(first).getItems().get(0).getId();

        assertEquals(firstItem, purchaseOrderService.getOrderItem(first, firstItem).getId());
        assertServiceException(() -> purchaseOrderService.getOrderItem(second, firstItem),
                PURCHASE_ORDER_ITEM_NOT_EXISTS, firstItem);
    }

    // ==================== 分次收货与价格快照 ====================

    @Test
    public void testRecordDeal_multipleDealsAgainstOneItem() {
        OrderFixture fixture = createDefaultOrder();
        purchaseOrderService.updateStatus(statusReq(fixture.orderId,
                PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));

        // 同一条明细分三次收货
        purchaseOrderService.recordDeal(dealReq(fixture.orderId, fixture.itemId, "10", "2000", null, null));
        purchaseOrderService.recordDeal(dealReq(fixture.orderId, fixture.itemId, "30", "2000", null, null));
        purchaseOrderService.recordDeal(dealReq(fixture.orderId, fixture.itemId, "15.5", "2000", null, null));

        PurchaseOrderRespVO detail = purchaseOrderService.getDetail(fixture.orderId);
        assertEquals(0, new BigDecimal("55.5").compareTo(detail.getItems().get(0).getReceivedQuantity()));
        assertEquals(3, detail.getItems().get(0).getDealCount());
        assertEquals(0, new BigDecimal("44.5").compareTo(detail.getItems().get(0).getRemainingQuantity()));

        // 每次成交留价格快照，只追加
        List<PurchaseOrderDealRespVO> deals = purchaseOrderService.getDealList(fixture.orderId);
        assertEquals(3, deals.size());
        assertEquals("废钢", deals.get(0).getCategoryName());
        assertFalse(deals.get(0).getPriceAdjusted());
        assertNotNull(deals.get(0).getDealNo());
        assertNotNull(deals.get(0).getDealTime());
    }

    @Test
    public void testRecordDeal_adjustReasonRequiredWhenPriceDiffers() {
        OrderFixture fixture = createDefaultOrder();
        purchaseOrderService.updateStatus(statusReq(fixture.orderId,
                PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));

        // 成交价与参考价不一致：必须说明
        assertServiceException(() -> purchaseOrderService.recordDeal(
                dealReq(fixture.orderId, fixture.itemId, "10", "1900", null, null)),
                PURCHASE_ORDER_DEAL_ADJUST_REASON_REQUIRED);

        purchaseOrderService.recordDeal(dealReq(fixture.orderId, fixture.itemId, "10", "1900", null, "含运费下调"));
        PurchaseOrderDealRespVO deal = purchaseOrderService.getDealList(fixture.orderId).get(0);
        assertTrue(deal.getPriceAdjusted());
        assertEquals("含运费下调", deal.getAdjustReason());
        assertEquals(0, new BigDecimal("2000").compareTo(deal.getReferenceUnitPrice()));
        assertEquals(0, new BigDecimal("1900").compareTo(deal.getUnitPrice()));
    }

    @Test
    public void testRecordDeal_requiresExecutingOrder() {
        OrderFixture fixture = createDefaultOrder();
        // 草稿不能有成交
        assertServiceException(() -> purchaseOrderService.recordDeal(
                dealReq(fixture.orderId, fixture.itemId, "10", "2000", null, null)),
                PURCHASE_ORDER_NOT_EFFECTIVE, purchaseOrderService.getOrder(fixture.orderId).getOrderNo());
        // 数量必须大于 0
        purchaseOrderService.updateStatus(statusReq(fixture.orderId,
                PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));
        assertServiceException(() -> purchaseOrderService.recordDeal(
                dealReq(fixture.orderId, fixture.itemId, "0", "2000", null, null)),
                PURCHASE_ORDER_DEAL_QUANTITY_INVALID);
        // 成交明细必须属于这个订单
        assertServiceException(() -> purchaseOrderService.recordDeal(
                dealReq(fixture.orderId, 888888L, "10", "2000", null, null)),
                PURCHASE_ORDER_ITEM_NOT_EXISTS, 888888L);
    }

    @Test
    public void testResolveUnitPrice_fixedTableAndFallback() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");

        // 固定单价：永远用明细单价
        PurchaseOrderItemReqVO fixed = fixedItem(goodsId, "10", "2000");
        Long fixedOrderId = purchaseOrderService.createOrder(naturalReq(payee.getId(), List.of(fixed)));
        Long fixedItemId = purchaseOrderService.getDetail(fixedOrderId).getItems().get(0).getId();
        assertEquals(0, new BigDecimal("2000").compareTo(
                purchaseOrderService.resolveUnitPrice(fixedItemId, LocalDate.now())));

        // 按交货日价格表：取「不晚于当日的最新一条」；未覆盖则回退明细单价
        PurchaseOrderItemReqVO byDate = new PurchaseOrderItemReqVO();
        byDate.setGoodsConfigId(goodsId);
        byDate.setQuantity(new BigDecimal("10"));
        byDate.setPriceMode(PurchaseOrderPriceModeEnum.BY_DELIVERY_DATE.getMode());
        byDate.setUnitPrice(new BigDecimal("1000"));
        byDate.setPrices(List.of(
                priceEntry(LocalDate.of(2026, 1, 1), "2100"),
                priceEntry(LocalDate.of(2026, 6, 1), "2300")));
        Long byDateOrderId = purchaseOrderService.createOrder(naturalReq(payee.getId(), List.of(byDate)));
        Long byDateItemId = purchaseOrderService.getDetail(byDateOrderId).getItems().get(0).getId();

        assertEquals(0, new BigDecimal("2100").compareTo(
                purchaseOrderService.resolveUnitPrice(byDateItemId, LocalDate.of(2026, 3, 15))));
        assertEquals(0, new BigDecimal("2300").compareTo(
                purchaseOrderService.resolveUnitPrice(byDateItemId, LocalDate.of(2026, 6, 1))));
        assertEquals(0, new BigDecimal("2300").compareTo(
                purchaseOrderService.resolveUnitPrice(byDateItemId, LocalDate.of(2026, 12, 31))));
        // 价格表还没生效的日期：回退明细参考单价
        assertEquals(0, new BigDecimal("1000").compareTo(
                purchaseOrderService.resolveUnitPrice(byDateItemId, LocalDate.of(2025, 12, 1))));

        // 按价格表成交：参考价来自价格表
        purchaseOrderService.updateStatus(statusReq(byDateOrderId,
                PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));
        purchaseOrderService.recordDeal(dealReq(byDateOrderId, byDateItemId, "5", "2300",
                LocalDate.of(2026, 6, 1), null));
        PurchaseOrderDealRespVO deal = purchaseOrderService.getDealList(byDateOrderId).get(0);
        assertEquals(0, new BigDecimal("2300").compareTo(deal.getReferenceUnitPrice()));
        assertFalse(deal.getPriceAdjusted());
    }

    @Test
    public void testProgress_plannedReceivedRemaining() {
        OrderFixture fixture = createDefaultOrder();
        purchaseOrderService.updateStatus(statusReq(fixture.orderId,
                PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));
        purchaseOrderService.recordDeal(dealReq(fixture.orderId, fixture.itemId, "40", "2000", null, null));

        PurchaseOrderProgressRespVO progress = purchaseOrderService.getProgress(fixture.orderId);
        assertEquals(fixture.orderNo, progress.getOrderNo());
        assertEquals(0, new BigDecimal("100").compareTo(progress.getTotalQuantity()));
        assertEquals(0, new BigDecimal("40").compareTo(progress.getReceivedQuantity()));
        assertEquals(0, new BigDecimal("60").compareTo(progress.getRemainingQuantity()));
        assertEquals(1, progress.getItems().size());
        assertNotNull(progress.getScopeNote());
    }

    // ==================== 删除 / 只读金额 / 分页 ====================

    @Test
    public void testDelete_draftOnly() {
        OrderFixture fixture = createDefaultOrder();
        purchaseOrderService.updateStatus(statusReq(fixture.orderId,
                PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));
        assertServiceException(() -> purchaseOrderService.deleteOrder(fixture.orderId),
                PURCHASE_ORDER_ONLY_DRAFT_DELETABLE);

        Long draftId = createDefaultOrder().orderId;
        purchaseOrderService.deleteOrder(draftId);
        assertServiceException(() -> purchaseOrderService.getOrder(draftId), PURCHASE_ORDER_NOT_EXISTS);
    }

    @Test
    public void testGetOrderAmount_readOnlyForInputInvoice() {
        OrderFixture fixture = createDefaultOrder();
        PurchaseOrderAmountDTO amount = purchaseOrderService.getOrderAmount(fixture.orderId);
        assertEquals(fixture.orderId, amount.getOrderId());
        assertEquals(fixture.orderNo, amount.getOrderNo());
        assertEquals(0, new BigDecimal("200000.00").compareTo(amount.getTotalAmount()));
        assertFalse(amount.getUsableAsPurchaseBasis());

        purchaseOrderService.updateStatus(statusReq(fixture.orderId,
                PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));
        assertTrue(purchaseOrderService.getOrderAmount(fixture.orderId).getUsableAsPurchaseBasis());
    }

    @Test
    public void testPage_filterByStatusAndCounterparty() {
        OrderFixture draft = createDefaultOrder();
        PayeeInfoDO payee = insertPayee("李四");
        Long goodsId = insertGoodsConfig("废铝", "吨");
        Long supplierOrderId = purchaseOrderService.createOrder(supplierReq(2002L, "某某再生资源",
                List.of(fixedItem(goodsId, "5", "100"))));
        purchaseOrderService.updateStatus(statusReq(supplierOrderId,
                PurchaseOrderStatusEnum.EXECUTING.getStatus(), null));

        PurchaseOrderPageReqVO draftQuery = new PurchaseOrderPageReqVO();
        draftQuery.setStatus(PurchaseOrderStatusEnum.DRAFT.getStatus());
        assertEquals(1, purchaseOrderService.getOrderPage(draftQuery).getTotal());
        assertEquals(draft.orderId, purchaseOrderService.getOrderPage(draftQuery).getList().get(0).getId());

        PurchaseOrderPageReqVO supplierQuery = new PurchaseOrderPageReqVO();
        supplierQuery.setCounterpartyType(5);
        assertEquals(1, purchaseOrderService.getOrderPage(supplierQuery).getTotal());
        assertEquals("某某再生资源",
                purchaseOrderService.getOrderPage(supplierQuery).getList().get(0).getCounterpartyName());
    }

    // ==================== 造数 ====================

    private static class OrderFixture {
        Long orderId;
        Long itemId;
        String orderNo;
    }

    private OrderFixture createDefaultOrder() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        Long orderId = purchaseOrderService.createOrder(naturalReq(payee.getId(),
                List.of(fixedItem(goodsId, "100", "2000"))));
        OrderFixture fixture = new OrderFixture();
        fixture.orderId = orderId;
        fixture.itemId = purchaseOrderService.getDetail(orderId).getItems().get(0).getId();
        fixture.orderNo = purchaseOrderService.getOrder(orderId).getOrderNo();
        return fixture;
    }

    private Long createDraftContract(Long payeeId, Long goodsId) {
        PurchaseContractSaveReqVO reqVO = new PurchaseContractSaveReqVO();
        reqVO.setName("2026 年度废钢采购合同");
        reqVO.setCounterpartyType(1);
        reqVO.setPayeeId(payeeId);
        reqVO.setStartDate(LocalDate.now().minusDays(1));
        reqVO.setEndDate(LocalDate.now().plusDays(365));
        reqVO.setCategoryIds(List.of(goodsId));
        return purchaseContractService.createContract(reqVO);
    }

    private Long submitAndAudit(Long contractId) {
        PurchaseContractSubmitReqVO submit = new PurchaseContractSubmitReqVO();
        submit.setId(contractId);
        submit.setChangeReason("首次送审");
        purchaseContractService.submitForAudit(submit);
        PurchaseContractAuditReqVO audit = new PurchaseContractAuditReqVO();
        audit.setId(contractId);
        audit.setApproved(true);
        audit.setRemark("通过");
        purchaseContractService.audit(audit);
        return contractId;
    }

    private PurchaseOrderSaveReqVO naturalReq(Long payeeId, List<PurchaseOrderItemReqVO> items) {
        PurchaseOrderSaveReqVO reqVO = new PurchaseOrderSaveReqVO();
        reqVO.setCounterpartyType(1);
        reqVO.setPayeeId(payeeId);
        reqVO.setStartDate(LocalDate.now().minusDays(1));
        reqVO.setEndDate(LocalDate.now().plusDays(30));
        reqVO.setItems(items);
        return reqVO;
    }

    private PurchaseOrderSaveReqVO supplierReq(Long supplierId, String name,
                                               List<PurchaseOrderItemReqVO> items) {
        PurchaseOrderSaveReqVO reqVO = new PurchaseOrderSaveReqVO();
        reqVO.setCounterpartyType(5);
        reqVO.setSupplierId(supplierId);
        reqVO.setCounterpartyName(name);
        reqVO.setStartDate(LocalDate.now().minusDays(1));
        reqVO.setEndDate(LocalDate.now().plusDays(30));
        reqVO.setItems(items);
        return reqVO;
    }

    private PurchaseOrderItemReqVO fixedItem(Long goodsConfigId, String quantity, String unitPrice) {
        PurchaseOrderItemReqVO item = new PurchaseOrderItemReqVO();
        item.setGoodsConfigId(goodsConfigId);
        item.setQuantity(new BigDecimal(quantity));
        item.setPriceMode(PurchaseOrderPriceModeEnum.FIXED.getMode());
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }

    private PurchaseOrderPriceReqVO priceEntry(LocalDate deliveryDate, String unitPrice) {
        PurchaseOrderPriceReqVO price = new PurchaseOrderPriceReqVO();
        price.setDeliveryDate(deliveryDate);
        price.setUnitPrice(new BigDecimal(unitPrice));
        return price;
    }

    private PurchaseOrderStatusUpdateReqVO statusReq(Long id, Integer status, String reason) {
        PurchaseOrderStatusUpdateReqVO reqVO = new PurchaseOrderStatusUpdateReqVO();
        reqVO.setId(id);
        reqVO.setStatus(status);
        reqVO.setReason(reason);
        return reqVO;
    }

    private PurchaseOrderDealReqVO dealReq(Long orderId, Long itemId, String quantity, String unitPrice,
                                           LocalDate deliveryDate, String adjustReason) {
        PurchaseOrderDealReqVO reqVO = new PurchaseOrderDealReqVO();
        reqVO.setOrderId(orderId);
        reqVO.setItemId(itemId);
        reqVO.setQuantity(new BigDecimal(quantity));
        reqVO.setUnitPrice(new BigDecimal(unitPrice));
        reqVO.setDeliveryDate(deliveryDate);
        reqVO.setAdjustReason(adjustReason);
        return reqVO;
    }

    private static final AtomicInteger PAYEE_SEQ = new AtomicInteger();

    private PayeeInfoDO insertPayee(String name) {
        int seq = PAYEE_SEQ.incrementAndGet();
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId("PARTNER_" + name + "_" + seq)
                .name(name)
                .mobile(String.format("138%08d", seq))
                .idCardNo(String.format("1101011990%08d", seq))
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private Long insertGoodsConfig(String name, String unit) {
        IcbcGoodsConfigDO goodsConfig = new IcbcGoodsConfigDO();
        goodsConfig.setName(name);
        goodsConfig.setUnit(unit);
        goodsConfig.setStatus(0);
        goodsConfigMapper.insert(goodsConfig);
        return goodsConfig.getId();
    }

    private IcbcStationDO insertStation(String name) {
        IcbcStationDO station = IcbcStationDO.builder()
                .stationCode("ST_" + name)
                .name(name)
                .openStatus(1)
                .build();
        stationMapper.insert(station);
        return station;
    }

}
