package cn.iocoder.yudao.module.icbc.service.purchaseorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionReviewReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.enums.PurchaseExceptionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseExceptionTypeEnum;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.impl.PurchaseOrderExceptionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PurchaseOrderExceptionServiceImpl} 的单元测试（#47 T09）。
 *
 * <p>覆盖：提交时的类型 / 明细 / 场站 / 原因校验与「同一异常不重复提交」、审核通过（可收紧追加量与
 * 有效期）与拒绝、只有待审核能审、分页与口径说明、授权过有效期后不再生效。
 */
@Import({PurchaseOrderExceptionServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class PurchaseOrderExceptionServiceTest extends BaseDbUnitTest {

    @Resource
    private PurchaseOrderExceptionService purchaseOrderExceptionService;
    @Resource
    private IcbcPurchaseOrderMapper orderMapper;
    @Resource
    private IcbcPurchaseOrderItemMapper itemMapper;
    @Resource
    private IcbcStationMapper stationMapper;

    @Test
    public void testRequest_overQuantityRequiresItemOfThisOrder() {
        Long orderId = insertOrder();
        Long itemId = insertItem(orderId, "废钢");

        // 订单不存在
        PurchaseOrderExceptionSaveReqVO missingOrder = requestReq(888888L, null,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "10", "客户急送");
        assertServiceException(() -> purchaseOrderExceptionService.requestException(missingOrder),
                PURCHASE_ORDER_NOT_EXISTS);

        // 类型不合法
        PurchaseOrderExceptionSaveReqVO badType = requestReq(orderId, itemId, null, null, "10", "客户急送");
        badType.setExceptionType("SOMETHING_ELSE");
        assertServiceException(() -> purchaseOrderExceptionService.requestException(badType),
                PURCHASE_EXCEPTION_TYPE_INVALID, "SOMETHING_ELSE");

        // 原因必填与数量必须为正
        PurchaseOrderExceptionSaveReqVO noReason = requestReq(orderId, itemId,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "10", "  ");
        assertServiceException(() -> purchaseOrderExceptionService.requestException(noReason),
                PURCHASE_EXCEPTION_REASON_REQUIRED);
        PurchaseOrderExceptionSaveReqVO zeroQuantity = requestReq(orderId, itemId,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "0", "客户急送");
        assertServiceException(() -> purchaseOrderExceptionService.requestException(zeroQuantity),
                PURCHASE_EXCEPTION_QUANTITY_INVALID);

        // 超量必须指明明细，且明细必须属于这个订单
        PurchaseOrderExceptionSaveReqVO noItem = requestReq(orderId, null,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "10", "客户急送");
        assertServiceException(() -> purchaseOrderExceptionService.requestException(noItem),
                PURCHASE_EXCEPTION_ITEM_REQUIRED);
        PurchaseOrderExceptionSaveReqVO otherOrderItem = requestReq(orderId, insertItem(insertOrder(), "废铜"),
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "10", "客户急送");
        assertServiceException(() -> purchaseOrderExceptionService.requestException(otherOrderItem),
                PURCHASE_ORDER_ITEM_NOT_EXISTS, otherOrderItem.getItemId());

        // 正常提交
        Long id = purchaseOrderExceptionService.requestException(requestReq(orderId, itemId,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "10", "客户急送一车"));
        PurchaseOrderExceptionRespVO resp = purchaseOrderExceptionService.getException(id);
        assertEquals(PurchaseExceptionStatusEnum.PENDING.getStatus(), resp.getStatus());
        assertEquals("待审核", resp.getStatusName());
        assertEquals("超量交货", resp.getExceptionTypeName());
        assertEquals("废钢", resp.getCategoryName());
        assertEquals(0, new BigDecimal("10").compareTo(resp.getRequestedQuantity()));
        assertFalse(resp.getEffective());
        assertNotNull(resp.getExceptionNo());
        assertNotNull(resp.getRequestedTime());
        assertNotNull(resp.getScopeNote());
        // 只追加：同一异常还有待审核的申请，不能重复提交
        assertServiceException(() -> purchaseOrderExceptionService.requestException(requestReq(orderId, itemId,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "10", "再提一次")),
                PURCHASE_EXCEPTION_ALREADY_PENDING, resp.getExceptionNo());
    }

    @Test
    public void testRequest_crossStationRequiresStation() {
        Long orderId = insertOrder();
        // 跨场站必须指明场站
        PurchaseOrderExceptionSaveReqVO noStation = requestReq(orderId, null,
                PurchaseExceptionTypeEnum.CROSS_STATION, null, "10", "客户改到城西");
        assertServiceException(() -> purchaseOrderExceptionService.requestException(noStation),
                PURCHASE_EXCEPTION_STATION_REQUIRED);
        // 场站必须存在
        PurchaseOrderExceptionSaveReqVO badStation = requestReq(orderId, null,
                PurchaseExceptionTypeEnum.CROSS_STATION, 888888L, "10", "客户改到城西");
        assertServiceException(() -> purchaseOrderExceptionService.requestException(badStation),
                PURCHASE_ORDER_STATION_NOT_EXISTS);

        IcbcStationDO station = insertStation("城西场站");
        Long id = purchaseOrderExceptionService.requestException(requestReq(orderId, null,
                PurchaseExceptionTypeEnum.CROSS_STATION, station.getId(), "10", "客户改到城西"));
        PurchaseOrderExceptionRespVO resp = purchaseOrderExceptionService.getException(id);
        assertEquals("城西场站", resp.getStationName());
        assertEquals(station.getId(), resp.getStationId());
        assertEquals("跨场站交货", resp.getExceptionTypeName());
    }

    @Test
    public void testRequest_expiredIsOrderLevelAndPendingScopeNote() {
        Long orderId = insertOrder();
        Long itemId = insertItem(orderId, "废钢");
        // 过期是订单级异常：即使传了明细也不挂在明细上
        Long expired = purchaseOrderExceptionService.requestException(requestReq(orderId, itemId,
                PurchaseExceptionTypeEnum.EXPIRED, null, "10", "合同还没续签"));
        PurchaseOrderExceptionRespVO expiredResp = purchaseOrderExceptionService.getException(expired);
        assertNull(expiredResp.getItemId());
        assertNull(expiredResp.getCategoryName());
        assertEquals("过期交货", expiredResp.getExceptionTypeName());
        assertTrue(expiredResp.getScopeNote().contains("订单结束日期之后"));

        // 超量申请还没审的时候，口径说明不谎说「追加量 0」
        Long overQuantity = purchaseOrderExceptionService.requestException(requestReq(orderId, itemId,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "10", "客户急送"));
        PurchaseOrderExceptionRespVO pending = purchaseOrderExceptionService.getException(overQuantity);
        assertTrue(pending.getScopeNote().startsWith("授权未生效"));
        assertTrue(pending.getScopeNote().contains("追加量由审核人给出"));
    }

    @Test
    public void testReview_approveTightensScopeAndOnlyPendingCanBeReviewed() {
        Long orderId = insertOrder();
        Long itemId = insertItem(orderId, "废钢");
        Long id = purchaseOrderExceptionService.requestException(requestReq(orderId, itemId,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "10", "客户急送"));
        // 审核通过：追加量与有效期由审核人收紧
        purchaseOrderExceptionService.reviewException(reviewReq(id, true, "3", LocalDate.now().plusDays(7), "只放 3 吨"));

        PurchaseOrderExceptionRespVO approved = purchaseOrderExceptionService.getException(id);
        assertEquals(PurchaseExceptionStatusEnum.APPROVED.getStatus(), approved.getStatus());
        assertEquals("已通过", approved.getStatusName());
        assertTrue(approved.getEffective());
        assertEquals(0, new BigDecimal("3").compareTo(approved.getApprovedQuantity()));
        assertEquals(LocalDate.now().plusDays(7), approved.getValidUntil());
        assertEquals("只放 3 吨", approved.getReviewRemark());
        assertTrue(approved.getScopeNote().contains("3"));

        // 已审核的不再能审第二次
        assertServiceException(() -> purchaseOrderExceptionService.reviewException(
                reviewReq(id, true, null, null, "再审一次")),
                PURCHASE_EXCEPTION_STATUS_NOT_ALLOW, "已通过");

        // 只填结论、不填追加量：按提交的交货量生效
        Long second = purchaseOrderExceptionService.requestException(requestReq(orderId, itemId,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "8", "再来一车"));
        purchaseOrderExceptionService.reviewException(reviewReq(second, true, null, null, "同意"));
        assertEquals(0, new BigDecimal("8").compareTo(
                purchaseOrderExceptionService.getException(second).getApprovedQuantity()));
    }

    @Test
    public void testReview_rejectAndExpiredAuthorization() {
        Long orderId = insertOrder();
        Long itemId = insertItem(orderId, "废钢");
        Long id = purchaseOrderExceptionService.requestException(requestReq(orderId, itemId,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "10", "客户急送"));
        purchaseOrderExceptionService.reviewException(reviewReq(id, false, null, null, "不同意，先补合同"));
        PurchaseOrderExceptionRespVO rejected = purchaseOrderExceptionService.getException(id);
        assertEquals(PurchaseExceptionStatusEnum.REJECTED.getStatus(), rejected.getStatus());
        assertEquals("已拒绝", rejected.getStatusName());
        assertNull(rejected.getApprovedQuantity());
        assertFalse(rejected.getEffective());

        // 有效期已过：授权不再生效（状态仍是已通过，有效与否是推导出来的）
        Long expired = purchaseOrderExceptionService.requestException(requestReq(orderId, itemId,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "10", "客户急送"));
        purchaseOrderExceptionService.reviewException(reviewReq(expired, true, "2",
                LocalDate.now().minusDays(1), "当时同意"));
        PurchaseOrderExceptionRespVO expiredResp = purchaseOrderExceptionService.getException(expired);
        assertEquals(PurchaseExceptionStatusEnum.APPROVED.getStatus(), expiredResp.getStatus());
        assertFalse(expiredResp.getEffective());
        assertTrue(expiredResp.getScopeNote().startsWith("授权已过有效期"));
    }

    @Test
    public void testGetExceptionPage_filterByTypeAndStatus() {
        Long orderId = insertOrder();
        Long itemId = insertItem(orderId, "废钢");
        Long overQuantity = purchaseOrderExceptionService.requestException(requestReq(orderId, itemId,
                PurchaseExceptionTypeEnum.OVER_QUANTITY, null, "10", "客户急送"));
        purchaseOrderExceptionService.reviewException(reviewReq(overQuantity, true, "10", null, "同意"));
        purchaseOrderExceptionService.requestException(requestReq(orderId, null,
                PurchaseExceptionTypeEnum.EXPIRED, null, "10", "合同还没续签"));

        PurchaseOrderExceptionPageReqVO all = new PurchaseOrderExceptionPageReqVO();
        all.setOrderId(orderId);
        PageResult<PurchaseOrderExceptionRespVO> allPage = purchaseOrderExceptionService.getExceptionPage(all);
        assertEquals(2, allPage.getTotal());

        PurchaseOrderExceptionPageReqVO pending = new PurchaseOrderExceptionPageReqVO();
        pending.setOrderId(orderId);
        pending.setStatus(PurchaseExceptionStatusEnum.PENDING.getStatus());
        PageResult<PurchaseOrderExceptionRespVO> pendingPage =
                purchaseOrderExceptionService.getExceptionPage(pending);
        assertEquals(1, pendingPage.getTotal());
        assertEquals("过期交货", pendingPage.getList().get(0).getExceptionTypeName());

        PurchaseOrderExceptionPageReqVO approvedQuery = new PurchaseOrderExceptionPageReqVO();
        approvedQuery.setExceptionType(PurchaseExceptionTypeEnum.OVER_QUANTITY.getCode());
        approvedQuery.setStatus(PurchaseExceptionStatusEnum.APPROVED.getStatus());
        assertEquals(1, purchaseOrderExceptionService.getExceptionPage(approvedQuery).getTotal());

        assertServiceException(() -> purchaseOrderExceptionService.getException(888888L),
                PURCHASE_EXCEPTION_NOT_EXISTS);
    }

    // ==================== 造数 ====================

    private static final AtomicInteger SEQ = new AtomicInteger();

    private Long insertOrder() {
        int seq = SEQ.incrementAndGet();
        IcbcPurchaseOrderDO order = IcbcPurchaseOrderDO.builder()
                .orderNo("PO" + seq)
                .counterpartyType(1)
                .payeeId(1L)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .status(1)
                .build();
        orderMapper.insert(order);
        return order.getId();
    }

    private Long insertItem(Long orderId, String categoryName) {
        IcbcPurchaseOrderItemDO item = IcbcPurchaseOrderItemDO.builder()
                .orderId(orderId)
                .goodsConfigId(1L)
                .categoryName(categoryName)
                .unit("吨")
                .quantity(new BigDecimal("100"))
                .priceMode(1)
                .unitPrice(new BigDecimal("2000"))
                .build();
        itemMapper.insert(item);
        return item.getId();
    }

    private IcbcStationDO insertStation(String name) {
        IcbcStationDO station = IcbcStationDO.builder()
                .stationCode("ST_" + name + SEQ.incrementAndGet())
                .name(name)
                .openStatus(1)
                .build();
        stationMapper.insert(station);
        return station;
    }

    private PurchaseOrderExceptionSaveReqVO requestReq(Long orderId, Long itemId, PurchaseExceptionTypeEnum type,
                                                       Long stationId, String quantity, String reason) {
        PurchaseOrderExceptionSaveReqVO reqVO = new PurchaseOrderExceptionSaveReqVO();
        reqVO.setOrderId(orderId);
        reqVO.setItemId(itemId);
        reqVO.setExceptionType(type == null ? null : type.getCode());
        reqVO.setStationId(stationId);
        reqVO.setRequestedQuantity(new BigDecimal(quantity));
        reqVO.setReason(reason);
        return reqVO;
    }

    private PurchaseOrderExceptionReviewReqVO reviewReq(Long id, boolean approved, String approvedQuantity,
                                                        LocalDate validUntil, String remark) {
        PurchaseOrderExceptionReviewReqVO reqVO = new PurchaseOrderExceptionReviewReqVO();
        reqVO.setId(id);
        reqVO.setApproved(approved);
        reqVO.setApprovedQuantity(approvedQuantity == null ? null : new BigDecimal(approvedQuantity));
        reqVO.setValidUntil(validUntil);
        reqVO.setReviewRemark(remark);
        return reqVO;
    }

}
