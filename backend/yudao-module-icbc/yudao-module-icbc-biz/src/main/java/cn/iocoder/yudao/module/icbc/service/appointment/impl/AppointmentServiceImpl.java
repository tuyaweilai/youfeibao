package cn.iocoder.yudao.module.icbc.service.appointment.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentArriveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentNoShowReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentRespVO;
import cn.iocoder.yudao.module.icbc.controller.app.appointment.vo.AppointmentCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.appointment.vo.AppointmentCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.appointment.vo.AppointmentGoodsRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.appointment.IcbcAppointmentDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.appointment.IcbcAppointmentMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.enums.AppointmentStatusEnum;
import cn.iocoder.yudao.module.icbc.service.appointment.AppointmentService;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 到站预约 Service 实现（#35，ADR 0020）。
 *
 * <p>跨企业读取集中在 {@link #getListForSeller}，用显式的 {@code TenantUtils.executeIgnore} 表达；
 * 写入一律落在场站所属租户（他扫码那家企业），不跟着请求头猜。
 */
@Service
@Validated
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 口径说明：预约不是订单。三条都要说清楚，避免被当成承诺。 */
    private static final String SCOPE_NOTE = "预约不是订单：不占额度、不产生开票、不进五流；到场后仍由回收企业按实际过磅建收购单";

    @Resource
    private IcbcAppointmentMapper appointmentMapper;
    @Resource
    private IcbcStationMapper stationMapper;
    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private PayeeInfoService payeeInfoService;
    @Resource
    private IcbcGoodsConfigService goodsConfigService;
    @Resource
    private TenantApi tenantApi;

    // ==================== 自然人侧 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(@Valid AppointmentCreateReqVO reqVO) {
        assertBound(reqVO.getNaturalPersonId());
        if (StrUtil.isBlank(reqVO.getStationCode())) {
            throw exception(STATION_CODE_REQUIRED);
        }
        if (reqVO.getExpectedQuantity() != null && reqVO.getExpectedQuantity().signum() < 0) {
            throw exception(APPOINTMENT_EXPECTED_QUANTITY_INVALID);
        }
        if (reqVO.getExpectedArrivalTime() == null) {
            throw exception(APPOINTMENT_ARRIVAL_TIME_REQUIRED);
        }
        // 场站码全局唯一，但预约要落在**场站所属租户**：公开端点没有租户上下文，这里显式解析。
        IcbcStationDO station = TenantUtils.executeIgnore(
                () -> stationMapper.selectByStationCode(reqVO.getStationCode().trim()));
        if (station == null) {
            throw exception(STATION_NOT_EXISTS);
        }
        return TenantUtils.execute(station.getTenantId(), () -> doCreate(reqVO, station));
    }

    private Long doCreate(AppointmentCreateReqVO reqVO, IcbcStationDO station) {
        IcbcGoodsConfigDO goods = goodsConfigMapper.selectById(reqVO.getGoodsConfigId());
        if (goods == null) {
            throw exception(ACQUISITION_GOODS_CONFIG_NOT_EXISTS);
        }
        // 本租户已建档就直接挂上档案；没建档也允许预约（到站后再建档），不拦人
        PayeeInfoDO payee = payeeInfoMapper.selectByNaturalPersonId(reqVO.getNaturalPersonId());
        IcbcAppointmentDO appointment = IcbcAppointmentDO.builder()
                .appointmentNo(generateAppointmentNo())
                .naturalPersonId(reqVO.getNaturalPersonId())
                .payeeId(payee == null ? null : payee.getId())
                .sellerName(payee == null ? null : payee.getName())
                .stationId(station.getId())
                .stationCode(station.getStationCode())
                .stationName(station.getName())
                .goodsConfigId(goods.getId())
                .categoryName(goods.getName())
                .unit(goods.getUnit())
                .expectedQuantity(reqVO.getExpectedQuantity())
                .plateNo(StrUtil.trim(reqVO.getPlateNo()))
                .expectedArrivalTime(reqVO.getExpectedArrivalTime())
                .status(AppointmentStatusEnum.PENDING.getStatus())
                .remark(reqVO.getRemark())
                .build();
        appointmentMapper.insert(appointment);
        return appointment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(@Valid AppointmentCancelReqVO reqVO) {
        assertBound(reqVO.getNaturalPersonId());
        IcbcAppointmentDO appointment = loadOwnedBySeller(reqVO.getId(), reqVO.getNaturalPersonId());
        if (!AppointmentStatusEnum.PENDING.getStatus().equals(appointment.getStatus())) {
            throw exception(APPOINTMENT_NOT_CANCELLABLE);
        }
        IcbcAppointmentDO update = new IcbcAppointmentDO();
        update.setId(appointment.getId());
        update.setStatus(AppointmentStatusEnum.CANCELLED.getStatus());
        update.setCancelledAt(LocalDateTime.now());
        update.setCancelReason(StrUtil.blankToDefault(reqVO.getReason(), "出售者本人取消"));
        TenantUtils.executeIgnore(() -> appointmentMapper.updateById(update));
    }

    @Override
    public List<AppointmentRespVO> getListForSeller(Long naturalPersonId) {
        assertBound(naturalPersonId);
        // 他可能在多家回收企业都约过：跨企业读取只对本人开放（CONTEXT「交易可见性边界」）
        return TenantUtils.executeIgnore(() -> appointmentMapper.selectListByNaturalPersonId(naturalPersonId))
                .stream()
                .map(appointment -> toResp(appointment, enterpriseName(appointment.getTenantId())))
                .toList();
    }

    @Override
    public List<AppointmentGoodsRespVO> listEnabledGoods() {
        return goodsConfigService.getEnabledList().stream().map(goods -> {
            AppointmentGoodsRespVO vo = new AppointmentGoodsRespVO();
            vo.setId(goods.getId());
            vo.setName(goods.getName());
            vo.setUnit(goods.getUnit());
            return vo;
        }).toList();
    }

    // ==================== 现场 / 企业侧 ====================

    @Override
    public List<AppointmentRespVO> listPendingForPayee(Long payeeId) {
        if (payeeId == null) {
            return List.of();
        }
        PayeeInfoDO payee = payeeInfoMapper.selectById(payeeId);
        if (payee == null) {
            return List.of();
        }
        Long naturalPersonId = payeeInfoService.ensureNaturalPerson(payee).getId();
        String enterprise = enterpriseName(TenantContextHolder.getTenantId());
        return appointmentMapper.selectPendingByNaturalPersonId(naturalPersonId).stream()
                .map(appointment -> toResp(appointment, enterprise))
                .toList();
    }

    @Override
    public AppointmentRespVO getAppointment(Long id) {
        return toResp(getAppointmentDO(id), enterpriseName(TenantContextHolder.getTenantId()));
    }

    @Override
    public PageResult<AppointmentRespVO> getPage(AppointmentPageReqVO reqVO) {
        PageResult<IcbcAppointmentDO> page = appointmentMapper.selectPage(reqVO);
        String enterprise = enterpriseName(TenantContextHolder.getTenantId());
        return new PageResult<>(page.getList().stream()
                .map(appointment -> toResp(appointment, enterprise)).toList(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markArrived(@Valid AppointmentArriveReqVO reqVO) {
        IcbcAppointmentDO appointment = getAppointmentDO(reqVO.getId());
        if (AppointmentStatusEnum.ARRIVED.getStatus().equals(appointment.getStatus())) {
            // 幂等：重复标记不覆盖到场时间；但补挂收购单是有意义的
            if (reqVO.getAcquisitionId() != null
                    && !Objects.equals(appointment.getAcquisitionId(), reqVO.getAcquisitionId())) {
                IcbcAppointmentDO update = new IcbcAppointmentDO();
                update.setId(appointment.getId());
                update.setAcquisitionId(reqVO.getAcquisitionId());
                appointmentMapper.updateById(update);
            }
            return;
        }
        assertPending(appointment);
        IcbcAppointmentDO update = new IcbcAppointmentDO();
        update.setId(appointment.getId());
        update.setStatus(AppointmentStatusEnum.ARRIVED.getStatus());
        update.setArrivedAt(LocalDateTime.now());
        update.setAcquisitionId(reqVO.getAcquisitionId());
        appointmentMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markNoShow(@Valid AppointmentNoShowReqVO reqVO) {
        IcbcAppointmentDO appointment = getAppointmentDO(reqVO.getId());
        if (AppointmentStatusEnum.NO_SHOW.getStatus().equals(appointment.getStatus())) {
            return; // 幂等
        }
        assertPending(appointment);
        IcbcAppointmentDO update = new IcbcAppointmentDO();
        update.setId(appointment.getId());
        update.setStatus(AppointmentStatusEnum.NO_SHOW.getStatus());
        update.setNoShowReason(reqVO.getReason());
        appointmentMapper.updateById(update);
    }

    @Override
    public IcbcAppointmentDO getAppointmentDO(Long id) {
        IcbcAppointmentDO appointment = id == null ? null : appointmentMapper.selectById(id);
        if (appointment == null) {
            throw exception(APPOINTMENT_NOT_EXISTS);
        }
        return appointment;
    }

    // ==================== 内部方法 ====================

    private void assertPending(IcbcAppointmentDO appointment) {
        if (!AppointmentStatusEnum.PENDING.getStatus().equals(appointment.getStatus())) {
            throw exception(APPOINTMENT_STATUS_NOT_ALLOW, appointment.getAppointmentNo());
        }
    }

    /**
     * 按编号取预约并校验它确实属于该自然人：不属于就报「不存在」，不泄露别人的预约是否存在。
     */
    private IcbcAppointmentDO loadOwnedBySeller(Long id, Long naturalPersonId) {
        IcbcAppointmentDO appointment = TenantUtils.executeIgnore(
                () -> id == null ? null : appointmentMapper.selectById(id));
        if (appointment == null || !Objects.equals(appointment.getNaturalPersonId(), naturalPersonId)) {
            throw exception(APPOINTMENT_NOT_EXISTS);
        }
        return appointment;
    }

    private void assertBound(Long naturalPersonId) {
        if (naturalPersonId == null
                || !naturalPersonService.isBoundToLogin(naturalPersonId, SecurityFrameworkUtils.getLoginUserId())) {
            throw exception(NATURAL_PERSON_NOT_BOUND_TO_LOGIN);
        }
    }

    private AppointmentRespVO toResp(IcbcAppointmentDO appointment, String enterpriseName) {
        AppointmentRespVO resp = new AppointmentRespVO();
        resp.setId(appointment.getId());
        resp.setAppointmentNo(appointment.getAppointmentNo());
        resp.setNaturalPersonId(appointment.getNaturalPersonId());
        resp.setPayeeId(appointment.getPayeeId());
        resp.setTenantId(appointment.getTenantId());
        resp.setEnterpriseName(enterpriseName);
        resp.setSellerName(appointment.getSellerName());
        resp.setStationId(appointment.getStationId());
        resp.setStationCode(appointment.getStationCode());
        resp.setStationName(appointment.getStationName());
        resp.setGoodsConfigId(appointment.getGoodsConfigId());
        resp.setCategoryName(appointment.getCategoryName());
        resp.setUnit(appointment.getUnit());
        resp.setExpectedQuantity(appointment.getExpectedQuantity());
        resp.setExpectedQuantityText(expectedQuantityText(appointment));
        resp.setPlateNo(appointment.getPlateNo());
        resp.setExpectedArrivalTime(appointment.getExpectedArrivalTime());
        resp.setStatus(appointment.getStatus());
        AppointmentStatusEnum status = AppointmentStatusEnum.ofStatus(appointment.getStatus()).orElse(null);
        resp.setStatusName(status == null ? null : status.getName());
        resp.setArrivedAt(appointment.getArrivedAt());
        resp.setAcquisitionId(appointment.getAcquisitionId());
        resp.setCancelledAt(appointment.getCancelledAt());
        resp.setCancelReason(appointment.getCancelReason());
        resp.setNoShowReason(appointment.getNoShowReason());
        resp.setRemark(appointment.getRemark());
        resp.setCreateTime(appointment.getCreateTime());
        resp.setScopeNote(SCOPE_NOTE);
        return resp;
    }

    /**
     * 预计数量一律以「约」标注：它通常不准，任何地方都不许把它当准数用。
     */
    private String expectedQuantityText(IcbcAppointmentDO appointment) {
        BigDecimal quantity = appointment.getExpectedQuantity();
        if (quantity == null) {
            return null;
        }
        String unit = StrUtil.blankToDefault(appointment.getUnit(), "");
        return "约 " + quantity.stripTrailingZeros().toPlainString()
                + (StrUtil.isBlank(unit) ? "" : " " + unit);
    }

    private String enterpriseName(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        String name = tenantApi.getTenantName(tenantId);
        return StrUtil.isBlank(name) ? "回收企业" : name;
    }

    private String generateAppointmentNo() {
        return "APT" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

}
