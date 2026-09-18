package cn.iocoder.yudao.module.icbc.service.invoice.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.IcbcTaxMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.service.acquisition.AcquisitionService;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceApplicationService;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 开票申请 Service 实现。
 *
 * <p>把「已登记的收购」变成「自然人确认页面」：先校验能不能开，通过后从收购单、
 * 出售者档案、回收企业付方档案里推导出完整的工行预下单报文，经适配层下单。
 * 开票员因此只需要选收购单与票种，不需要懂工行报文的任何一个字段。
 */
@Slf4j
@Service
@Validated
public class InvoiceApplicationServiceImpl implements InvoiceApplicationService {

    private static final String CHECK_TENANT_QUALIFICATION = "TENANT_QUALIFICATION";
    private static final String CHECK_PAYER_INFO = "PAYER_INFO";
    private static final String CHECK_SELLER_AVAILABLE = "SELLER_AVAILABLE";
    private static final String CHECK_TAX_METHOD_INVOICE_TYPE = "TAX_METHOD_INVOICE_TYPE";
    private static final String CHECK_GOODS_CODE_CONFIGURED = "GOODS_CODE_CONFIGURED";
    private static final String CHECK_ACQUISITION_ELEMENTS = "ACQUISITION_ELEMENTS";
    private static final String CHECK_ACQUISITION_STATUS = "ACQUISITION_STATUS";

    private static final String REMEDY_QUALIFICATION = "在「租户开票就绪 · 三层资质」录入税务侧反向开票资格、行业侧资质与公安侧备案，并由平台运营核实";
    private static final String REMEDY_PAYER = "在「付方档案」补全企业名称、纳税人识别号与合作方付方编号";
    private static final String REMEDY_SELLER = "在「出售者建档」完成实人认证、收方入驻、框架收购协议与首次授权";
    private static final String REMEDY_TAX_METHOD = "把票种改为增值税普通发票（02），或在「编码配置」把该品类的计税方法改为一般计税";
    private static final String REMEDY_GOODS_CODE = "在「租户开票就绪 · 编码配置」为该品类配置商品和服务税收分类合并编码";
    private static final String REMEDY_ELEMENTS = "在「收购登记」补齐缺失要件后重新发起";
    private static final String REMEDY_STATUS = "仅「已登记」的收购单可以发起开票申请";

    @Resource
    private AcquisitionService acquisitionService;
    @Resource
    private InvoiceOrderService invoiceOrderService;
    @Resource
    private IcbcQualificationService qualificationService;
    @Resource
    private SellerOnboardingService sellerOnboardingService;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private PayerInfoMapper payerInfoMapper;

    // ==================== 发起前校验 ====================

    @Override
    public InvoicePreCheckRespVO preCheck(Long acquisitionId, String invoiceType) {
        IcbcAcquisitionDO acquisition = acquisitionService.getAcquisition(acquisitionId);
        List<InvoicePreCheckItemVO> items = runPreChecks(acquisition, invoiceType);
        InvoicePreCheckRespVO resp = new InvoicePreCheckRespVO();
        resp.setAcquisitionId(acquisition.getId());
        resp.setAcquisitionNo(acquisition.getAcquisitionNo());
        resp.setItems(items);
        resp.setAllPassed(items.stream().allMatch(item -> Boolean.TRUE.equals(item.getPassed())));
        return resp;
    }

    private List<InvoicePreCheckItemVO> runPreChecks(IcbcAcquisitionDO acquisition, String invoiceType) {
        List<InvoicePreCheckItemVO> items = new ArrayList<>();
        items.add(checkTenantQualification());
        items.add(checkPayerInfo());
        items.add(checkSellerAvailable(acquisition));
        items.add(checkTaxMethodInvoiceType(acquisition, invoiceType));
        items.add(checkGoodsCodeConfigured(acquisition));
        items.add(checkAcquisitionElements(acquisition));
        items.add(checkAcquisitionStatus(acquisition));
        return items;
    }

    private InvoicePreCheckItemVO checkTenantQualification() {
        boolean ready = false;
        try {
            ready = qualificationService.isTenantReady();
        } catch (RuntimeException e) {
            log.warn("校验租户开票就绪时异常", e);
        }
        return item(CHECK_TENANT_QUALIFICATION, "租户三层资质", ready,
                ready ? "税务侧 / 行业侧 / 公安侧三层资质均有效"
                        : "租户三层资质不齐或已失效，该租户当前不能开票",
                REMEDY_QUALIFICATION);
    }

