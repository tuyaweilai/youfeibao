package cn.iocoder.yudao.module.icbc.service.invoice.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationResultVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreCheckItemVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerInvoiceConfirmItemVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.InvoiceConfirmStageEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.service.invoice.AutoInvoiceApplicationService;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceApplicationService;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import cn.iocoder.yudao.module.icbc.service.token.PublicPageLinkBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 结算确认后自动预下单的实现（#106，ADR 0039）。
 *
 * <p><b>本企业开票参数</b>（开票人姓名 / 证件、应税行为发生地）是工行预下单的必输项，而自动预下单
 * 发生在自然人的手机上、那一刻没有开票员在场——所以这三项落在付方档案上（一租户一行）。
 * 三项不全时不发起，把「企业还没配好开票参数」如实回给自然人，而不是拿空值去求工行。
 */
@Slf4j
@Service
public class AutoInvoiceApplicationServiceImpl implements AutoInvoiceApplicationService {

    /** 公开确认页路径（后端自己输出工行表单 HTML） */
    private static final String CONFIRM_PAGE_PATH = "/icbc/public/invoice/confirm-page";

    /** 自动路径一律开普票：自然人出售者不抵扣进项，专票要开票员显式选（兜底入口仍然在） */
    private static final String AUTO_INVOICE_TYPE = "02";

    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private PayerInfoMapper payerInfoMapper;
    @Resource
    private InvoiceOrderService invoiceOrderService;
    @Resource
    private InvoiceApplicationService invoiceApplicationService;
    @Resource
    private PublicPageLinkBuilder publicPageLinkBuilder;

    /** 工行确认完成后回跳的地址前缀（工行要求必输） */
    @Value("${icbc.invoice.jump-url-base:}")
    private String jumpUrlBase;
    /** 未配回跳地址时退回自然人端入口 */
    @Value("${icbc.notify.seller-app-url:}")
    private String sellerAppUrl;

    @Override
    public void applyForSettlement(Long settlementId) {
        List<IcbcAcquisitionDO> acquisitions = acquisitionMapper.selectListBySettlementId(settlementId);
        for (IcbcAcquisitionDO acquisition : acquisitions) {
            try {
                applyOne(acquisition);
            } catch (RuntimeException e) {
                // 一张失败不拖累其他张，更不回滚已经落库的结算确认：原因在「开票信息确认」列表里给他
                log.warn("自动预下单单张失败 - acquisitionNo: {}", acquisition.getAcquisitionNo(), e);
            }
        }
    }

    private void applyOne(IcbcAcquisitionDO acquisition) {
        if (StrUtil.isNotBlank(acquisition.getInvoicePartnerOrderId())) {
            return; // 幂等：这一张已经发起过
        }
        if (StrUtil.isNotBlank(acquisition.getCancelReason())) {
            return; // 已作废的不发起
        }
        InvoiceApplicationApplyReqVO reqVO = buildApplyReq(acquisition);
        if (reqVO == null) {
            log.info("自动预下单跳过：本企业开票参数不齐 - acquisitionNo: {}", acquisition.getAcquisitionNo());
            return;
        }
        InvoiceApplicationResultVO result = invoiceApplicationService.apply(reqVO);
        if (result != null && Boolean.TRUE.equals(result.getSuccess())) {
            log.info("结算确认后自动预下单 - acquisitionNo: {}, partnerOrderId: {}",
                    acquisition.getAcquisitionNo(), result.getPartnerOrderId());
        } else {
            log.info("自动预下单未通过校验 - acquisitionNo: {}, message: {}",
                    acquisition.getAcquisitionNo(), result == null ? "无返回" : result.getMessage());
        }
    }

    @Override
    public List<SellerInvoiceConfirmItemVO> statusForSettlement(Long settlementId) {
        List<IcbcAcquisitionDO> acquisitions = acquisitionMapper.selectListBySettlementId(settlementId);
        List<SellerInvoiceConfirmItemVO> items = new ArrayList<>();
        for (IcbcAcquisitionDO acquisition : acquisitions) {
            if (StrUtil.isNotBlank(acquisition.getCancelReason())) {
                continue; // 作废的不进「我要确认几张」这一列
            }
            items.add(toItem(acquisition));
        }
        return items;
    }

    @Override
    public String confirmPageHtml(String partnerOrderId) {
        if (StrUtil.isBlank(partnerOrderId)) {
            return null;
        }
        InvoiceOrderDO order = invoiceOrderService.getOrderByPartnerOrderId(partnerOrderId);
        return order == null ? null : order.getConfirmPageHtml();
    }

    // ==================== 逐张整理 ====================

    private SellerInvoiceConfirmItemVO toItem(IcbcAcquisitionDO acquisition) {
        SellerInvoiceConfirmItemVO item = new SellerInvoiceConfirmItemVO();
        item.setAcquisitionId(acquisition.getId());
        item.setAcquisitionNo(acquisition.getAcquisitionNo());
        item.setCategoryName(acquisition.getCategoryName());
        item.setAmount(acquisition.getAmount());

        InvoiceOrderDO order = StrUtil.isBlank(acquisition.getInvoicePartnerOrderId())
                ? null : invoiceOrderService.getOrderByPartnerOrderId(acquisition.getInvoicePartnerOrderId());
        if (order == null) {
            // 还没发起：把「为什么还不能发起」按开票申请那一套校验结果如实回给他
            markBlocked(item, acquisition, acquireBlockMessage(acquisition));
            return item;
        }
        if (order.getConfirmStatus() != null && order.getConfirmStatus() >= 1
                || PreInvoiceStatusEnum.SUCCESS.getStatus().equals(order.getPreInvoiceStatus())
                || InvoiceIssueStatusEnum.isIssued(order.getInvoiceStatus())) {
            mark(item, InvoiceConfirmStageEnum.CONFIRMED, "这一张已经确认过了，等企业付款");
            return item;
        }
        if (PreInvoiceStatusEnum.FAILED.getStatus().equals(order.getPreInvoiceStatus())) {
            markBlocked(item, acquisition, "工行预开票失败，平台已记录，企业会重新发起或联系工行核实");
            return item;
        }
        mark(item, InvoiceConfirmStageEnum.WAITING_CONFIRM, "请点开工行页面，核对开票信息并确认");
        fillConfirmPage(item, order);
        return item;
    }

