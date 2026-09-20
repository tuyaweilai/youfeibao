package cn.iocoder.yudao.module.icbc.service.handover.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchUpdateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingAddReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingEffectiveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.appointment.IcbcAppointmentDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcHandoverBatchDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcWeighingDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.appointment.IcbcAppointmentMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.handover.IcbcHandoverBatchMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.handover.IcbcWeighingMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.enums.HandoverSourceTypeEnum;
import cn.iocoder.yudao.module.icbc.service.handover.HandoverBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 交接批次与有效磅次 Service 实现（#50 T12）。
 *
 * <p>显式 bean 名：{@code yudao-server} 里 {@code yudao-module-waste} 也有一个同名的
 * {@code HandoverBatchServiceImpl}（默认 bean 名会撞车导致整机起不来）。
 */
@Service("icbcHandoverBatchServiceImpl")
@Validated
@Slf4j
public class HandoverBatchServiceImpl implements HandoverBatchService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private IcbcHandoverBatchMapper icbcHandoverBatchMapper;
    @Resource
    private IcbcWeighingMapper icbcWeighingMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcStationMapper icbcStationMapper;
    @Resource
    private IcbcAppointmentMapper icbcAppointmentMapper;
    @Resource
    private IcbcAcquisitionMapper icbcAcquisitionMapper;

    // ==================== 批次 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBatch(@Valid HandoverBatchCreateReqVO reqVO) {
        PayeeInfoDO payee = reqVO.getPayeeId() == null ? null : payeeInfoMapper.selectById(reqVO.getPayeeId());
        if (payee == null) {
            throw exception(HANDOVER_BATCH_PAYEE_REQUIRED);
        }
        assertLocationPresent(reqVO.getStationId(), reqVO.getVisitAddress());
        if (StrUtil.isBlank(reqVO.getPlateNo())) {
            throw exception(HANDOVER_BATCH_PLATE_REQUIRED);
        }
        HandoverSourceTypeEnum sourceType = resolveSourceType(reqVO.getSourceType());
        String stationName = resolveStationName(reqVO.getStationId());
        // 预约只是可选关联：没有预约照样能建批次（临时上门的散户不被流程挡住）
        if (reqVO.getAppointmentId() != null) {
            IcbcAppointmentDO appointment = icbcAppointmentMapper.selectById(reqVO.getAppointmentId());
            if (appointment == null) {
                throw exception(APPOINTMENT_NOT_EXISTS);
            }
        }

        IcbcHandoverBatchDO batch = IcbcHandoverBatchDO.builder()
                .batchNo(generateBatchNo())
                .payeeId(payee.getId())
                .sellerName(payee.getName())
                .sellerMobile(payee.getMobile())
                .stationId(reqVO.getStationId())
                .stationName(stationName)
                .visitAddress(StrUtil.trim(reqVO.getVisitAddress()))
                .occurTime(reqVO.getOccurTime() == null ? LocalDateTime.now() : reqVO.getOccurTime())
                .sourceType(sourceType.getType())
                .driverName(StrUtil.trim(reqVO.getDriverName()))
                .driverMobile(StrUtil.trim(reqVO.getDriverMobile()))
                .plateNo(StrUtil.trim(reqVO.getPlateNo()))
                .appointmentId(reqVO.getAppointmentId())
                .purchaseOrderId(reqVO.getPurchaseOrderId())
                .remark(reqVO.getRemark())
                .build();
        icbcHandoverBatchMapper.insert(batch);
        log.info("交接批次登记成功 - batchNo: {}, payeeId: {}, plate: {}, sourceType: {}",
                batch.getBatchNo(), batch.getPayeeId(), batch.getPlateNo(), batch.getSourceType());
        return batch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatch(@Valid HandoverBatchUpdateReqVO reqVO) {
        IcbcHandoverBatchDO batch = getBatchDO(reqVO.getId());
        IcbcHandoverBatchDO update = new IcbcHandoverBatchDO();
        update.setId(batch.getId());
        if (reqVO.getStationId() != null) {
            update.setStationId(reqVO.getStationId());
            update.setStationName(resolveStationName(reqVO.getStationId()));
        }
        if (reqVO.getVisitAddress() != null) {
            update.setVisitAddress(StrUtil.trim(reqVO.getVisitAddress()));
        }
        Long stationId = reqVO.getStationId() != null ? reqVO.getStationId() : batch.getStationId();
        String visitAddress = reqVO.getVisitAddress() != null ? reqVO.getVisitAddress() : batch.getVisitAddress();
        assertLocationPresent(stationId, visitAddress);
        if (reqVO.getPlateNo() != null) {
            if (StrUtil.isBlank(reqVO.getPlateNo())) {
                throw exception(HANDOVER_BATCH_PLATE_REQUIRED);
            }
            update.setPlateNo(StrUtil.trim(reqVO.getPlateNo()));
        }
        if (reqVO.getOccurTime() != null) {
            update.setOccurTime(reqVO.getOccurTime());
        }
        if (StrUtil.isNotBlank(reqVO.getSourceType())) {
            update.setSourceType(resolveSourceType(reqVO.getSourceType()).getType());
        }
        if (reqVO.getDriverName() != null) {
            update.setDriverName(StrUtil.trim(reqVO.getDriverName()));
        }
        if (reqVO.getDriverMobile() != null) {
            update.setDriverMobile(StrUtil.trim(reqVO.getDriverMobile()));
        }
        if (reqVO.getRemark() != null) {
            update.setRemark(reqVO.getRemark());
        }
        icbcHandoverBatchMapper.updateById(update);
    }

    @Override
    public HandoverBatchRespVO getBatch(Long id) {
        return toResp(getBatchDO(id), true);
    }

    @Override
    public PageResult<HandoverBatchRespVO> getPage(@Valid HandoverBatchPageReqVO reqVO) {
        PageResult<IcbcHandoverBatchDO> page = icbcHandoverBatchMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(batch -> toResp(batch, false)).toList(),
                page.getTotal());
    }

    // ==================== 磅次 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addWeighing(@Valid HandoverWeighingAddReqVO reqVO) {
        IcbcHandoverBatchDO batch = getBatchDO(reqVO.getBatchId());
        assertWeightsValid(reqVO.getGrossWeight(), reqVO.getTareWeight());
        int seqNo = icbcWeighingMapper.selectCountByBatchId(batch.getId()).intValue() + 1;
        IcbcWeighingDO weighing = IcbcWeighingDO.builder()
                .batchId(batch.getId())
                .seqNo(seqNo)
                .grossWeight(reqVO.getGrossWeight())
                .tareWeight(reqVO.getTareWeight())
                .netWeight(reqVO.getGrossWeight().subtract(reqVO.getTareWeight()))
                .weighTime(reqVO.getWeighTime() == null ? LocalDateTime.now() : reqVO.getWeighTime())
                .weightTicketNo(StrUtil.trim(reqVO.getWeightTicketNo()))
                .weightTicketImageUrl(reqVO.getWeightTicketImageUrl())
                .plateNo(StrUtil.trim(reqVO.getPlateNo()))
                // 第一次磅次自动成为有效磅次：现场只有一次磅次时不必多一步点击
                .effective(seqNo == 1)
                .remark(reqVO.getRemark())
                .build();
        icbcWeighingMapper.insert(weighing);
        return weighing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void selectEffectiveWeighing(@Valid HandoverWeighingEffectiveReqVO reqVO) {
        IcbcHandoverBatchDO batch = getBatchDO(reqVO.getBatchId());
        IcbcWeighingDO weighing = reqVO.getWeighingId() == null
                ? null : icbcWeighingMapper.selectById(reqVO.getWeighingId());
        if (weighing == null) {
            throw exception(WEIGHING_NOT_EXISTS);
        }
        if (!Objects.equals(weighing.getBatchId(), batch.getId())) {
            throw exception(WEIGHING_NOT_IN_BATCH);
        }
        // 计量结果引用的是有效磅次的值与版本：已产生收购单就不能再换那一次
        if (countAcquisitions(batch.getId()) > 0) {
            throw exception(WEIGHING_BATCH_IN_USE);
        }
        icbcWeighingMapper.clearEffective(batch.getId(), weighing.getId());
        IcbcWeighingDO update = new IcbcWeighingDO();
        update.setId(weighing.getId());
        update.setEffective(true);
        if (StrUtil.isNotBlank(reqVO.getReason())) {
            update.setRemark(StrUtil.isBlank(weighing.getRemark())
                    ? reqVO.getReason() : weighing.getRemark() + "；" + reqVO.getReason());
        }
        icbcWeighingMapper.updateById(update);
    }

    @Override
    public List<HandoverWeighingRespVO> listWeighings(Long batchId) {
        getBatchDO(batchId);
        return icbcWeighingMapper.selectListByBatchId(batchId).stream().map(this::toWeighingResp).toList();
    }

    // ==================== 供收购登记使用 ====================

    @Override
    public IcbcHandoverBatchDO getBatchDO(Long id) {
        IcbcHandoverBatchDO batch = id == null ? null : icbcHandoverBatchMapper.selectById(id);
        if (batch == null) {
            throw exception(HANDOVER_BATCH_NOT_EXISTS);
        }
        return batch;
    }

    @Override
    public IcbcWeighingDO getEffectiveWeighing(Long batchId) {
        return icbcWeighingMapper.selectEffectiveByBatchId(batchId);
    }

    @Override
    public Long countAcquisitions(Long batchId) {
        return (long) icbcAcquisitionMapper.selectListByHandoverBatchId(batchId).size();
    }

    // ==================== 内部方法 ====================

    private void assertLocationPresent(Long stationId, String visitAddress) {
        if (stationId == null && StrUtil.isBlank(visitAddress)) {
            throw exception(HANDOVER_BATCH_LOCATION_REQUIRED);
        }
    }

    private String resolveStationName(Long stationId) {
        if (stationId == null) {
            return null;
        }
        IcbcStationDO station = icbcStationMapper.selectById(stationId);
        if (station == null) {
            throw exception(STATION_NOT_EXISTS);
        }
        return station.getName();
    }

    private HandoverSourceTypeEnum resolveSourceType(String sourceType) {
        if (StrUtil.isBlank(sourceType)) {
            return HandoverSourceTypeEnum.WALK_IN;
        }
        return HandoverSourceTypeEnum.ofType(sourceType)
                .orElseThrow(() -> exception(HANDOVER_SOURCE_TYPE_INVALID, sourceType));
    }

    /**
     * 明显错误进不了库（用户故事 28 的一部分）：负重量、皮重大于毛重在这里拦下。
     */
    private void assertWeightsValid(BigDecimal grossWeight, BigDecimal tareWeight) {
        if (grossWeight == null || tareWeight == null) {
            throw exception(WEIGHING_WEIGHT_INVALID, "毛重与皮重都要填");
        }
        if (grossWeight.signum() < 0 || tareWeight.signum() < 0) {
            throw exception(WEIGHING_WEIGHT_INVALID, "毛重与皮重不能为负");
        }
        if (grossWeight.compareTo(tareWeight) < 0) {
            throw exception(WEIGHING_WEIGHT_INVALID,
                    "皮重 " + tareWeight.stripTrailingZeros().toPlainString()
                            + " 大于毛重 " + grossWeight.stripTrailingZeros().toPlainString());
        }
    }

    private HandoverBatchRespVO toResp(IcbcHandoverBatchDO batch, boolean withWeighings) {
        HandoverBatchRespVO resp = BeanUtils.toBean(batch, HandoverBatchRespVO.class);
        HandoverSourceTypeEnum.ofType(batch.getSourceType())
                .ifPresent(source -> resp.setSourceTypeName(source.getName()));
        Long acquisitionCount = countAcquisitions(batch.getId());
        resp.setAcquisitionCount(acquisitionCount);
        resp.setWeighingChangeLocked(acquisitionCount > 0);
        List<IcbcWeighingDO> weighings = withWeighings
                ? icbcWeighingMapper.selectListByBatchId(batch.getId()) : Collections.emptyList();
        if (withWeighings) {
            resp.setWeighingList(weighings.stream().map(this::toWeighingResp).toList());
        }
        IcbcWeighingDO effective = weighings.stream()
                .filter(weighing -> Boolean.TRUE.equals(weighing.getEffective()))
                .findFirst()
                .orElseGet(() -> withWeighings ? null : icbcWeighingMapper.selectEffectiveByBatchId(batch.getId()));
        if (effective != null) {
            resp.setEffectiveWeighingId(effective.getId());
            resp.setEffectiveWeighingSeqNo(effective.getSeqNo());
        }
        return resp;
    }

    private HandoverWeighingRespVO toWeighingResp(IcbcWeighingDO weighing) {
        HandoverWeighingRespVO resp = BeanUtils.toBean(weighing, HandoverWeighingRespVO.class);
        resp.setEffectiveText(Boolean.TRUE.equals(weighing.getEffective()) ? "参与计量" : "留档不参与");
        return resp;
    }

    private String generateBatchNo() {
        return "HB" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

}