    private InvoicePreCheckItemVO checkPayerInfo() {
        PayerInfoDO payer = getPayer();
        boolean ready = payer != null && StrUtil.isNotBlank(payer.getTaxNo())
                && StrUtil.isNotBlank(payer.getPartnerPayerId());
        return item(CHECK_PAYER_INFO, "回收企业付方档案", ready,
                ready ? "付方档案已就绪" : "回收企业付方档案缺少纳税人识别号或合作方付方编号",
                REMEDY_PAYER);
    }

    private InvoicePreCheckItemVO checkSellerAvailable(IcbcAcquisitionDO acquisition) {
        boolean ready;
        String message;
        try {
            sellerOnboardingService.assertReadyForInvoice(acquisition.getPayeeId());
            ready = true;
            message = "出售者建档已完成";
        } catch (ServiceException e) {
            ready = false;
            message = e.getMessage();
        } catch (RuntimeException e) {
            ready = false;
            message = "出售者状态校验失败：" + e.getMessage();
        }
        return item(CHECK_SELLER_AVAILABLE, "出售者状态", ready, message, REMEDY_SELLER);
    }

    /**
     * 简易计税只能开普票；一般计税（或未配置计税方法，兼容历史数据）可以开专票。
     */
    private InvoicePreCheckItemVO checkTaxMethodInvoiceType(IcbcAcquisitionDO acquisition, String invoiceType) {
        boolean specialInvoice = "01".equals(invoiceType);
        boolean simple = IcbcTaxMethodEnum.isSimple(acquisition.getTaxMethod());
        boolean passed = !(specialInvoice && simple);
        return item(CHECK_TAX_METHOD_INVOICE_TYPE, "票种与计税方法", passed,
                passed ? "票种与计税方法匹配"
                        : "该品类为简易计税，只能开具增值税普通发票，不能开专票",
                REMEDY_TAX_METHOD);
    }

    private InvoicePreCheckItemVO checkGoodsCodeConfigured(IcbcAcquisitionDO acquisition) {
        boolean configured = StrUtil.isNotBlank(acquisition.getMergedCode());
        return item(CHECK_GOODS_CODE_CONFIGURED, "品类税收分类编码", configured,
                configured ? "已配置商品和服务税收分类合并编码" : "该品类未配置商品和服务税收分类合并编码，开不出票",
                REMEDY_GOODS_CODE);
    }

    private InvoicePreCheckItemVO checkAcquisitionElements(IcbcAcquisitionDO acquisition) {
        List<String> missing = new ArrayList<>();
        PayeeInfoDO payee = payeeInfoMapper.selectById(acquisition.getPayeeId());
        if (payee == null) {
            missing.add("出售者档案");
        } else {
            if (StrUtil.isBlank(payee.getName())) {
                missing.add("出售者姓名");
            }
            if (StrUtil.isBlank(payee.getIdCardNo())) {
                missing.add("出售者身份证");
            }
            if (StrUtil.isBlank(payee.getMobile())) {
                missing.add("出售者手机号");
            }
            if (StrUtil.isBlank(payee.getAddress())) {
                missing.add("出售者地址");
            }
        }
        if (acquisition.getGoodsConfigId() == null) {
            missing.add("品类");
        }
        if (acquisition.getQuantity() == null || acquisition.getQuantity().signum() <= 0) {
            missing.add("数量");
        }
        if (acquisition.getAmount() == null || acquisition.getAmount().signum() <= 0) {
            missing.add("金额");
        }
        if (acquisition.getTaxRate() == null) {
            missing.add("税率");
        }
        if (StrUtil.isBlank(acquisition.getWeightTicketNo())
                && StrUtil.isBlank(acquisition.getWeightTicketImageUrl())) {
            missing.add("磅单");
        }
        boolean passed = missing.isEmpty();
        return item(CHECK_ACQUISITION_ELEMENTS, "收购单要件齐备", passed,
                passed ? "收购单要件齐备" : "缺少：" + String.join("、", missing),
                REMEDY_ELEMENTS);
    }

    private InvoicePreCheckItemVO checkAcquisitionStatus(IcbcAcquisitionDO acquisition) {
        if (StrUtil.isNotBlank(acquisition.getInvoicePartnerOrderId())) {
            return item(CHECK_ACQUISITION_STATUS, "收购单状态", false,
                    "该收购单已发起过开票申请，合作方订单号：" + acquisition.getInvoicePartnerOrderId(),
                    "在「开票申请」按合作方订单号查询其自然人确认与预开票状态");
        }
        boolean registered = AcquisitionStatusEnum.REGISTERED.getStatus().equals(acquisition.getStatus());
        return item(CHECK_ACQUISITION_STATUS, "收购单状态", registered,
                registered ? "收购单已登记，可以发起开票申请"
                        : "收购单当前状态为「" + statusName(acquisition.getStatus()) + "」，不能发起开票申请",
                REMEDY_STATUS);
    }

