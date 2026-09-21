package cn.iocoder.yudao.module.icbc.service.evidence.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.settlement.IcbcSettlementDO;
import cn.iocoder.yudao.module.icbc.enums.SettlementConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceDownloadDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceFileDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.evidence.IcbcEvidenceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.OrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payment.PaymentOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceDownloadMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceFileMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.evidence.IcbcEvidenceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.OrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payment.PaymentOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.settlement.IcbcSettlementMapper;
import cn.iocoder.yudao.module.icbc.enums.EvidenceFlowEnum;
import cn.iocoder.yudao.module.icbc.enums.IcbcEvidenceTypeEnum;
import cn.iocoder.yudao.module.icbc.service.evidence.InvoiceEvidenceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 一票一档证据链 Service 实现。
 *
 * <p>五流里能从业务表自动取到的三流（资金流=支付单、发票流=发票+原件、信息流=台账条目）
 * 在读取时聚合；合同流与货物流暂由人工补录（{@code icbc_evidence}），
 * 等 #7 收购登记落地后再把自动来源接进来。
 *
 * <p>本类只负责取数与装配；证据包 / 台账的输出格式（zip、CSV、Excel）交给
 * {@link EvidencePackageWriter}。
 */
@Service
@Validated
public class InvoiceEvidenceServiceImpl implements InvoiceEvidenceService {

    /** 五流总数，齐备率的分母 */
    private static final int TOTAL_FLOW_COUNT = 5;
    /** 工行支付状态：2-支付成功 */
    private static final int PAYMENT_STATUS_SUCCESS = 2;

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private RedInvoiceMapper redInvoiceMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private InvoiceDownloadMapper invoiceDownloadMapper;
    @Resource
    private InvoiceFileMapper invoiceFileMapper;
    @Resource
    private IcbcEvidenceMapper evidenceMapper;
    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private IcbcFrameworkAgreementMapper frameworkAgreementMapper;
    @Resource
    private IcbcSettlementMapper settlementMapper;
    @Resource
    private EvidencePackageWriter evidencePackageWriter;

    @Override
    public EvidenceChainRespVO getEvidenceChain(String partnerOrderId) {
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (order == null) {
            throw exception(INVOICE_ORDER_NOT_EXISTS);
        }
        return assembleChain(order, buildContext(Collections.singletonList(order)));
    }

    @Override
    public PageResult<EvidenceChainRespVO> getEvidencePage(EvidencePageReqVO pageReqVO) {
        LambdaQueryWrapper<InvoiceOrderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(pageReqVO.getPartnerOrderId()),
                        InvoiceOrderDO::getPartnerOrderId, pageReqVO.getPartnerOrderId())
                .like(StrUtil.isNotBlank(pageReqVO.getInvoiceNo()),
                        InvoiceOrderDO::getInvoiceNo, pageReqVO.getInvoiceNo());
        if (pageReqVO.getCreateTime() != null && pageReqVO.getCreateTime().length == 2) {
            wrapper.between(InvoiceOrderDO::getCreateTime, pageReqVO.getCreateTime()[0], pageReqVO.getCreateTime()[1]);
        }
        wrapper.orderByDesc(InvoiceOrderDO::getId);
        PageResult<InvoiceOrderDO> page = invoiceOrderMapper.selectPage(pageReqVO, wrapper);

