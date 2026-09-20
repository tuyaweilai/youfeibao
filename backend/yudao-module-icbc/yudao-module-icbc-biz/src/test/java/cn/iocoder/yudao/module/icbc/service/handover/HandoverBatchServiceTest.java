package cn.iocoder.yudao.module.icbc.service.handover;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchIntakeReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchUpdateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingAddReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingEffectiveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverIntakeCandidateRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcHandoverBatchDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcWeighingDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.handover.IcbcHandoverBatchMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.handover.IcbcWeighingMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.HandoverSourceTypeEnum;
import cn.iocoder.yudao.module.icbc.service.handover.impl.HandoverBatchServiceImpl;
import cn.iocoder.yudao.module.logistics.api.transport.dto.LogisticsTransportHandoverRespDTO;
import cn.iocoder.yudao.module.logistics.api.transport.LogisticsTransportApi;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * {@link HandoverBatchServiceImpl} 的单元测试（#50 T12）。
 *
 * <p>断言的是外部可观察行为：批次登记了什么、同一车同一天两次送货是否各归各、多次磅次里
 * 哪一次参与计量、以及「已产生收购单后不能再换有效磅次」这条不变量。
 */
@Import({HandoverBatchServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class HandoverBatchServiceTest extends BaseDbUnitTest {

    /**
     * 物流读取面是**跨模块 API**（V6 #73）：icbc 的单测上下文只扫本模块，所以用 @MockBean。
     * 默认返回空 / null，正好覆盖「到站收货没有现场交接」这条路。
     */
    @org.springframework.boot.test.mock.mockito.MockBean
    private LogisticsTransportApi logisticsTransportApi;

    @Resource
    private HandoverBatchService handoverBatchService;
    @Resource
    private IcbcHandoverBatchMapper handoverBatchMapper;
    @Resource
    private IcbcWeighingMapper weighingMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcStationMapper stationMapper;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;

    // ==================== 批次登记（AC1 / AC4） ====================

    @Test
    public void testCreateBatch_snapshotsCounterpartyStationSourceDriverAndPlate() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_A", "城东收货点");

        HandoverBatchCreateReqVO reqVO = baseReq(payee.getId());
        reqVO.setStationId(station.getId());
        reqVO.setSourceType(HandoverSourceTypeEnum.APPOINTMENT.getType());
        reqVO.setDriverName("李师傅");
        reqVO.setDriverMobile("13900139000");
        reqVO.setPlateNo("京A12345");
        reqVO.setOccurTime(LocalDateTime.now().minusHours(1));

        Long id = handoverBatchService.createBatch(reqVO);

        IcbcHandoverBatchDO saved = handoverBatchMapper.selectById(id);
        assertNotNull(saved);
        assertTrue(saved.getBatchNo().startsWith("HB"));
        assertEquals(payee.getId(), saved.getPayeeId());
        assertEquals("张三", saved.getSellerName());
        assertEquals("13800138000", saved.getSellerMobile());
        assertEquals(station.getId(), saved.getStationId());
        assertEquals("城东收货点", saved.getStationName());
        assertEquals("APPOINTMENT", saved.getSourceType());
        assertEquals("李师傅", saved.getDriverName());
        assertEquals("京A12345", saved.getPlateNo());
        assertNotNull(saved.getOccurTime());

        HandoverBatchRespVO resp = handoverBatchService.getBatch(id);
        assertEquals("预约到站", resp.getSourceTypeName());
        assertEquals(0L, resp.getAcquisitionCount());
        assertFalse(resp.getWeighingChangeLocked());
        assertTrue(resp.getWeighingList().isEmpty());
    }

    @Test
    public void testCreateBatch_withoutAppointmentAndWithoutPurchaseOrder_allowed() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_A", "城东收货点");

        // AC4：临时上门的散户不被流程挡住——没有预约、没有采购订单也能建批次
        HandoverBatchCreateReqVO reqVO = baseReq(payee.getId());
        reqVO.setStationId(station.getId());
        Long id = handoverBatchService.createBatch(reqVO);

        IcbcHandoverBatchDO saved = handoverBatchMapper.selectById(id);
        assertNull(saved.getAppointmentId());
        assertNull(saved.getPurchaseOrderId());
        // 不填来源方式时默认「直接到场」
        assertEquals(HandoverSourceTypeEnum.WALK_IN.getType(), saved.getSourceType());
    }

    @Test
    public void testCreateBatch_onSiteAddressInsteadOfStation() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");

        HandoverBatchCreateReqVO reqVO = baseReq(payee.getId());
        reqVO.setVisitAddress("北京市朝阳区某某路 1 号");
        reqVO.setSourceType(HandoverSourceTypeEnum.ON_SITE.getType());
        Long id = handoverBatchService.createBatch(reqVO);

        IcbcHandoverBatchDO saved = handoverBatchMapper.selectById(id);
        assertNull(saved.getStationId());
        assertEquals("北京市朝阳区某某路 1 号", saved.getVisitAddress());
    }

    @Test
    public void testCreateBatch_missingLocationOrPlateOrUnknownPayeeOrBadSource_rejected() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_A", "城东收货点");

        HandoverBatchCreateReqVO noLocation = baseReq(payee.getId());
        assertServiceException(() -> handoverBatchService.createBatch(noLocation),
                HANDOVER_BATCH_LOCATION_REQUIRED);

        HandoverBatchCreateReqVO noPlate = baseReq(payee.getId());
        noPlate.setStationId(station.getId());
        noPlate.setPlateNo("  ");
        assertServiceException(() -> handoverBatchService.createBatch(noPlate),
                HANDOVER_BATCH_PLATE_REQUIRED);

        HandoverBatchCreateReqVO unknownPayee = baseReq(payee.getId());
        unknownPayee.setStationId(station.getId());
        unknownPayee.setPayeeId(payee.getId() + 100);
        assertServiceException(() -> handoverBatchService.createBatch(unknownPayee),
                HANDOVER_BATCH_PAYEE_REQUIRED);

        HandoverBatchCreateReqVO badSource = baseReq(payee.getId());
        badSource.setStationId(station.getId());
        badSource.setSourceType("TELEPORT");
        assertServiceException(() -> handoverBatchService.createBatch(badSource),
                HANDOVER_SOURCE_TYPE_INVALID, "TELEPORT");

        HandoverBatchCreateReqVO unknownStation = baseReq(payee.getId());
        unknownStation.setStationId(9999L);
        assertServiceException(() -> handoverBatchService.createBatch(unknownStation),
                STATION_NOT_EXISTS);
    }

    @Test
    public void testUpdateBatch_backfillsDriverAndPlate_butKeepsCounterparty() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_A", "城东收货点");
        HandoverBatchCreateReqVO reqVO = baseReq(payee.getId());
        reqVO.setStationId(station.getId());
        Long id = handoverBatchService.createBatch(reqVO);

        HandoverBatchUpdateReqVO update = new HandoverBatchUpdateReqVO();
        update.setId(id);
        update.setPlateNo("京B54321");
        update.setDriverName("王师傅");
        update.setSourceType(HandoverSourceTypeEnum.ON_SITE.getType());
        update.setVisitAddress("上门地点：某某村 3 号");
        handoverBatchService.updateBatch(update);

        IcbcHandoverBatchDO saved = handoverBatchMapper.selectById(id);
        assertEquals("京B54321", saved.getPlateNo());
        assertEquals("王师傅", saved.getDriverName());
        // 补录不碰交易对方
        assertEquals(payee.getId(), saved.getPayeeId());
        assertEquals("上门地点：某某村 3 号", saved.getVisitAddress());
    }

    @Test
    public void testGetPage_filterByPlateAndSource() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_A", "城东收货点");
        Long first = createBatch(payee.getId(), station.getId(), "京A12345", HandoverSourceTypeEnum.WALK_IN);
        createBatch(payee.getId(), station.getId(), "京B54321", HandoverSourceTypeEnum.APPOINTMENT);

        HandoverBatchPageReqVO reqVO = new HandoverBatchPageReqVO();
        reqVO.setPlateNo("A123");
        PageResult<HandoverBatchRespVO> page = handoverBatchService.getPage(reqVO);
        assertEquals(1, page.getTotal());
        assertEquals(first, page.getList().get(0).getId());
        assertEquals("直接到场", page.getList().get(0).getSourceTypeName());
    }

    // ==================== 磅次与有效磅次（AC2） ====================

    @Test
    public void testFirstWeighingBecomesEffective_thenSelectSecond() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_A", "城东收货点");
        Long batchId = createBatch(payee.getId(), station.getId(), "京A12345", HandoverSourceTypeEnum.WALK_IN);

        Long first = addWeighing(batchId, "18000", "5500", "WD001");
        // 第一次磅次自动成为有效磅次：现场只有一次过磅时不必多点一步
        IcbcWeighingDO firstSaved = weighingMapper.selectById(first);
        assertEquals(1, firstSaved.getSeqNo());
        assertTrue(firstSaved.getEffective());
        assertEquals(0, new BigDecimal("12500.0000").compareTo(firstSaved.getNetWeight()));

        // 复磅：第二次磅次留档，不自动抢有效位
        Long second = addWeighing(batchId, "18100", "5500", "WD002");
        assertEquals(2, weighingMapper.selectById(second).getSeqNo());
        assertFalse(weighingMapper.selectById(second).getEffective());
        assertEquals(first, handoverBatchService.getEffectiveWeighing(batchId).getId());

        // 显式指定第二次有效：第一次留档不参与
        HandoverWeighingEffectiveReqVO effectiveReq = new HandoverWeighingEffectiveReqVO();
        effectiveReq.setBatchId(batchId);
        effectiveReq.setWeighingId(second);
        effectiveReq.setReason("复磅后以第二次为准");
        handoverBatchService.selectEffectiveWeighing(effectiveReq);

        assertFalse(weighingMapper.selectById(first).getEffective());
        assertTrue(weighingMapper.selectById(second).getEffective());
        assertEquals(second, handoverBatchService.getEffectiveWeighing(batchId).getId());

        // 留档的那些仍在：不参与计量不等于删掉
        List<HandoverWeighingRespVO> weighings = handoverBatchService.listWeighings(batchId);
        assertEquals(2, weighings.size());
        assertEquals("留档不参与", weighings.get(0).getEffectiveText());
        assertEquals("参与计量", weighings.get(1).getEffectiveText());

        HandoverBatchRespVO resp = handoverBatchService.getBatch(batchId);
        assertEquals(second, resp.getEffectiveWeighingId());
        assertEquals(2, resp.getEffectiveWeighingSeqNo());
    }

    @Test
    public void testSelectEffective_foreignOrUnknownWeighingOrNoWeighing_rejected() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_A", "城东收货点");
        Long batchA = createBatch(payee.getId(), station.getId(), "京A12345", HandoverSourceTypeEnum.WALK_IN);
        Long batchB = createBatch(payee.getId(), station.getId(), "京B54321", HandoverSourceTypeEnum.WALK_IN);
        Long weighingOfB = addWeighing(batchB, "18000", "5500", "WD-B");

        // 别的批次的磅次不能拿到本批次来指定
        HandoverWeighingEffectiveReqVO foreign = new HandoverWeighingEffectiveReqVO();
        foreign.setBatchId(batchA);
        foreign.setWeighingId(weighingOfB);
        assertServiceException(() -> handoverBatchService.selectEffectiveWeighing(foreign),
                WEIGHING_NOT_IN_BATCH);

        HandoverWeighingEffectiveReqVO unknown = new HandoverWeighingEffectiveReqVO();
        unknown.setBatchId(batchA);
        unknown.setWeighingId(9999L);
        assertServiceException(() -> handoverBatchService.selectEffectiveWeighing(unknown),
                WEIGHING_NOT_EXISTS);

        // 一次磅次都没有的批次：没有有效磅次，计量依据不明（调用方必须拦住）
        assertNull(handoverBatchService.getEffectiveWeighing(batchA));
    }

    @Test
    public void testSelectEffective_lockedOnceAcquisitionExists() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_A", "城东收货点");
        Long batchId = createBatch(payee.getId(), station.getId(), "京A12345", HandoverSourceTypeEnum.WALK_IN);
        Long first = addWeighing(batchId, "18000", "5500", "WD001");
        Long second = addWeighing(batchId, "18100", "5500", "WD002");

        // 模拟这台批次已产生一张收购单（计量结果已引用第一次磅次的值与版本）
        IcbcAcquisitionDO acquisition = new IcbcAcquisitionDO();
        acquisition.setAcquisitionNo("ACQ_TEST_" + System.nanoTime());
        acquisition.setPayeeId(payee.getId());
        acquisition.setHandoverBatchId(batchId);
        acquisition.setWeighingId(first);
        acquisition.setWeighingSeqNo(1);
        acquisition.setStatus(AcquisitionStatusEnum.REGISTERED.getStatus());
        acquisitionMapper.insert(acquisition);

        HandoverWeighingEffectiveReqVO reqVO = new HandoverWeighingEffectiveReqVO();
        reqVO.setBatchId(batchId);
        reqVO.setWeighingId(second);
        assertServiceException(() -> handoverBatchService.selectEffectiveWeighing(reqVO),
                WEIGHING_BATCH_IN_USE);
        // 锁定后有效磅次不变
        assertEquals(first, handoverBatchService.getEffectiveWeighing(batchId).getId());
        assertTrue(handoverBatchService.getBatch(batchId).getWeighingChangeLocked());
        assertEquals(1L, handoverBatchService.countAcquisitions(batchId));

        // 作废后这笔计量不再成立：有效磅次可以重新指定（单据仍保留，作废原因对人可见）
        IcbcAcquisitionDO cancelled = new IcbcAcquisitionDO();
        cancelled.setId(acquisition.getId());
        cancelled.setStatus(AcquisitionStatusEnum.CANCELLED.getStatus());
        acquisitionMapper.updateById(cancelled);
        handoverBatchService.selectEffectiveWeighing(reqVO);
        assertEquals(second, handoverBatchService.getEffectiveWeighing(batchId).getId());
        assertEquals(0L, handoverBatchService.countAcquisitions(batchId));
        assertFalse(handoverBatchService.getBatch(batchId).getWeighingChangeLocked());
    }

    @Test
    public void testAddWeighing_negativeOrTareGreaterThanGross_rejected() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_A", "城东收货点");
        Long batchId = createBatch(payee.getId(), station.getId(), "京A12345", HandoverSourceTypeEnum.WALK_IN);

        HandoverWeighingAddReqVO negative = new HandoverWeighingAddReqVO();
        negative.setBatchId(batchId);
        negative.setGrossWeight(new BigDecimal("-1"));
        negative.setTareWeight(new BigDecimal("0"));
        assertServiceException(() -> handoverBatchService.addWeighing(negative), WEIGHING_WEIGHT_INVALID,
                "毛重与皮重不能为负");

        HandoverWeighingAddReqVO tareGreater = new HandoverWeighingAddReqVO();
        tareGreater.setBatchId(batchId);
        tareGreater.setGrossWeight(new BigDecimal("5000"));
        tareGreater.setTareWeight(new BigDecimal("5500"));
        assertServiceException(() -> handoverBatchService.addWeighing(tareGreater), WEIGHING_WEIGHT_INVALID,
                "皮重 5500 大于毛重 5000");

        // 一次都没落库
        assertTrue(handoverBatchService.listWeighings(batchId).isEmpty());
        // 批次不存在时也不能挂磅次
        HandoverWeighingAddReqVO unknownBatch = new HandoverWeighingAddReqVO();
        unknownBatch.setBatchId(9999L);
        unknownBatch.setGrossWeight(new BigDecimal("5000"));
        unknownBatch.setTareWeight(new BigDecimal("0"));
        assertServiceException(() -> handoverBatchService.addWeighing(unknownBatch),
                HANDOVER_BATCH_NOT_EXISTS);
    }

    @Test
    public void testTwoBatchesSameVehicleSameDay_areSeparateBatches() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_A", "城东收货点");

        // AC3：同一车同一天两次送货 = 两个批次（不做车牌 + 日期的去重）
        Long morning = createBatch(payee.getId(), station.getId(), "京A12345", HandoverSourceTypeEnum.WALK_IN);
        Long afternoon = createBatch(payee.getId(), station.getId(), "京A12345", HandoverSourceTypeEnum.WALK_IN);
        assertNotEquals(morning, afternoon);
        assertNotEquals(handoverBatchMapper.selectById(morning).getBatchNo(),
                handoverBatchMapper.selectById(afternoon).getBatchNo());

        Long morningWeighing = addWeighing(morning, "18000", "5500", "WD-AM");
        Long afternoonWeighing = addWeighing(afternoon, "9000", "3000", "WD-PM");

        // 磅单与有效磅次各归各：早上那批指定有效不影响下午那批
        assertEquals(morningWeighing, handoverBatchService.getEffectiveWeighing(morning).getId());
        assertEquals(afternoonWeighing, handoverBatchService.getEffectiveWeighing(afternoon).getId());
        assertEquals("WD-AM", handoverBatchService.getEffectiveWeighing(morning).getWeightTicketNo());
        assertEquals("WD-PM", handoverBatchService.getEffectiveWeighing(afternoon).getWeightTicketNo());
        assertEquals(1, handoverBatchService.listWeighings(morning).size());
        assertEquals(1, handoverBatchService.listWeighings(afternoon).size());
    }

    // ==================== 辅助方法 ====================


    // ==================== 现场交接登记 → 回场复磅（V6 #73） ====================

    @Test
    public void testIntakeFromHandover_createsBatchWithReferenceAndDocumentStatus_andFirstWeighing() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_INTAKE", "城东收货点");
        when(logisticsTransportApi.getHandover(3072L)).thenReturn(
                handoverDto(3072L, payee.getId(), "PENDING", "缺身份证"));

        HandoverBatchIntakeReqVO reqVO = intakeReq(3072L, station.getId());

        Long batchId = handoverBatchService.intakeFromHandover(reqVO);

        IcbcHandoverBatchDO batch = handoverBatchMapper.selectById(batchId);
        // 与物流侧现场交接登记唯一挂接
        assertEquals(3072L, batch.getLogisticsHandoverId());
        // 上门回收：场站是**归属场站**，实际提货地址存 visitAddress（ADR 0031）
        assertEquals(station.getId(), batch.getStationId());
        assertEquals("城东收货点", batch.getStationName());
        assertEquals(HandoverSourceTypeEnum.ON_SITE.getType(), batch.getSourceType());
        assertEquals("某某路 1 号", batch.getVisitAddress());
        assertEquals(payee.getId(), batch.getPayeeId());
        // 司机与车辆：引用 + 快照并存（ADR 0032 第 7 条）
        assertEquals(77L, batch.getDriverId());
        assertEquals("李师傅", batch.getDriverName());
        assertEquals(88L, batch.getVehicleId());
        assertEquals("京A12345", batch.getPlateNo());
        // 要件状态与现场参考值快照
        assertEquals("PENDING", batch.getDocumentStatus());
        assertEquals("缺身份证", batch.getDocumentGap());
        assertEquals(0, new BigDecimal("12.5").compareTo(batch.getReferenceQuantity()));
        assertEquals(0, new BigDecimal("2600.00").compareTo(batch.getReferenceUnitPrice()));

        // 第一次磅次自动成为有效磅次（回场复磅的读数）
        IcbcWeighingDO weighing = weighingMapper.selectEffectiveByBatchId(batchId);
        assertNotNull(weighing);
        assertEquals(0, new BigDecimal("12500").compareTo(weighing.getNetWeight()));

        // 磅房在批次详情里看得到现场参考量与照片凭证
        HandoverBatchRespVO resp = handoverBatchService.getBatch(batchId);
        assertEquals("待补档", resp.getDocumentStatusName());
        assertEquals(0, new BigDecimal("12.5").compareTo(resp.getReferenceQuantity()));
        assertEquals(List.of("https://file/handover.jpg"), resp.getReferencePhotos());
        assertEquals(1L, resp.getEffectiveWeighingSeqNo().longValue());
    }

    @Test
    public void testIntakeFromHandover_isIdempotent() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_INTAKE", "城东收货点");
        when(logisticsTransportApi.getHandover(3072L)).thenReturn(
                handoverDto(3072L, payee.getId(), "COMPLETE", null));
        HandoverBatchIntakeReqVO reqVO = intakeReq(3072L, station.getId());

        Long first = handoverBatchService.intakeFromHandover(reqVO);
        Long second = handoverBatchService.intakeFromHandover(reqVO);

        assertEquals(first, second, "同一个现场交接登记只会建出一个批次");
        assertEquals(1, weighingMapper.selectListByBatchId(first).size(), "重复复磅不产生第二条磅次");
    }

    @Test
    public void testIntakeFromHandover_withoutStation_isRejected() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        when(logisticsTransportApi.getHandover(3072L)).thenReturn(
                handoverDto(3072L, payee.getId(), "COMPLETE", null));
        HandoverBatchIntakeReqVO reqVO = intakeReq(3072L, null);

        // 上门提货也要有归属场站：不用上门地址顶替，也不引入虚拟场站
        assertServiceException(() -> handoverBatchService.intakeFromHandover(reqVO),
                HANDOVER_INTAKE_STATION_REQUIRED);
    }

    @Test
    public void testIntakeFromHandover_unknownHandoverOrUnknownPayee_isRejected() {
        IcbcStationDO station = insertStation("ST_INTAKE", "城东收货点");
        // 物流侧查不到这条现场交接（伪编号 / 已被清）
        when(logisticsTransportApi.getHandover(9999L)).thenReturn(null);
        assertServiceException(() -> handoverBatchService.intakeFromHandover(intakeReq(9999L, station.getId())),
                HANDOVER_LOGISTICS_HANDOVER_NOT_EXISTS);

        // 现场登记里的出售者在本租户查不到（跨租户 / 已删）：不能凭空建批次
        when(logisticsTransportApi.getHandover(3072L)).thenReturn(
                handoverDto(3072L, 424242L, "COMPLETE", null));
        assertServiceException(() -> handoverBatchService.intakeFromHandover(intakeReq(3072L, station.getId())),
                HANDOVER_BATCH_PAYEE_REQUIRED);
    }

    @Test
    public void testGetPendingIntakeList_excludesHandoversAlreadyIntaken() {
        PayeeInfoDO payee = insertPayee("张三", "13800138000");
        IcbcStationDO station = insertStation("ST_INTAKE", "城东收货点");
        when(logisticsTransportApi.getRecentHandoverList()).thenReturn(List.of(
                handoverDto(3071L, payee.getId(), "COMPLETE", null),
                handoverDto(3072L, payee.getId(), "PENDING", "缺银行卡")));
        when(logisticsTransportApi.getHandover(3072L)).thenReturn(
                handoverDto(3072L, payee.getId(), "PENDING", "缺银行卡"));

        // 先复磅 3072：它就不再是候选
        handoverBatchService.intakeFromHandover(intakeReq(3072L, station.getId()));

        List<HandoverIntakeCandidateRespVO> pending = handoverBatchService.getPendingIntakeList();
        assertEquals(1, pending.size());
        assertEquals(3071L, pending.get(0).getLogisticsHandoverId());
        // 候选里带现场参考量与照片凭证，磅房对得上现场谈的事
        assertEquals(0, new BigDecimal("12.5").compareTo(pending.get(0).getReferenceQuantity()));
        assertEquals(1, pending.get(0).getPhotos().size());
    }

    private HandoverBatchIntakeReqVO intakeReq(Long logisticsHandoverId, Long stationId) {
        HandoverBatchIntakeReqVO reqVO = new HandoverBatchIntakeReqVO();
        reqVO.setLogisticsHandoverId(logisticsHandoverId);
        reqVO.setStationId(stationId);
        reqVO.setGrossWeight(new BigDecimal("18000"));
        reqVO.setTareWeight(new BigDecimal("5500"));
        reqVO.setWeightTicketNo("WD-INTAKE");
        return reqVO;
    }

    private LogisticsTransportHandoverRespDTO handoverDto(Long id, Long payeeId, String documentStatus,
                                                          String documentGap) {
        LogisticsTransportHandoverRespDTO dto = new LogisticsTransportHandoverRespDTO();
        dto.setId(id);
        dto.setHandoverNo("HO" + id);
        dto.setTaskId(55L);
        dto.setTaskNo("TASK-55");
        dto.setStopId(66L);
        dto.setAddress("某某路 1 号");
        dto.setPayeeId(payeeId);
        dto.setPayeeName("张三");
        dto.setPayeeMobile("13800138000");
        dto.setGoodsConfigId(2048L);
        dto.setCategoryName("废钢铁");
        dto.setUnit("吨");
        dto.setReferenceQuantity(new BigDecimal("12.5"));
        dto.setReferenceUnitPrice(new BigDecimal("2600.00"));
        dto.setPhotos(new java.util.ArrayList<>(List.of("https://file/handover.jpg")));
        dto.setDriverId(77L);
        dto.setDriverName("李师傅");
        dto.setVehicleId(88L);
        dto.setPlateNo("京A12345");
        dto.setOccurTime(LocalDateTime.now().minusHours(2));
        dto.setDocumentStatus(documentStatus);
        dto.setDocumentStatusName("PENDING".equals(documentStatus) ? "待补档" : "已齐");
        dto.setDocumentGap(documentGap);
        return dto;
    }

    private HandoverBatchCreateReqVO baseReq(Long payeeId) {
        HandoverBatchCreateReqVO reqVO = new HandoverBatchCreateReqVO();
        reqVO.setPayeeId(payeeId);
        reqVO.setPlateNo("京A12345");
        return reqVO;
    }

    private Long createBatch(Long payeeId, Long stationId, String plateNo,
                             HandoverSourceTypeEnum sourceType) {
        HandoverBatchCreateReqVO reqVO = new HandoverBatchCreateReqVO();
        reqVO.setPayeeId(payeeId);
        reqVO.setStationId(stationId);
        reqVO.setPlateNo(plateNo);
        reqVO.setSourceType(sourceType.getType());
        return handoverBatchService.createBatch(reqVO);
    }

    private Long addWeighing(Long batchId, String gross, String tare, String ticketNo) {
        HandoverWeighingAddReqVO reqVO = new HandoverWeighingAddReqVO();
        reqVO.setBatchId(batchId);
        reqVO.setGrossWeight(new BigDecimal(gross));
        reqVO.setTareWeight(new BigDecimal(tare));
        reqVO.setWeightTicketNo(ticketNo);
        reqVO.setPlateNo("京A12345");
        return handoverBatchService.addWeighing(reqVO);
    }

    private PayeeInfoDO insertPayee(String name, String mobile) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId("PARTNER_" + System.nanoTime())
                .name(name)
                .mobile(mobile)
                .idCardNo("110101199001011234")
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private IcbcStationDO insertStation(String code, String name) {
        IcbcStationDO station = IcbcStationDO.builder().stationCode(code).name(name).openStatus(1).build();
        stationMapper.insert(station);
        return station;
    }

}