    private String statusName(Integer status) {
        return AcquisitionStatusEnum.ofStatus(status).map(AcquisitionStatusEnum::getName).orElse("未知");
    }

    private InvoicePreCheckItemVO item(String code, String name, boolean passed, String message, String remedy) {
        return InvoicePreCheckItemVO.builder()
                .code(code).name(name).passed(passed)
                .message(message).remedy(passed ? null : remedy)
                .build();
    }

    // ==================== 发起开票申请 ====================

    @Override
    public InvoiceApplicationResultVO apply(InvoiceApplicationApplyReqVO reqVO) {
        return doApply(reqVO, reqVO.getAcquisitionId());
    }

    @Override
    public List<InvoiceApplicationResultVO> applyBatch(InvoiceApplicationBatchReqVO reqVO) {
        List<InvoiceApplicationResultVO> results = new ArrayList<>();
        for (Long acquisitionId : reqVO.getAcquisitionIds()) {
            try {
                results.add(doApply(reqVO, acquisitionId));
            } catch (RuntimeException e) {
                // 单笔异常不拖垮整批：逐笔可见失败原因
                log.error("开票申请单笔异常 - acquisitionId: {}", acquisitionId, e);
                InvoiceApplicationResultVO result = new InvoiceApplicationResultVO();
                result.setAcquisitionId(acquisitionId);
                result.setSuccess(false);
                result.setDuplicate(false);
                result.setMessage(e.getMessage());
                results.add(result);
            }
        }
        return results;
    }

