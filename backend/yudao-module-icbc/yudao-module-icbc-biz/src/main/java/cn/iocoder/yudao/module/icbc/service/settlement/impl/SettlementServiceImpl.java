package cn.iocoder.yudao.module.icbc.service.settlement.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo.AcquisitionCorrectionReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo.*;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerSettlementConfirmReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerSettlementDisputeReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementVersionDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementVersionMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SettlementConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SettlementDisputeReasonEnum;
import cn.iocoder.yudao.module.icbc.service.acquisition.AcquisitionService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.icbc.service.settlement.SettlementService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 结算单 Service 实现。
 *
 * <p>三件事刻意分开：**生成**是现场动作（收货员点「结束本次收货」），**确认 / 异议**在自然人端，
 * **改版**是企业对异议的答复。生成后不再加单；改版只追加版本、不覆盖；确认把该版快照哈希落进结算单。
 */
@Slf4j
@Service
@Validated
public class SettlementServiceImpl implements SettlementService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String SOURCE_GENERATE = "GENERATE";
    private static final String SOURCE_ENTERPRISE_CHANGE = "ENTERPRISE_CHANGE";
    private static final String SOURCE_OPS = "OPS";
    private static final String UNINVOICED_REMINDER = "[已确认未开票超期，请尽快开票付款]";

    @Value("${icbc.settlement.confirm-timeout-days:7}")
    private int confirmTimeoutDays;
    @Value("${icbc.settlement.reply-timeout-days:3}")
    private int replyTimeoutDays;
    @Value("${icbc.settlement.uninvoiced-timeout-days:30}")
    private int uninvoicedTimeoutDays;

    @Resource
    private IcbcSettlementMapper settlementMapper;
    @Resource
    private IcbcSettlementVersionMapper versionMapper;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private AcquisitionService acquisitionService;
    @Resource
    private PayeeInfoService payeeInfoService;
    @Resource
    private NaturalPersonService naturalPersonService;

    // ==================== 生成 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generate(@Valid SettlementGenerateReqVO reqVO) {
        PayeeInfoDO payee = payeeInfoService.getPayeeInfo(reqVO.getPayeeId());
        // 现场动作是唯一可信的批次边界：只取「同一出售者、尚未归入结算单、且未作废」的收购单
        List<IcbcAcquisitionDO> acquisitions =
                acquisitionMapper.selectUngroupedByPayeeId(reqVO.getPayeeId(), reqVO.getBatchKey());
        if (acquisitions.isEmpty()) {
            throw exception(SETTLEMENT_NO_ACQUISITION);
        }
        IcbcNaturalPersonDO person = payeeInfoService.ensureNaturalPerson(payee);
        LocalDateTime now = LocalDateTime.now();
        IcbcSettlementDO settlement = IcbcSettlementDO.builder()
                .settlementNo("ST" + now.format(NO_FORMATTER) + IdUtil.fastSimpleUUID().substring(0, 4).toUpperCase())
                .payeeId(payee.getId())
                .naturalPersonId(person.getId())
                .sellerName(payee.getName())
                .sellerMobile(payee.getMobile())
                .batchKey(reqVO.getBatchKey())
                .generateTime(now)
                .generatedBy(SecurityFrameworkUtils.getLoginUserId())
                .confirmStatus(SettlementConfirmStatusEnum.PENDING.getStatus())
                .disputeCount(0)
                .deadlineTime(now.plusDays(confirmTimeoutDays))
                .remark(reqVO.getRemark())
                .build();
        settlementMapper.insert(settlement);
        // 生成后不得再加单：把收购单挂上结算单
        for (IcbcAcquisitionDO acquisition : acquisitions) {
            IcbcAcquisitionDO update = new IcbcAcquisitionDO();
            update.setId(acquisition.getId());
            update.setSettlementId(settlement.getId());
            acquisitionMapper.updateById(update);
            acquisition.setSettlementId(settlement.getId());
        }
        // 版本 1 整单快照
        IcbcSettlementVersionDO version = createVersion(settlement, acquisitions,
                "生成结算单", currentUser(), SOURCE_GENERATE);
        settlement.setCurrentVersionId(version.getId());
        settlement.setCurrentVersionNo(version.getVersionNo());
        settlementMapper.updateById(settlement);
        log.info("结算单生成成功 - settlementNo: {}, payeeId: {}, 收购单 {} 张",
                settlement.getSettlementNo(), payee.getId(), acquisitions.size());
        return settlement.getId();
    }

    // ==================== 查询 ====================

    @Override
    public PageResult<SettlementRespVO> getPage(SettlementPageReqVO reqVO) {
        PageResult<IcbcSettlementDO> page = settlementMapper.selectPage(reqVO);
        PageResult<SettlementRespVO> result = new PageResult<>();
        result.setList(page.getList().stream().map(this::toSimpleResp).toList());
        result.setTotal(page.getTotal());
        return result;
    }

    @Override
    public SettlementRespVO getDetail(Long id) {
        IcbcSettlementDO settlement = getSettlement(id);
        List<IcbcSettlementVersionDO> versions = versionMapper.selectListBySettlementId(id);
        List<IcbcAcquisitionDO> acquisitions = acquisitionMapper.selectListBySettlementId(id);
        SettlementRespVO resp = toResp(settlement, acquisitions);
        resp.setVersions(versions.stream().map(this::toVersionResp).toList());
        return resp;
    }

    @Override
    public List<SettlementRespVO> getListByPayeeId(Long payeeId) {
        return settlementMapper.selectListByPayeeId(payeeId).stream().map(this::toSimpleResp).toList();
    }

    @Override
    public List<SettlementRespVO> getListForSeller(Long naturalPersonId) {
        assertBound(naturalPersonId);
        return settlementMapper.selectListByNaturalPersonId(naturalPersonId).stream()
                .map(settlement -> toResp(settlement,
                        acquisitionMapper.selectListBySettlementId(settlement.getId())))
                .toList();
    }

    @Override
    public SettlementRespVO getDetailForSeller(Long naturalPersonId, Long id) {
        assertBound(naturalPersonId);
        IcbcSettlementDO settlement = getSettlement(id);
        assertOwnedBy(settlement, naturalPersonId);
        SettlementRespVO resp = toResp(settlement, acquisitionMapper.selectListBySettlementId(id));
        resp.setVersions(versionMapper.selectListBySettlementId(id).stream().map(this::toVersionResp).toList());
        return resp;
    }

    // ==================== 企业对异议 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeByEnterprise(@Valid SettlementChangeReqVO reqVO) {
        IcbcSettlementDO settlement = getSettlement(reqVO.getSettlementId());
        assertNotInvoiced(settlement.getId());
        // 只允许改本结算单自己的收购单：否则会改到别的结算单的档案上，而版本快照又无法解释这次改动。
        Set<Long> settlementAcquisitionIds = acquisitionMapper.selectListBySettlementId(settlement.getId()).stream()
                .map(IcbcAcquisitionDO::getId)
                .collect(Collectors.toSet());
        for (SettlementChangeReqVO.Line line : reqVO.getLines()) {
            if (!settlementAcquisitionIds.contains(line.getAcquisitionId())) {
                throw exception(SETTLEMENT_ACQUISITION_NOT_IN_SETTLEMENT,
                        String.valueOf(line.getAcquisitionId()));
            }
        }
        // 改：逐条修正计价（走与现场更正同一入口，保证结算重量 / 金额口径一致）
        for (SettlementChangeReqVO.Line line : reqVO.getLines()) {
            AcquisitionCorrectionReqVO correction = new AcquisitionCorrectionReqVO();
            correction.setId(line.getAcquisitionId());
            correction.setDeduction(line.getDeduction());
            correction.setDeductionMethod(line.getDeductionMethod());
            correction.setUnitPrice(line.getUnitPrice());
            correction.setAdjustmentAmount(line.getAdjustmentAmount());
            correction.setAdjustmentReason(line.getAdjustmentReason());
            acquisitionService.correctRecognition(correction);
        }
        List<IcbcAcquisitionDO> acquisitions = acquisitionMapper.selectListBySettlementId(settlement.getId());
        IcbcSettlementVersionDO version = createVersion(settlement, acquisitions,
                reqVO.getChangeReason(), currentUser(), SOURCE_ENTERPRISE_CHANGE);
        applyPending(settlement, version, reqVO.getChangeReason());
        settlementMapper.updateById(settlement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyNoChange(@Valid SettlementReplyReqVO reqVO) {
        IcbcSettlementDO settlement = getSettlement(reqVO.getSettlementId());
        assertNotInvoiced(settlement.getId());
        // 不改但附说明：内容不变，只是回到待确认，自然人再确认一次即等于接受
        settlement.setConfirmStatus(SettlementConfirmStatusEnum.PENDING.getStatus());
        settlement.setConfirmTime(null);
        settlement.setConfirmHash(null);
        settlement.setConfirmIp(null);
        settlement.setConfirmDevice(null);
        settlement.setConfirmMemberUserId(null);
        settlement.setEnterpriseReplyNote(reqVO.getNote());
        settlement.setEnterpriseReplyTime(LocalDateTime.now());
        settlement.setDeadlineTime(LocalDateTime.now().plusDays(confirmTimeoutDays));
        settlementMapper.updateById(settlement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineSign(@Valid SettlementOfflineSignReqVO reqVO) {
        IcbcSettlementDO settlement = getSettlement(reqVO.getSettlementId());
        assertNotInvoiced(settlement.getId());
        IcbcSettlementVersionDO current = currentVersion(settlement);
        settlement.setConfirmStatus(SettlementConfirmStatusEnum.OFFLINE_CONFIRMED.getStatus());
        settlement.setConfirmTime(LocalDateTime.now());
        settlement.setConfirmHash(current == null ? null : current.getSnapshotHash());
        settlement.setOfflineSignFileUrl(reqVO.getFileUrl());
        settlement.setOfflineSignHandler(reqVO.getHandler());
        settlement.setOfflineSignTime(LocalDateTime.now());
        settlement.setDeadlineTime(null);
        if (StrUtil.isNotBlank(reqVO.getRemark())) {
            settlement.setRemark(reqVO.getRemark());
        }
        settlementMapper.updateById(settlement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAcquisition(@Valid SettlementAcquisitionCancelReqVO reqVO) {
        IcbcAcquisitionDO acquisition = acquisitionService.getAcquisition(reqVO.getAcquisitionId());
        if (StrUtil.isBlank(reqVO.getReason())) {
            throw exception(SETTLEMENT_CANCEL_REASON_REQUIRED);
        }
        // 作废是结算单里的动作：尚未归入结算单的收购单不在此处处置
        if (acquisition.getSettlementId() == null) {
            throw exception(SETTLEMENT_ACQUISITION_NOT_GROUPED);
        }
        if (isInvoiceStarted(acquisition)) {
            throw exception(SETTLEMENT_STATUS_NOT_ALLOW, "该收购单已发起开票付款，作废请改走红冲");
        }
        IcbcAcquisitionDO update = new IcbcAcquisitionDO();
        update.setId(acquisition.getId());
        update.setStatus(AcquisitionStatusEnum.CANCELLED.getStatus());
        update.setCancelReason(reqVO.getReason());
        acquisitionMapper.updateById(update);
    }

    // ==================== 自然人侧 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(@Valid SellerSettlementConfirmReqVO reqVO, String ip, String device) {
        assertBound(reqVO.getNaturalPersonId());
        IcbcSettlementDO settlement = getSettlement(reqVO.getSettlementId());
        assertOwnedBy(settlement, reqVO.getNaturalPersonId());
        if (!Objects.equals(settlement.getCurrentVersionId(), reqVO.getVersionId())) {
            throw exception(SETTLEMENT_VERSION_NOT_EXISTS, "该版本已不是最新版，请刷新后重看");
        }
        if (SettlementConfirmStatusEnum.isConfirmed(settlement.getConfirmStatus())) {
            return; // 幂等
        }
        IcbcSettlementVersionDO version = versionMapper.selectById(reqVO.getVersionId());
        if (version == null) {
            throw exception(SETTLEMENT_VERSION_NOT_EXISTS, String.valueOf(reqVO.getVersionId()));
        }
        settlement.setConfirmStatus(SettlementConfirmStatusEnum.CONFIRMED.getStatus());
        settlement.setConfirmTime(LocalDateTime.now());
        settlement.setConfirmIp(ip);
        settlement.setConfirmDevice(device);
        settlement.setConfirmHash(version.getSnapshotHash());
        settlement.setConfirmMemberUserId(SecurityFrameworkUtils.getLoginUserId());
        settlement.setDeadlineTime(null);
        settlementMapper.updateById(settlement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void raiseDispute(@Valid SellerSettlementDisputeReqVO reqVO) {
        assertBound(reqVO.getNaturalPersonId());
        IcbcSettlementDO settlement = getSettlement(reqVO.getSettlementId());
        assertOwnedBy(settlement, reqVO.getNaturalPersonId());
        SettlementDisputeReasonEnum reason = SettlementDisputeReasonEnum.ofReason(reqVO.getReason())
                .orElseThrow(() -> exception(SETTLEMENT_DISPUTE_REASON_INVALID, reqVO.getReason()));
        if (reason == SettlementDisputeReasonEnum.OTHER && StrUtil.isBlank(reqVO.getNote())) {
            throw exception(SETTLEMENT_DISPUTE_NOTE_REQUIRED);
        }
        LocalDateTime now = LocalDateTime.now();
        settlement.setConfirmStatus(SettlementConfirmStatusEnum.DISPUTED.getStatus());
        settlement.setDisputeReason(reason.getReason());
        settlement.setDisputeNote(reqVO.getNote());
        settlement.setDisputeTime(now);
        settlement.setDisputeCount((settlement.getDisputeCount() == null ? 0 : settlement.getDisputeCount()) + 1);
        // 异议后企业须在 3 天内处理；到期不自动确认，自然人侧显示「企业尚未回复」
        settlement.setDeadlineTime(now.plusDays(replyTimeoutDays));
        settlement.setEnterpriseReplyNote(null);
        settlement.setEnterpriseReplyTime(null);
        settlementMapper.updateById(settlement);
    }

    // ==================== 门禁与定时 ====================

    @Override
    public boolean isSettlementConfirmed(Long acquisitionId) {
        IcbcAcquisitionDO acquisition = acquisitionService.getAcquisition(acquisitionId);
        if (acquisition.getSettlementId() == null) {
            return false;
        }
        IcbcSettlementDO settlement = settlementMapper.selectById(acquisition.getSettlementId());
        return settlement != null && SettlementConfirmStatusEnum.isConfirmed(settlement.getConfirmStatus());
    }

    @Override
    public void assertSettlementConfirmed(Long acquisitionId) {
        if (!isSettlementConfirmed(acquisitionId)) {
            throw exception(SETTLEMENT_NOT_CONFIRMED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int handleTimeout() {
        LocalDateTime now = LocalDateTime.now();
        List<IcbcSettlementDO> overdue = settlementMapper.selectTimeoutList(
                List.of(SettlementConfirmStatusEnum.PENDING.getStatus()), now);
        int escalated = 0;
        for (IcbcSettlementDO settlement : overdue) {
            // 到期**不自动确认**：升级为「需线下签字确认」，并在企业侧生成待办（状态本身就是待办）
            IcbcSettlementDO update = new IcbcSettlementDO();
            update.setId(settlement.getId());
            update.setConfirmStatus(SettlementConfirmStatusEnum.OFFLINE_REQUIRED.getStatus());
            update.setRemark(StrUtil.blankToDefault(settlement.getRemark(), "")
                    + "[确认超期，转线下签字确认]");
            // 保留 deadline 以便页面显示「超期未确认」，不再自动升级
            settlementMapper.updateById(update);
            escalated++;
        }
        // 已确认但长期未开票：只提醒企业尽快开票付款，不自动作废
        List<IcbcSettlementDO> confirmedOverdue = settlementMapper.selectConfirmedBefore(
                List.of(SettlementConfirmStatusEnum.CONFIRMED.getStatus(),
                        SettlementConfirmStatusEnum.OFFLINE_CONFIRMED.getStatus()),
                now.minusDays(uninvoicedTimeoutDays));
        for (IcbcSettlementDO settlement : confirmedOverdue) {
            if (isSettled(acquisitionMapper.selectListBySettlementId(settlement.getId()))) {
                continue;
            }
            if (StrUtil.contains(settlement.getRemark(), UNINVOICED_REMINDER)) {
                continue; // 幂等：同一个提醒不重复追加
            }
            IcbcSettlementDO update = new IcbcSettlementDO();
            update.setId(settlement.getId());
            update.setRemark(StrUtil.blankToDefault(settlement.getRemark(), "") + UNINVOICED_REMINDER);
            settlementMapper.updateById(update);
        }
        return escalated;
    }

    // ==================== 内部 ====================

    private IcbcSettlementDO getSettlement(Long id) {
        IcbcSettlementDO settlement = id == null ? null : settlementMapper.selectById(id);
        if (settlement == null) {
            throw exception(SETTLEMENT_NOT_EXISTS);
        }
        return settlement;
    }

    private IcbcSettlementVersionDO currentVersion(IcbcSettlementDO settlement) {
        if (settlement.getCurrentVersionId() == null) {
            return null;
        }
        return versionMapper.selectById(settlement.getCurrentVersionId());
    }

    private void assertOwnedBy(IcbcSettlementDO settlement, Long naturalPersonId) {
        if (!Objects.equals(settlement.getNaturalPersonId(), naturalPersonId)) {
            throw exception(SETTLEMENT_STATUS_NOT_ALLOW, "该结算单不属于所选身份");
        }
    }

    private void assertNotInvoiced(Long settlementId) {
        List<IcbcAcquisitionDO> acquisitions = acquisitionMapper.selectListBySettlementId(settlementId);
        boolean invoiced = acquisitions.stream().anyMatch(this::isInvoiceStarted);
        if (invoiced) {
            throw exception(SETTLEMENT_INVOICED_NOT_EDITABLE);
        }
    }

    private boolean isInvoiceStarted(IcbcAcquisitionDO acquisition) {
        return StrUtil.isNotBlank(acquisition.getInvoicePartnerOrderId())
                || Objects.equals(acquisition.getStatus(), AcquisitionStatusEnum.PENDING_PAYMENT.getStatus())
                || Objects.equals(acquisition.getStatus(), AcquisitionStatusEnum.PAID.getStatus())
                || Objects.equals(acquisition.getStatus(), AcquisitionStatusEnum.INVOICED.getStatus());
    }

    private void applyPending(IcbcSettlementDO settlement, IcbcSettlementVersionDO version, String note) {
        settlement.setCurrentVersionId(version.getId());
        settlement.setCurrentVersionNo(version.getVersionNo());
        settlement.setConfirmStatus(SettlementConfirmStatusEnum.PENDING.getStatus());
        settlement.setConfirmTime(null);
        settlement.setConfirmHash(null);
        settlement.setConfirmIp(null);
        settlement.setConfirmDevice(null);
        settlement.setConfirmMemberUserId(null);
        settlement.setEnterpriseReplyNote(note);
        settlement.setEnterpriseReplyTime(LocalDateTime.now());
        settlement.setDeadlineTime(LocalDateTime.now().plusDays(confirmTimeoutDays));
    }

    /**
     * 落一版整单快照（结算重量 / 单价 / 调整项 / 逐条收购单明细），并计算快照哈希。
     * 版本只追加，不覆盖。
     */
    private IcbcSettlementVersionDO createVersion(IcbcSettlementDO settlement,
                                                  List<IcbcAcquisitionDO> acquisitions,
                                                  String changeReason, String changedBy, String source) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("settlementId", settlement.getId());
        snapshot.put("settlementNo", settlement.getSettlementNo());
        snapshot.put("payeeId", settlement.getPayeeId());
        snapshot.put("naturalPersonId", settlement.getNaturalPersonId());
        List<Map<String, Object>> lines = new ArrayList<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (IcbcAcquisitionDO acquisition : acquisitions) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("acquisitionId", acquisition.getId());
            line.put("acquisitionNo", acquisition.getAcquisitionNo());
            line.put("categoryName", acquisition.getCategoryName());
            line.put("unit", acquisition.getUnit());
            line.put("quantity", acquisition.getQuantity());
            line.put("grossWeight", acquisition.getGrossWeight());
            line.put("tareWeight", acquisition.getTareWeight());
            line.put("deduction", acquisition.getDeduction());
            line.put("deductionMethod", acquisition.getDeductionMethod());
            line.put("settlementWeight", acquisition.getSettlementWeight());
            line.put("unitPrice", acquisition.getUnitPrice());
            line.put("adjustmentAmount", acquisition.getAdjustmentAmount());
            line.put("adjustmentReason", acquisition.getAdjustmentReason());
            line.put("amount", acquisition.getAmount());
            lines.add(line);
            if (acquisition.getSettlementWeight() != null) {
                totalWeight = totalWeight.add(acquisition.getSettlementWeight());
            }
            if (acquisition.getAmount() != null) {
                totalAmount = totalAmount.add(acquisition.getAmount());
            }
        }
        snapshot.put("lines", lines);
        String json = JSON.toJSONString(snapshot);
        int versionNo = versionMapper.selectMaxVersionNo(settlement.getId()) + 1;
        IcbcSettlementVersionDO version = IcbcSettlementVersionDO.builder()
                .settlementId(settlement.getId())
                .versionNo(versionNo)
                .snapshotJson(json)
                .snapshotHash(DigestUtil.sha256Hex(json))
                .totalSettlementWeight(totalWeight)
                .totalAmount(totalAmount)
                .acquisitionCount(acquisitions.size())
                .changeReason(changeReason)
                .changedBy(changedBy)
                .source(source)
                .build();
        versionMapper.insert(version);
        return version;
    }

    /**
     * 校验所选身份在当前登录名下（显式选择，不静默推断）。直接走自然人身份层，避免拉入会员模块依赖。
     */
    private void assertBound(Long naturalPersonId) {
        if (!naturalPersonService.isBoundToLogin(naturalPersonId, SecurityFrameworkUtils.getLoginUserId())) {
            throw exception(NATURAL_PERSON_NOT_BOUND_TO_LOGIN);
        }
    }

    private String currentUser() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId == null ? SOURCE_OPS : String.valueOf(userId);
    }

    // ==================== 映射 ====================

    private SettlementRespVO toSimpleResp(IcbcSettlementDO settlement) {
        IcbcSettlementVersionDO current = currentVersion(settlement);
        SettlementRespVO resp = new SettlementRespVO();
        fillCommon(resp, settlement, current);
        List<IcbcAcquisitionDO> acquisitions = acquisitionMapper.selectListBySettlementId(settlement.getId());
        resp.setAcquisitionCount(acquisitions.size());
        resp.setSettled(isSettled(acquisitions));
        return resp;
    }

    private SettlementRespVO toResp(IcbcSettlementDO settlement, List<IcbcAcquisitionDO> acquisitions) {
        IcbcSettlementVersionDO current = currentVersion(settlement);
        SettlementRespVO resp = new SettlementRespVO();
        fillCommon(resp, settlement, current);
        resp.setAcquisitionCount(acquisitions.size());
        resp.setSettled(isSettled(acquisitions));
        resp.setLines(acquisitions.stream().map(this::toLineResp).toList());
        return resp;
    }

    private void fillCommon(SettlementRespVO resp, IcbcSettlementDO settlement, IcbcSettlementVersionDO current) {
        BeanUtils.copyProperties(settlement, resp);
        SettlementConfirmStatusEnum status = SettlementConfirmStatusEnum.ofStatus(settlement.getConfirmStatus())
                .orElse(null);
        resp.setConfirmStatusName(status == null ? null : status.getName());
        resp.setDisputeReasonName(SettlementDisputeReasonEnum.nameOf(settlement.getDisputeReason()));
        resp.setSuggestOffline(settlement.getDisputeCount() != null && settlement.getDisputeCount() >= 3);
        // 企业尚未回复：有异议且已过企业处理时限
        resp.setEnterpriseNotReplied(Objects.equals(settlement.getConfirmStatus(),
                        SettlementConfirmStatusEnum.DISPUTED.getStatus())
                && settlement.getDeadlineTime() != null
                && settlement.getDeadlineTime().isBefore(LocalDateTime.now()));
        if (current != null) {
            resp.setTotalSettlementWeight(current.getTotalSettlementWeight());
            resp.setTotalAmount(current.getTotalAmount());
            resp.setAcquisitionCount(current.getAcquisitionCount());
        }
    }

    private SettlementRespVO.VersionVO toVersionResp(IcbcSettlementVersionDO version) {
        SettlementRespVO.VersionVO vo = new SettlementRespVO.VersionVO();
        BeanUtils.copyProperties(version, vo);
        return vo;
    }

    private SettlementRespVO.LineVO toLineResp(IcbcAcquisitionDO acquisition) {
        SettlementRespVO.LineVO line = new SettlementRespVO.LineVO();
        BeanUtils.copyProperties(acquisition, line);
        line.setAcquisitionId(acquisition.getId());
        AcquisitionStatusEnum status = AcquisitionStatusEnum.ofStatus(acquisition.getStatus()).orElse(null);
        line.setStatusName(status == null ? null : status.getName());
        return line;
    }

    /**
     * 是否已结清：其下**非作废**收购单都已开票。由其下收购单推导，不落库（ADR 0021）。
     */
    private boolean isSettled(List<IcbcAcquisitionDO> acquisitions) {
        boolean hasActive = false;
        for (IcbcAcquisitionDO acquisition : acquisitions) {
            if (Objects.equals(acquisition.getStatus(), AcquisitionStatusEnum.CANCELLED.getStatus())) {
                continue;
            }
            hasActive = true;
            if (!Objects.equals(acquisition.getStatus(), AcquisitionStatusEnum.INVOICED.getStatus())) {
                return false;
            }
        }
        return hasActive;
    }

}