        ChainContext ctx = buildContext(page.getList());
        List<EvidenceChainRespVO> chains = page.getList().stream()
                .map(order -> assembleChain(order, ctx))
                .collect(Collectors.toList());
        return new PageResult<>(chains, page.getTotal());
    }

    @Override
    public EvidenceCompletenessSummaryRespVO getCompleteness(EvidenceScopeReqVO scopeReqVO) {
        List<InvoiceOrderDO> orders = resolveOrders(scopeReqVO.getPartnerOrderIds(), scopeReqVO.getCreateTime());
        ChainContext ctx = buildContext(orders);

        List<EvidenceCompletenessItemRespVO> items = new ArrayList<>();
        int presentSum = 0;
        int completeCount = 0;
        for (InvoiceOrderDO order : orders) {
            EvidenceChainRespVO chain = assembleChain(order, ctx);
            presentSum += chain.getPresentCount();
            if (Boolean.TRUE.equals(chain.getComplete())) {
                completeCount++;
            }
            EvidenceCompletenessItemRespVO item = new EvidenceCompletenessItemRespVO();
            item.setTenantId(order.getTenantId());
            item.setPartnerOrderId(order.getPartnerOrderId());
            item.setInvoiceNo(order.getInvoiceNo());
            item.setPresentCount(chain.getPresentCount());
            item.setTotalCount(TOTAL_FLOW_COUNT);
            item.setCompletenessRate(chain.getCompletenessRate());
            item.setMissingFlows(chain.getFlows().stream()
                    .filter(flow -> !Boolean.TRUE.equals(flow.getPresent()))
                    .map(EvidenceFlowRespVO::getFlowName)
                    .collect(Collectors.toList()));
            items.add(item);
        }

        EvidenceCompletenessSummaryRespVO summary = new EvidenceCompletenessSummaryRespVO();
        summary.setInvoiceCount(orders.size());
        summary.setCompleteCount(completeCount);
        summary.setCompletenessRate(orders.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(presentSum * 100.0 / (orders.size() * (double) TOTAL_FLOW_COUNT))
                        .setScale(2, RoundingMode.HALF_UP));
        summary.setItems(items);
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long attachEvidence(EvidenceAttachReqVO reqVO) {
        IcbcEvidenceTypeEnum type = IcbcEvidenceTypeEnum.ofCode(reqVO.getEvidenceType())
                .orElseThrow(() -> exception(EVIDENCE_TYPE_INVALID));
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(reqVO.getPartnerOrderId());
        if (order == null) {
            throw exception(INVOICE_ORDER_NOT_EXISTS);
        }
        IcbcEvidenceDO evidence = IcbcEvidenceDO.builder()
                .invoiceOrderId(order.getId())
                .partnerOrderId(order.getPartnerOrderId())
                .flow(type.getFlow().getCode())
                .evidenceType(type.getCode())
                .title(StrUtil.blankToDefault(reqVO.getTitle(), type.getName()))
                .fileUrl(reqVO.getFileUrl())
                .fileName(reqVO.getFileName())
                .occurredTime(reqVO.getOccurredTime())
                .remark(reqVO.getRemark())
                .build();
        evidenceMapper.insert(evidence);
        return evidence.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEvidence(Long id) {
        if (id == null || evidenceMapper.selectById(id) == null) {
            throw exception(EVIDENCE_NOT_EXISTS);
        }
        evidenceMapper.deleteById(id);
    }

    @Override
    public List<AcquisitionLedgerRespVO> getLedgerRows(AcquisitionLedgerReqVO reqVO) {
        List<InvoiceOrderDO> orders;
        if (StrUtil.isNotBlank(reqVO.getPartnerOrderId())) {
            InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(reqVO.getPartnerOrderId());
            orders = order != null ? Collections.singletonList(order) : Collections.emptyList();
        } else {
            LambdaQueryWrapper<InvoiceOrderDO> wrapper = new LambdaQueryWrapper<>();
            if (reqVO.getStartTime() != null) {
                wrapper.ge(InvoiceOrderDO::getCreateTime, reqVO.getStartTime());
            }
            if (reqVO.getEndTime() != null) {
                wrapper.le(InvoiceOrderDO::getCreateTime, reqVO.getEndTime());
            }
            wrapper.orderByAsc(InvoiceOrderDO::getId);
            orders = invoiceOrderMapper.selectList(wrapper);
        }

        ChainContext ctx = buildContext(orders);
        List<AcquisitionLedgerRespVO> rows = new ArrayList<>();
        for (InvoiceOrderDO order : orders) {
            rows.addAll(buildLedgerRows(order, ctx));
        }
        return rows;
    }

    @Override
    public void exportEvidencePackage(String partnerOrderId, HttpServletResponse response) {
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (order == null) {
            throw exception(INVOICE_ORDER_NOT_EXISTS);
        }
        ChainContext ctx = buildContext(Collections.singletonList(order));
        evidencePackageWriter.writeZip(response, "证据包_" + partnerOrderId,
                Collections.singletonList(buildPackageContent(order, ctx)));
    }

    @Override
    public void exportEvidencePackageBatch(EvidenceScopeReqVO scopeReqVO, HttpServletResponse response) {
        List<InvoiceOrderDO> orders = resolveOrders(scopeReqVO.getPartnerOrderIds(), scopeReqVO.getCreateTime());
        if (CollUtil.isEmpty(orders)) {
            throw exception(EVIDENCE_PACKAGE_NO_INVOICE);
        }
        ChainContext ctx = buildContext(orders);
        List<EvidencePackageWriter.PackageContent> packages = orders.stream()
                .map(order -> buildPackageContent(order, ctx))
                .collect(Collectors.toList());
        evidencePackageWriter.writeZip(response, "证据包_" + orders.size() + "票", packages);
    }

    @Override
    public void exportAcquisitionLedger(AcquisitionLedgerReqVO reqVO, HttpServletResponse response) {
        evidencePackageWriter.writeLedgerExcel(response, getLedgerRows(reqVO));
    }

    // ==================== 证据装配 ====================

    private EvidenceChainRespVO assembleChain(InvoiceOrderDO order, ChainContext ctx) {
        List<IcbcEvidenceDO> evidenceList = ctx.evidenceByOrder
                .getOrDefault(order.getPartnerOrderId(), Collections.emptyList());
        List<OrderItemDO> items = ctx.itemsByOrderId
                .getOrDefault(order.getId(), Collections.emptyList());
        PaymentOrderDO payment = ctx.paymentByOrder.get(order.getPartnerOrderId());
        InvoiceDownloadDO download = ctx.downloadByOrder.get(order.getPartnerOrderId());
        List<InvoiceFileDO> files = download != null
                ? ctx.filesByDownload.getOrDefault(download.getId(), Collections.emptyList())
                : Collections.emptyList();

        Map<EvidenceFlowEnum, List<EvidenceSourceRespVO>> flowSources = new EnumMap<>(EvidenceFlowEnum.class);
        for (EvidenceFlowEnum flow : EvidenceFlowEnum.ordered()) {
            flowSources.put(flow, new ArrayList<>());
        }

        // 收购登记单：该笔的合同流（收购确认书）、货物流（磅单 + 车辆照片）与信息流骨架
        IcbcAcquisitionDO acquisition = ctx.acquisitionByInvoiceOrder.get(order.getPartnerOrderId());

        // 人工补录的证据，按各自归属的流落位
        for (IcbcEvidenceDO evidence : evidenceList) {
            EvidenceFlowEnum.ofCode(evidence.getFlow())
                    .ifPresent(flow -> flowSources.get(flow).add(toSource(evidence)));
        }

        // 合同流：收购登记单本身就是该笔收购的确认书
        if (acquisition != null) {
            EvidenceSourceRespVO source = new EvidenceSourceRespVO();
            source.setSourceType("ACQUISITION");
            source.setTitle("单笔收购确认书");
            source.setRef(acquisition.getAcquisitionNo());
            source.setOccurredTime(acquisition.getTradeTime());
            flowSources.get(EvidenceFlowEnum.CONTRACT).add(source);

            // 合同流：出售者的结算确认记录（含快照哈希、线下签字件）也是签署证据（ADR 0018 / 0024，不新增第六流）
            addSettlementConfirmationSource(flowSources.get(EvidenceFlowEnum.CONTRACT), acquisition);
        }

        // 合同流：出售者**生效中**的框架收购协议（两份文书一个合同组，已签文件托管在第三方，#95 / ADR 0036）。
        // 两份文书**分别成条**：ADR 0036 决策 3 否决「拼成一个 PDF」的理由就是要能分别引用，
        // 所以这里按主文书 + 告知函各自挂一条，同归 FRAMEWORK_AGREEMENT 这一类型 / 合同流，
        // 不新增证据类型、不新增第六流。主文书 = 框架收购协议（`fileUrl`），告知函单独放 `noticeFileUrl`。
        // **地址为空不成条**（#95 SP-2）：纸路径没有电子地址时不会挂出空壳条目。
        IcbcFrameworkAgreementDO agreement = order.getPayeeId() != null
                ? ctx.agreementByPayee.get(order.getPayeeId()) : null;
        if (agreement != null) {
            String type = IcbcEvidenceTypeEnum.FRAMEWORK_AGREEMENT.getCode();
            addSource(flowSources.get(EvidenceFlowEnum.CONTRACT), type,
                    "框架收购协议", agreement.getAgreementNo(), agreement.getFileUrl(), agreement.getSignedAt());
            addSource(flowSources.get(EvidenceFlowEnum.CONTRACT), type,
                    "反向发票合规告知函", agreement.getAgreementNo(), agreement.getNoticeFileUrl(),
                    agreement.getSignedAt());
        }

        // 货物流：磅单与车头车尾照片直接从收购登记单取
        if (acquisition != null) {
            if (StrUtil.isNotBlank(acquisition.getWeightTicketNo())
                    || StrUtil.isNotBlank(acquisition.getWeightTicketImageUrl())) {
                EvidenceSourceRespVO source = new EvidenceSourceRespVO();
                source.setSourceType("ACQUISITION");
                source.setTitle("过磅单");
                source.setRef(acquisition.getWeightTicketNo());
                source.setUrl(acquisition.getWeightTicketImageUrl());
                source.setOccurredTime(acquisition.getTradeTime());
                flowSources.get(EvidenceFlowEnum.GOODS).add(source);
            }
            addImageSource(flowSources.get(EvidenceFlowEnum.GOODS), "车头照片",
                    acquisition.getVehicleFrontImageUrl(), acquisition.getTradeTime());
            addImageSource(flowSources.get(EvidenceFlowEnum.GOODS), "车尾照片",
                    acquisition.getVehicleRearImageUrl(), acquisition.getTradeTime());
        }

        // 资金流：支付成功后归档的转账回单（回单号 + 回单文件），挂回该笔收购
        if (payment != null && Objects.equals(payment.getPaymentStatus(), PAYMENT_STATUS_SUCCESS)) {
            EvidenceSourceRespVO source = new EvidenceSourceRespVO();
            source.setSourceType("PAYMENT_ORDER");
            source.setTitle("转账回单");
            source.setRef(StrUtil.blankToDefault(payment.getReceiptNo(),
                    StrUtil.blankToDefault(payment.getPaymentSerialNo(), payment.getOrderNo())));
            source.setUrl(payment.getReceiptFileUrl());
            source.setOccurredTime(payment.getReceiptTime() != null
                    ? payment.getReceiptTime() : payment.getPaymentTime());
            flowSources.get(EvidenceFlowEnum.CAPITAL).add(source);
        }

        // 发票流：发票号码 + 已下载的原件
        if (StrUtil.isNotBlank(order.getInvoiceNo())) {
            EvidenceSourceRespVO source = new EvidenceSourceRespVO();
            source.setSourceType("INVOICE_ORDER");
            source.setTitle("报废产品收购发票");
            source.setRef(order.getInvoiceNo());
            source.setUrl(isHttpUrl(order.getInvoiceFileUrl()) ? order.getInvoiceFileUrl() : null);
            source.setOccurredTime(order.getInvoiceDate());
            flowSources.get(EvidenceFlowEnum.INVOICE).add(source);
        }
        for (InvoiceFileDO file : files) {
            EvidenceSourceRespVO source = new EvidenceSourceRespVO();
            source.setSourceType("INVOICE_FILE");
            source.setTitle(file.getFileName());
            source.setRef(file.getInvoiceNumber());
            source.setDownloadId(file.getDownloadId());
            source.setFileType(file.getFileType());
            source.setOccurredTime(file.getUploadTime());
            flowSources.get(EvidenceFlowEnum.INVOICE).add(source);
        }

        // 发票流（红冲）：红票开出后，蓝票的红冲也是发票流的一部分，红蓝一一对应
        RedInvoiceDO red = ctx.redByOrder.get(order.getPartnerOrderId());
        if (red != null && StrUtil.isNotBlank(red.getRedInvoiceNo())) {
            EvidenceSourceRespVO source = new EvidenceSourceRespVO();
            source.setSourceType("RED_INVOICE");
            source.setTitle("红字发票（红冲）");
            source.setRef(red.getRedInvoiceNo());
            source.setOccurredTime(red.getRedInvoiceDate());
            flowSources.get(EvidenceFlowEnum.INVOICE).add(source);
        }

        // 信息流：整张票就是一个台账条目；有收购登记单时以登记单为准
        if (CollUtil.isNotEmpty(items)) {
            EvidenceSourceRespVO source = new EvidenceSourceRespVO();
            source.setSourceType("INVOICE_ORDER");
            source.setTitle("收购台账条目");
            source.setRef(order.getOrderNo());
            source.setOccurredTime(order.getCreateTime());
            flowSources.get(EvidenceFlowEnum.INFO).add(source);
        }
        if (acquisition != null) {
            EvidenceSourceRespVO source = new EvidenceSourceRespVO();
            source.setSourceType("ACQUISITION");
            source.setTitle("收购台账条目");
            source.setRef(acquisition.getAcquisitionNo());
            source.setOccurredTime(acquisition.getTradeTime());
            flowSources.get(EvidenceFlowEnum.INFO).add(source);
        }

        List<EvidenceFlowRespVO> flows = new ArrayList<>();
        int presentCount = 0;
        for (EvidenceFlowEnum flow : EvidenceFlowEnum.ordered()) {
            List<EvidenceSourceRespVO> sources = flowSources.get(flow);
            boolean present = CollUtil.isNotEmpty(sources);
            if (present) {
                presentCount++;
            }
            EvidenceFlowRespVO flowVO = new EvidenceFlowRespVO();
            flowVO.setFlow(flow.getCode());
            flowVO.setFlowName(flow.getName());
            flowVO.setPresent(present);
            flowVO.setSources(sources);
            flows.add(flowVO);
        }

        EvidenceChainRespVO chain = new EvidenceChainRespVO();
        chain.setPartnerOrderId(order.getPartnerOrderId());
        chain.setOrderNo(order.getOrderNo());
        chain.setInvoiceNo(order.getInvoiceNo());
        PayeeInfoDO payee = order.getPayeeId() != null ? ctx.payeeById.get(order.getPayeeId()) : null;
        chain.setSellerName(payee != null ? payee.getName() : null);
        chain.setTotalAmount(order.getTotalAmount());
        chain.setPresentCount(presentCount);
        chain.setTotalCount(TOTAL_FLOW_COUNT);
        chain.setCompletenessRate(rate(presentCount));
        chain.setComplete(presentCount == TOTAL_FLOW_COUNT);
        chain.setFlows(flows);
        chain.setAttachments(evidenceList.stream().map(this::toEvidenceRespVO).collect(Collectors.toList()));
        chain.setUpdateTime(order.getUpdateTime());
        return chain;
    }

    private List<AcquisitionLedgerRespVO> buildLedgerRows(InvoiceOrderDO order, ChainContext ctx) {
        PayeeInfoDO payee = order.getPayeeId() != null ? ctx.payeeById.get(order.getPayeeId()) : null;
        IcbcAcquisitionDO acquisition = ctx.acquisitionByInvoiceOrder.get(order.getPartnerOrderId());

        // 信息流以收购登记单为准：时间、地点、出售者及联系方式、报废产品、数量、价格都在单上
        if (acquisition != null) {
            AcquisitionLedgerRespVO row = new AcquisitionLedgerRespVO();
            row.setTradeTime(acquisition.getTradeTime() != null ? acquisition.getTradeTime()
                    : (order.getInvoiceDate() != null ? order.getInvoiceDate() : order.getCreateTime()));
            row.setTradeAddress(StrUtil.blankToDefault(acquisition.getTradeAddress(),
                    payee != null ? payee.getAddress() : null));
            row.setSellerName(StrUtil.blankToDefault(acquisition.getSellerName(),
                    payee != null ? payee.getName() : null));
            row.setSellerMobile(StrUtil.blankToDefault(acquisition.getSellerMobile(),
                    payee != null ? payee.getMobile() : null));
            row.setProductName(acquisition.getCategoryName());
            row.setSpecification(acquisition.getSpecification());
            row.setQuantity(acquisition.getQuantity());
            row.setUnit(acquisition.getUnit());
            row.setUnitPrice(acquisition.getUnitPrice());
            row.setAmount(acquisition.getAmount());
            row.setInvoiceNo(order.getInvoiceNo());
            row.setPartnerOrderId(order.getPartnerOrderId());
            return Collections.singletonList(row);
        }

        List<AcquisitionLedgerRespVO> rows = new ArrayList<>();
        for (OrderItemDO item : ctx.itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList())) {
            AcquisitionLedgerRespVO row = new AcquisitionLedgerRespVO();
            row.setTradeTime(order.getInvoiceDate() != null ? order.getInvoiceDate() : order.getCreateTime());
            // 没有收购登记单时先用出售者登记地址兜底，不伪造现场地址
            row.setTradeAddress(payee != null ? payee.getAddress() : null);
            row.setSellerName(payee != null ? payee.getName() : null);
            row.setSellerMobile(payee != null ? payee.getMobile() : null);
            row.setProductName(item.getItemName());
            row.setSpecification(item.getSpecification());
            row.setQuantity(item.getQuantity());
            row.setUnit(item.getUnit());
            row.setUnitPrice(item.getUnitPrice());
            row.setAmount(item.getAmount());
            row.setInvoiceNo(order.getInvoiceNo());
            row.setPartnerOrderId(order.getPartnerOrderId());
            rows.add(row);
        }
        return rows;
    }

    private EvidencePackageWriter.PackageContent buildPackageContent(InvoiceOrderDO order, ChainContext ctx) {
        List<EvidencePackageWriter.EvidenceFileRef> files = new ArrayList<>();
        InvoiceDownloadDO download = ctx.downloadByOrder.get(order.getPartnerOrderId());
        if (download != null) {
            for (InvoiceFileDO file : ctx.filesByDownload.getOrDefault(download.getId(), Collections.emptyList())) {
                files.add(new EvidencePackageWriter.EvidenceFileRef(file.getFileName(), file.getFilePath()));
            }
        }
        return new EvidencePackageWriter.PackageContent(order.getPartnerOrderId(),
                assembleChain(order, ctx), buildLedgerRows(order, ctx), files);
    }

    private ChainContext buildContext(List<InvoiceOrderDO> orders) {
        ChainContext ctx = new ChainContext();
        if (CollUtil.isEmpty(orders)) {
            return ctx;
        }
        List<String> partnerOrderIds = orders.stream().map(InvoiceOrderDO::getPartnerOrderId)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        List<Long> orderIds = orders.stream().map(InvoiceOrderDO::getId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> payeeIds = orders.stream().map(InvoiceOrderDO::getPayeeId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        if (CollUtil.isNotEmpty(partnerOrderIds)) {
            for (IcbcAcquisitionDO acquisition : acquisitionMapper.selectListByInvoicePartnerOrderIds(partnerOrderIds)) {
                ctx.acquisitionByInvoiceOrder.put(acquisition.getInvoicePartnerOrderId(), acquisition);
            }
            for (IcbcEvidenceDO evidence : evidenceMapper.selectListByPartnerOrderIds(partnerOrderIds)) {
                ctx.evidenceByOrder.computeIfAbsent(evidence.getPartnerOrderId(), key -> new ArrayList<>())
                        .add(evidence);
            }
            for (PaymentOrderDO payment : paymentOrderMapper.selectList(
                    PaymentOrderDO::getPartnerOrderId, partnerOrderIds)) {
                ctx.paymentByOrder.put(payment.getPartnerOrderId(), payment);
            }
            for (RedInvoiceDO red : redInvoiceMapper.selectList(
                    RedInvoiceDO::getPartnerOrderId, partnerOrderIds)) {
                // 同一蓝票可能有多条（撤销后重冲），取 id 最大的一条
                RedInvoiceDO exists = ctx.redByOrder.get(red.getPartnerOrderId());
                if (exists == null || red.getId() > exists.getId()) {
                    ctx.redByOrder.put(red.getPartnerOrderId(), red);
                }
            }
            List<InvoiceDownloadDO> downloads = invoiceDownloadMapper.selectList(
                    InvoiceDownloadDO::getPartnerOrderId, partnerOrderIds);
            for (InvoiceDownloadDO download : downloads) {
                ctx.downloadByOrder.put(download.getPartnerOrderId(), download);
            }
            List<Long> downloadIds = downloads.stream().map(InvoiceDownloadDO::getId)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (CollUtil.isNotEmpty(downloadIds)) {
                for (InvoiceFileDO file : invoiceFileMapper.selectList(InvoiceFileDO::getDownloadId, downloadIds)) {
                    ctx.filesByDownload.computeIfAbsent(file.getDownloadId(), key -> new ArrayList<>()).add(file);
                }
            }
        }
        if (CollUtil.isNotEmpty(orderIds)) {
            for (OrderItemDO item : orderItemMapper.selectList(OrderItemDO::getOrderId, orderIds)) {
                ctx.itemsByOrderId.computeIfAbsent(item.getOrderId(), key -> new ArrayList<>()).add(item);
            }
        }
        if (CollUtil.isNotEmpty(payeeIds)) {
            for (PayeeInfoDO payee : payeeInfoMapper.selectByIds(payeeIds)) {
                ctx.payeeById.put(payee.getId(), payee);
            }
            // 每人的生效协议取最新一条（mapper 已按 id 倒序）
            for (IcbcFrameworkAgreementDO agreement : frameworkAgreementMapper.selectEffectiveByPayeeIds(payeeIds)) {
                ctx.agreementByPayee.putIfAbsent(agreement.getPayeeId(), agreement);
            }
        }
        return ctx;
    }

    private List<InvoiceOrderDO> resolveOrders(List<String> partnerOrderIds, LocalDateTime[] createTime) {
        if (CollUtil.isNotEmpty(partnerOrderIds)) {
            return invoiceOrderMapper.selectList(InvoiceOrderDO::getPartnerOrderId, partnerOrderIds);
        }
        LambdaQueryWrapper<InvoiceOrderDO> wrapper = new LambdaQueryWrapper<>();
        if (createTime != null && createTime.length == 2) {
            wrapper.between(InvoiceOrderDO::getCreateTime, createTime[0], createTime[1]);
        }
        wrapper.orderByAsc(InvoiceOrderDO::getId);
        return invoiceOrderMapper.selectList(wrapper);
    }

    private boolean isHttpUrl(String url) {
        return StrUtil.isNotBlank(url) && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private BigDecimal rate(int presentCount) {
        return BigDecimal.valueOf(presentCount * 100.0 / TOTAL_FLOW_COUNT).setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== VO 转换 ====================

    private void addImageSource(List<EvidenceSourceRespVO> sources, String title, String url,
                                LocalDateTime occurredTime) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        EvidenceSourceRespVO source = new EvidenceSourceRespVO();
        source.setSourceType("ACQUISITION");
        source.setTitle(title);
        source.setUrl(url);
        source.setOccurredTime(occurredTime);
        sources.add(source);
    }

    /**
     * 往某条流里挂一条证据来源。
     *
     * <p>框架收购协议的两份文书都用它：主文书（框架收购协议）与告知函各自成条。
     * <b>地址为空不成条</b>（#95 SP-2）：纸路径的告知函没有电子地址，`noticeFileUrl` 全仓只有
     * 签署回调会写，纸路径下它就是空的；硬挂一条永远没有地址的条目，等于在证据链上凭空多出
     * 一份「无法引用的文书」。ADR 0036 决策 3 要的是两份文书能**分别引用**，不是条数固定为二，
     * 所以哪份有地址就挂哪份，两份都没有（纸签且主文书也没扫描件）就不挂。
     */
    private void addSource(List<EvidenceSourceRespVO> sources, String sourceType, String title,
                           String ref, String url, LocalDateTime occurredTime) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        EvidenceSourceRespVO source = new EvidenceSourceRespVO();
        source.setSourceType(sourceType);
        source.setTitle(title);
        source.setRef(ref);
        source.setUrl(url);
        source.setOccurredTime(occurredTime);
        sources.add(source);
    }

    /**
     * 结算确认记录作为合同流的签署证据：已确认（含线下签字确认）的结算单，带快照哈希与签字件。
     */
    private void addSettlementConfirmationSource(List<EvidenceSourceRespVO> sources,
                                                 IcbcAcquisitionDO acquisition) {
        if (acquisition.getSettlementId() == null) {
            return;
        }
        IcbcSettlementDO settlement = settlementMapper.selectById(acquisition.getSettlementId());
        if (settlement == null || !SettlementConfirmStatusEnum.isConfirmed(settlement.getConfirmStatus())) {
            return;
        }
        boolean offline = SettlementConfirmStatusEnum.OFFLINE_CONFIRMED.getStatus()
                .equals(settlement.getConfirmStatus());
        EvidenceSourceRespVO source = new EvidenceSourceRespVO();
        source.setSourceType("SETTLEMENT_CONFIRMATION");
        source.setTitle(offline ? "结算线下签字确认书" : "结算确认记录");
        source.setRef(settlement.getSettlementNo() + "#" + StrUtil.blankToDefault(settlement.getConfirmHash(), ""));
        source.setUrl(settlement.getOfflineSignFileUrl());
        source.setOccurredTime(settlement.getConfirmTime());
        sources.add(source);
    }

    private EvidenceSourceRespVO toSource(IcbcEvidenceDO evidence) {
        EvidenceSourceRespVO source = new EvidenceSourceRespVO();
        source.setSourceType("EVIDENCE");
        source.setTitle(evidence.getTitle());
        source.setRef(IcbcEvidenceTypeEnum.ofCode(evidence.getEvidenceType())
                .map(IcbcEvidenceTypeEnum::getName).orElse(evidence.getEvidenceType()));
        source.setUrl(evidence.getFileUrl());
        source.setOccurredTime(evidence.getOccurredTime());
        return source;
    }

    private EvidenceRespVO toEvidenceRespVO(IcbcEvidenceDO evidence) {
        EvidenceRespVO vo = new EvidenceRespVO();
        vo.setId(evidence.getId());
        vo.setFlow(evidence.getFlow());
        EvidenceFlowEnum.ofCode(evidence.getFlow()).ifPresent(flow -> vo.setFlowName(flow.getName()));
        vo.setEvidenceType(evidence.getEvidenceType());
        IcbcEvidenceTypeEnum.ofCode(evidence.getEvidenceType()).ifPresent(type -> vo.setEvidenceTypeName(type.getName()));
        vo.setTitle(evidence.getTitle());
        vo.setFileUrl(evidence.getFileUrl());
        vo.setFileName(evidence.getFileName());
        vo.setOccurredTime(evidence.getOccurredTime());
        vo.setRemark(evidence.getRemark());
        vo.setCreateTime(evidence.getCreateTime());
        return vo;
    }

    /**
     * 一次装配所需的关联数据，批量场景下只查一次，避免逐票 N+1。
     */
    private static class ChainContext {
        private final Map<String, List<IcbcEvidenceDO>> evidenceByOrder = new HashMap<>();
        private final Map<String, IcbcAcquisitionDO> acquisitionByInvoiceOrder = new HashMap<>();
        private final Map<String, PaymentOrderDO> paymentByOrder = new HashMap<>();
        private final Map<String, InvoiceDownloadDO> downloadByOrder = new HashMap<>();
        private final Map<Long, List<InvoiceFileDO>> filesByDownload = new HashMap<>();
        private final Map<Long, List<OrderItemDO>> itemsByOrderId = new HashMap<>();
        private final Map<Long, PayeeInfoDO> payeeById = new HashMap<>();
        private final Map<Long, IcbcFrameworkAgreementDO> agreementByPayee = new HashMap<>();
        private final Map<String, RedInvoiceDO> redByOrder = new HashMap<>();
    }

}