    private InvoiceApplicationResultVO doApply(InvoiceApplicationBaseReqVO base, Long acquisitionId) {
        InvoiceApplicationResultVO result = new InvoiceApplicationResultVO();
        result.setAcquisitionId(acquisitionId);
        result.setDuplicate(false);

        // 1. 取收购单
        IcbcAcquisitionDO acquisition;
        try {
            acquisition = acquisitionService.getAcquisition(acquisitionId);
        } catch (ServiceException e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
        result.setAcquisitionNo(acquisition.getAcquisitionNo());

        // 2. 幂等：同一收购单重复发起不再下单，返回既有业务
        if (StrUtil.isNotBlank(acquisition.getInvoicePartnerOrderId())) {
            InvoiceOrderDO existing = invoiceOrderService.getOrderByPartnerOrderId(
                    acquisition.getInvoicePartnerOrderId());
            result.setSuccess(true);
            result.setDuplicate(true);
            result.setPartnerOrderId(acquisition.getInvoicePartnerOrderId());
            result.setMessage("该收购单已发起过开票申请，未重复下单");
            fillOrderFields(result, existing);
            return result;
        }

        // 3. 发起前校验：不通过时逐条给出原因与补齐方式
        List<InvoicePreCheckItemVO> failures = runPreChecks(acquisition, base.getInvoiceType()).stream()
                .filter(check -> !Boolean.TRUE.equals(check.getPassed()))
                .collect(Collectors.toList());
        if (!failures.isEmpty()) {
            result.setSuccess(false);
            result.setFailures(failures);
            result.setMessage("开票申请校验未通过");
            return result;
        }

        // 4. 经适配层预下单，取得自然人确认页面
        try {
            PayerInfoDO payer = getPayer();
            PayeeInfoDO payee = payeeInfoMapper.selectById(acquisition.getPayeeId());
            InvoicePreOrderReqVO preOrderReq = buildPreOrderReq(acquisition, payee, payer, base);
            InvoicePreOrderRespVO resp = invoiceOrderService.createPreOrder(preOrderReq);
            // 5. 挂回收购单并推进为「待付款」
            invoiceOrderService.bindAcquisition(resp.getPartnerOrderId(), acquisition.getId(),
                    acquisition.getPayeeId(), payer.getId());
            acquisitionService.linkInvoice(acquisition.getId(), resp.getPartnerOrderId());

            result.setSuccess(true);
            result.setPartnerOrderId(resp.getPartnerOrderId());
            result.setOrderNo(resp.getOrderNo());
            result.setConfirmPageHtml(resp.getRedirectUrl());
            result.setConfirmStatus(InvoiceConfirmStatusEnum.NOT_CONFIRMED.getStatus());
            result.setPreInvoiceStatus(PreInvoiceStatusEnum.IN_PROGRESS.getStatus());
            result.setOrderStatus(0);
            result.setMessage(StrUtil.blankToDefault(resp.getReturnMsg(), "已取得自然人确认页面"));
        } catch (ServiceException e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    private void fillOrderFields(InvoiceApplicationResultVO result, InvoiceOrderDO order) {
        if (order == null) {
            return;
        }
        result.setOrderNo(order.getOrderNo());
        result.setConfirmStatus(order.getConfirmStatus());
        result.setPreInvoiceStatus(order.getPreInvoiceStatus());
        result.setOrderStatus(order.getOrderStatus());
    }

    private PayerInfoDO getPayer() {
        // 回收企业 = 租户 = 开票主体，正常情况下每个租户只有一份付方档案；按 id 取最早的一份，保证确定性。
        List<PayerInfoDO> payers = payerInfoMapper.selectList(
                new LambdaQueryWrapperX<PayerInfoDO>().orderByAsc(PayerInfoDO::getId));
        return payers == null || payers.isEmpty() ? null : payers.get(0);
    }

    /**
     * 从收购单、出售者档案、付方档案推导完整的工行预下单报文。
     */
    private InvoicePreOrderReqVO buildPreOrderReq(IcbcAcquisitionDO acquisition, PayeeInfoDO payee,
                                                  PayerInfoDO payer, InvoiceApplicationBaseReqVO base) {
        InvoicePreOrderReqVO req = new InvoicePreOrderReqVO();
        req.setOutOrderId(acquisition.getAcquisitionNo());
        req.setOutVendorId(payer.getPartnerPayerId());
        req.setOutUserId(payee.getPartnerPayeeId());
        req.setInvoiceType(base.getInvoiceType());
        req.setOrderAmount(acquisition.getAmount());
        req.setSpecificElements("24");
        req.setBuyerInvTypeCode("04");
        req.setNaturalPersonName(payee.getName());
        req.setCardType("111");
        req.setCardNumber(payee.getIdCardNo());
        req.setSellerAddress(StrUtil.blankToDefault(payee.getAddress(), acquisition.getTradeAddress()));
        req.setSellerTelephone(StrUtil.blankToDefault(payee.getMobile(), acquisition.getSellerMobile()));
        req.setTaxpayerNo(payer.getTaxNo());
        req.setTaxpayerName(payer.getName());
        req.setDrawerName(base.getDrawerName());
        req.setDrawerCardType(StrUtil.blankToDefault(base.getDrawerCardType(), "111"));
        req.setDrawerCardNumber(base.getDrawerCardNumber());
        req.setAreaCode(base.getAreaCode());
        req.setMac(StrUtil.blankToDefault(base.getMac(), "00:00:00:00:00:00"));
        req.setTaxRate(acquisition.getTaxRate());
        req.setPayChannel("05");
        req.setIitProject("1");
        req.setCurrency("001");
        req.setPayJumpUrl(base.getJumpUrlBase() + "/icbc/pay/return");
        req.setInvoiceNotifyUrl(base.getJumpUrlBase() + "/admin-api/icbc/callback/notify");
        req.setInvoiceJumpUrl(base.getJumpUrlBase() + "/icbc/invoice/confirmed");
        req.setNotes(base.getNotes());
        req.setGoodsInfo(Collections.singletonList(buildGoodsInfo(acquisition)));
        return req;
    }

    private InvoicePreOrderReqVO.GoodsInfoVO buildGoodsInfo(IcbcAcquisitionDO acquisition) {
        InvoicePreOrderReqVO.GoodsInfoVO goods = new InvoicePreOrderReqVO.GoodsInfoVO();
        goods.setGoodsSeqno("1");
        goods.setProjectName(acquisition.getCategoryName());
        goods.setGoodsNum(acquisition.getQuantity());
        goods.setGoodsAmt(acquisition.getAmount());
        goods.setPrice(resolveUnitPrice(acquisition));
        goods.setUnits(acquisition.getUnit());
        goods.setTaxRate(acquisition.getTaxRate());
        goods.setMergedCode(acquisition.getMergedCode());
        return goods;
    }

    private BigDecimal resolveUnitPrice(IcbcAcquisitionDO acquisition) {
        if (acquisition.getUnitPrice() != null) {
            return acquisition.getUnitPrice();
        }
        BigDecimal quantity = acquisition.getQuantity();
        if (quantity == null || quantity.signum() == 0) {
            return null;
        }
        return acquisition.getAmount().divide(quantity, 2, RoundingMode.HALF_UP);
    }
}