    private void mark(SellerInvoiceConfirmItemVO item, InvoiceConfirmStageEnum stage, String message) {
        item.setStage(stage.getCode());
        item.setStageName(stage.getName());
        item.setMessage(message);
        item.setConfirmPageAvailable(false);
    }

    private void markBlocked(SellerInvoiceConfirmItemVO item, IcbcAcquisitionDO acquisition, String message) {
        mark(item, InvoiceConfirmStageEnum.BLOCKED, message);
        item.setFailures(preCheckFailures(acquisition));
    }

    /**
     * 还没发起时的原因：先看本企业开票参数齐不齐（这是自动路径特有的前置），
     * 再让开票申请的前置校验给出逐项原因。
     */
    private String acquireBlockMessage(IcbcAcquisitionDO acquisition) {
        String missing = missingInvoiceParam();
        if (missing != null) {
            return "回收企业还没配好开票参数（缺：" + missing + "），配好后即可确认开票";
        }
        if (StrUtil.isBlank(acquisition.getInvoicePartnerOrderId())) {
            return "这一张还没发起开票申请，原因见下；企业可先在后台发起";
        }
        return "暂时无法发起开票申请";
    }

    private List<InvoicePreCheckItemVO> preCheckFailures(IcbcAcquisitionDO acquisition) {
        InvoicePreCheckRespVO preCheck = invoiceApplicationService.preCheck(acquisition.getId(), AUTO_INVOICE_TYPE);
        List<InvoicePreCheckItemVO> failures = new ArrayList<>();
        for (InvoicePreCheckItemVO check : preCheck.getItems()) {
            if (!Boolean.TRUE.equals(check.getPassed())) {
                failures.add(check);
            }
        }
        return failures;
    }

    private void fillConfirmPage(SellerInvoiceConfirmItemVO item, InvoiceOrderDO order) {
        if (StrUtil.isBlank(order.getConfirmPageHtml())) {
            item.setConfirmPageAvailable(false);
            item.setMessage("确认页已过期或未留存，请让企业重新发起这一张开票申请");
            return;
        }
        String url = publicPageLinkBuilder.buildOrderPageLink(CONFIRM_PAGE_PATH, order.getPartnerOrderId(),
                PublicTokenPurposeEnum.INVOICE_CONFIRM_PAGE);
        if (url == null) {
            item.setConfirmPageAvailable(false);
            item.setMessage("平台未配置公开访问地址（icbc.public-base-url），暂时打不开确认页，请联系企业");
            return;
        }
        item.setConfirmPageUrl(url);
        item.setConfirmPageAvailable(true);
    }

    // ==================== 本企业开票参数 ====================

    /**
     * 组装自动开票申请入参；本企业开票参数或回跳地址不全时返回 {@code null}。
     */
    private InvoiceApplicationApplyReqVO buildApplyReq(IcbcAcquisitionDO acquisition) {
        PayerInfoDO payer = currentPayer();
        String missing = missingInvoiceParam(payer);
        String jump = resolveJumpUrlBase();
        if (missing != null || jump == null) {
            return null;
        }
        InvoiceApplicationApplyReqVO reqVO = new InvoiceApplicationApplyReqVO();
        reqVO.setAcquisitionId(acquisition.getId());
        reqVO.setInvoiceType(AUTO_INVOICE_TYPE);
        reqVO.setAreaCode(payer.getAreaCode().trim());
        reqVO.setDrawerName(payer.getDrawerName().trim());
        reqVO.setDrawerCardNumber(payer.getDrawerCardNumber().trim());
        reqVO.setJumpUrlBase(jump);
        return reqVO;
    }

    /**
     * 缺哪一项（用于把「为什么还不能确认」说清楚）；齐了返回 {@code null}。
     */
    private String missingInvoiceParam() {
        return missingInvoiceParam(currentPayer());
    }

    private String missingInvoiceParam(PayerInfoDO payer) {
        if (payer == null) {
            return "付方档案";
        }
        List<String> missing = new ArrayList<>();
        if (StrUtil.isBlank(payer.getDrawerName())) {
            missing.add("开票人姓名");
        }
        if (StrUtil.isBlank(payer.getDrawerCardNumber())) {
            missing.add("开票人证件号码");
        }
        if (StrUtil.isBlank(payer.getAreaCode())) {
            missing.add("应税行为发生地");
        }
        return missing.isEmpty() ? null : String.join("、", missing);
    }

    /**
     * 回收企业 = 租户 = 开票主体：正常情况下每个租户只有一份付方档案，取最早的一份保证确定性。
     */
    private PayerInfoDO currentPayer() {
        List<PayerInfoDO> payers = payerInfoMapper.selectList(
                new LambdaQueryWrapperX<PayerInfoDO>().orderByAsc(PayerInfoDO::getId));
        return payers == null || payers.isEmpty() ? null : payers.get(0);
    }

    private String resolveJumpUrlBase() {
        if (StrUtil.isNotBlank(jumpUrlBase)) {
            return jumpUrlBase.trim();
        }
        return StrUtil.isBlank(sellerAppUrl) ? null : sellerAppUrl.trim();
    }

}
