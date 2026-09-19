package cn.iocoder.yudao.module.icbc.service.invoice.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreOrderReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreOrderRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.OrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.OrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.enums.IcbcTaxMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.IcbcTaxPaymentMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceQueryReq;
import cn.iocoder.yudao.module.icbc.gateway.model.PreOrderGoods;
import cn.iocoder.yudao.module.icbc.gateway.model.PreOrderReq;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import cn.iocoder.yudao.module.icbc.util.AmountUtils;
import cn.iocoder.yudao.module.icbc.util.IcbcTimeUtils;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 工行反向开票订单 Service 实现类
 *
 * @author 芋道源码
 */
@Slf4j
@Service
public class InvoiceOrderServiceImpl implements InvoiceOrderService {

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    
    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private RedInvoiceMapper redInvoiceMapper;
    
    @Resource
    private IcbcGateway icbcGateway;

    @Resource
    private IcbcQualificationService qualificationService;

    @Resource
    private IcbcGoodsConfigService goodsConfigService;

    @Resource
    private SellerOnboardingService sellerOnboardingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoicePreOrderRespVO createPreOrder(InvoicePreOrderReqVO createReqVO) {
        // 1. 租户开票就绪校验：任一层资质失效即冻结开票（入口先拦）
        if (!qualificationService.isTenantReady()) {
            throw exception(ErrorCodeConstants.TENANT_NOT_READY);
        }

        // 1.1 出售者建档门禁：审核未通过 / 未完成建档的出售者不能用于开票
        sellerOnboardingService.assertReadyForInvoiceByOutUserId(createReqVO.getOutUserId());

        // 2. 参数校验
        validatePreOrderRequest(createReqVO);
        
        // 2. 幂等：同一合作方订单号已发起过开票申请时不再下单，直接返回既有订单
        InvoiceOrderDO existingOrder = invoiceOrderMapper.selectByPartnerOrderId(createReqVO.getOutOrderId());
        if (existingOrder != null) {
            return buildIdempotentResponse(existingOrder);
        }
        
        // 3. 生成订单号
        String orderNo = generateOrderNo();
        
        // 4. 创建订单记录
        InvoiceOrderDO order = createOrderFromRequest(createReqVO, orderNo);
        try {
            invoiceOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 并发下同一合作方订单号可能在预检之后才落库，靠唯一约束兜底；
            // 命中则视为同一笔业务，返回既有订单，不再下第二单。
            InvoiceOrderDO concurrent = invoiceOrderMapper.selectByPartnerOrderId(createReqVO.getOutOrderId());
            if (concurrent != null) {
                return buildIdempotentResponse(concurrent);
            }
            throw e;
        }
        
        // 5. 创建商品明细记录
        List<OrderItemDO> orderItems = createOrderItemsFromRequest(createReqVO, order.getId(), orderNo);
        for (OrderItemDO item : orderItems) {
            orderItemMapper.insert(item);
        }
        
        // 6. 经端口调用工行预下单（UI 接口，返回自然人确认页面表单）
        IcbcGatewayResult<IcbcPage> preOrderResult = icbcGateway.submitPreOrder(toPreOrderReq(createReqVO));
        if (!preOrderResult.isSuccess()) {
            throw exception(ErrorCodeConstants.ICBC_API_CALL_FAILED);
        }
        String redirectUrl = preOrderResult.getData().getFormHtml();

        // 7. 构造响应
        InvoicePreOrderRespVO response = new InvoicePreOrderRespVO();
        response.setReturnCode(0);
        response.setReturnMsg("成功");
        response.setRedirectUrl(redirectUrl);
        response.setOrderNo(orderNo);
        response.setPartnerOrderId(createReqVO.getOutOrderId());

        log.info("反向开票预下单成功 - orderNo: {}, partnerOrderId: {}", orderNo, createReqVO.getOutOrderId());
        return response;
    }

    @Override
    public InvoiceQueryRespVO queryInvoiceInfo(InvoiceQueryReqVO queryReqVO) {
        // 1. 查询本地订单信息
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(queryReqVO.getOutOrderId());
        if (order == null) {
            throw exception(ErrorCodeConstants.INVOICE_ORDER_NOT_EXISTS);
        }

        // 2. 经适配层调用工行预查询，取回自然人确认状态与五条状态线；查不到工行最新状态时退回本地快照
        IcbcGatewayResult<InvoiceInfo> result = icbcGateway.queryInvoiceInfo(InvoiceQueryReq.builder()
                .outOrderId(order.getPartnerOrderId())
                .outUserId(order.getPayeeNo())
                .outVendorId(order.getPayerNo())
                .build());
        if (result.isSuccess() && result.getData() != null) {
            applyInvoiceInfo(order.getPartnerOrderId(), result.getData());
            order = invoiceOrderMapper.selectById(order.getId());
        }
        log.info("反向开票查询成功 - orderNo: {}, partnerOrderId: {}, confirmStatus: {}, preInvoiceStatus: {}, invoiceStatus: {}, taxStatus: {}, uploadStatus: {}",
                order.getOrderNo(), order.getPartnerOrderId(), order.getConfirmStatus(), order.getPreInvoiceStatus(),
                order.getInvoiceStatus(), order.getTaxStatus(), order.getUploadStatus());
        return buildQueryResponse(order);
    }

    private InvoiceQueryRespVO buildQueryResponse(InvoiceOrderDO order) {
        InvoiceQueryRespVO response = new InvoiceQueryRespVO();
        response.setReturnCode(0);
        response.setReturnMsg("成功");
        response.setOrderNo(order.getOrderNo());
        response.setPartnerOrderId(order.getPartnerOrderId());
        response.setAcquisitionId(order.getAcquisitionId());
        response.setOrderStatus(order.getOrderStatus());
        response.setInvoiceStatus(order.getInvoiceStatus());
        response.setInvoiceStatusName(InvoiceIssueStatusEnum.nameOf(order.getInvoiceStatus()));
        response.setPaymentStatus(order.getPaymentStatus());
        response.setTaxStatus(order.getTaxStatus());
        response.setTaxStatusName(TaxStatusEnum.nameOf(order.getTaxStatus()));
        response.setUploadStatus(order.getUploadStatus());
        response.setUploadStatusName(UploadStatusEnum.nameOf(order.getUploadStatus()));
        response.setConfirmStatus(order.getConfirmStatus());
        response.setPreInvoiceStatus(order.getPreInvoiceStatus());
        response.setInvoiceNo(order.getInvoiceNo());
        response.setInvoiceCode(order.getInvoiceCode());
        response.setInvoiceDate(order.getInvoiceDate());
        response.setInvoiceAmount(order.getInvoiceAmount());
        response.setTaxAmount(order.getTaxAmount());
        response.setTaxRealAmount(order.getTaxRealAmount());
        response.setTaxTime(order.getTaxTime());
        response.setTaxPaymentMethod(order.getTaxPaymentMethod());
        response.setTaxPaymentMethodName(IcbcTaxPaymentMethodEnum.nameOf(order.getTaxPaymentMethod()));
        response.setTaxVoucherNo(order.getTaxVoucherNo());
        fillRedFields(response, order);
        response.setNextAction(resolveNextAction(order));
        return response;
    }

    /**
     * 把该蓝票最近一次红冲记录带进查询响应：红冲与取消后状态同步可见。
     */
    private void fillRedFields(InvoiceQueryRespVO response, InvoiceOrderDO order) {
        RedInvoiceDO red = redInvoiceMapper.selectLatestByPartnerOrderId(order.getPartnerOrderId());
        if (red == null) {
            return;
        }
        response.setRedSerialNo(red.getRedOffsetNo());
        response.setRedOffsetStatus(red.getRedOffsetStatus());
        response.setRedOffsetStatusName(RedOffsetStatusEnum.nameOf(red.getRedOffsetStatus()));
        response.setRedInvoiceNo(red.getRedInvoiceNo());
        response.setRedInvoiceDate(red.getRedInvoiceDate());
    }

    /**
     * 把工行通知 / 预查询结果收敛回本地订单：自然人确认、预开票、开票、缴税、上传
     * <strong>五条状态线各自独立更新</strong>，任一条线上送新值就更新那一条，不互相覆盖。
     *
     * <p>通知（{@code notifyType=01/03/04/05}）与主动预查询<strong>调用同一个方法</strong>，
     * 所以两侧得到同一份状态；乱序到达的旧通知、重复到达的通知与通知早于本地数据落库三种情况
     * 都能收敛：
     * <ul>
     *   <li>重复：状态写成同一值，无副作用；</li>
     *   <li>乱序：已结清的成功态（已开票 / 缴税成功 / 上传成功）不被旧的进行中态回退；</li>
     *   <li>早到：查不到业务单时抛 {@link ErrorCodeConstants#CALLBACK_BUSINESS_NOT_EXISTS}，
     *       通知落为失败但保留记录，数据落库后可重放。</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyInvoiceInfo(String partnerOrderId, InvoiceInfo info) {
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (order == null) {
            // 通知早于平台数据落库：抛业务异常，让通知落为失败、数据落库后可重放
            throw exception(ErrorCodeConstants.CALLBACK_BUSINESS_NOT_EXISTS);
        }
        InvoiceOrderDO update = new InvoiceOrderDO();
        update.setId(order.getId());
        boolean changed = false;

        // 1. 自然人确认与预开票状态：两条线独立更新，未上送的不覆盖
        Integer confirm = order.getConfirmStatus();
        Integer preInvoice = order.getPreInvoiceStatus();
        if (StrUtil.isNotBlank(info.getConfirmStatus())) {
            confirm = InvoiceConfirmStatusEnum.toStatus(info.getConfirmStatus());
            update.setConfirmStatus(confirm);
            changed = true;
        }
        if (StrUtil.isNotBlank(info.getInvoiceStatus())) {
            preInvoice = PreInvoiceStatusEnum.toStatus(info.getInvoiceStatus());
            update.setPreInvoiceStatus(preInvoice);
            changed = true;
        }

        // 2. 开票状态：拿到发票号码即已开票；预开票失败即开票失败；已付款但未出票为开票中
        Integer issue = order.getInvoiceStatus();
        Integer newIssue = resolveIssueStatus(order, info, preInvoice);
        if (newIssue != null && shouldApplyIssue(issue, newIssue)) {
            issue = newIssue;
            update.setInvoiceStatus(issue);
            changed = true;
        }

        // 3. 缴税状态：独立收敛，已结清后不回退
        Integer tax = order.getTaxStatus();
        Integer newTax = TaxStatusEnum.toStatus(info.getTaxStatus());
        if (newTax != null && shouldApplyTax(tax, newTax)) {
            tax = newTax;
            update.setTaxStatus(tax);
            changed = true;
        }

        // 4. 上传状态：独立收敛，上传成功后不回退
        Integer upload = order.getUploadStatus();
        Integer newUpload = UploadStatusEnum.toStatus(info.getUploadStatus());
        if (newUpload != null && shouldApplyUpload(upload, newUpload)) {
            upload = newUpload;
            update.setUploadStatus(upload);
            changed = true;
        }

        // 5. 发票与缴税字段
        applyInvoiceFields(update, order, info, issue);
        applyTaxFields(update, info);

        // 6. 订单状态由各条状态线推出，且不回退（已取消不碰）。
        //    没有任何新状态时不重算，避免用本地快照把订单状态推着走
        if (changed) {
            Integer derived = resolveOrderStatus(confirm, preInvoice, order.getPaymentStatus(), issue, tax, upload);
            Integer current = order.getOrderStatus() == null ? 0 : order.getOrderStatus();
            if (!Integer.valueOf(9).equals(current) && derived > current) {
                update.setOrderStatus(derived);
            }
        }
        invoiceOrderMapper.updateById(update);
        log.info("开票状态收敛 - partnerOrderId: {}, confirmStatus: {}, preInvoiceStatus: {}, invoiceStatus: {}, taxStatus: {}, uploadStatus: {}",
                partnerOrderId, confirm, preInvoice, issue, tax, upload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPaymentSucceeded(String partnerOrderId) {
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (order == null) {
            // 付款状态已经收敛，不因开票数据缺失把付款拖下水
            return;
        }
        // 付款成功是「真正开票」的触发点：先进入开票中，再尽力向工行确认一次最新状态
        if (!InvoiceIssueStatusEnum.isIssued(order.getInvoiceStatus())
                && !InvoiceIssueStatusEnum.isException(order.getInvoiceStatus())) {
            InvoiceOrderDO update = new InvoiceOrderDO();
            update.setId(order.getId());
            update.setInvoiceStatus(InvoiceIssueStatusEnum.ISSUING.getStatus());
            int current = order.getOrderStatus() == null ? 0 : order.getOrderStatus();
            if (!Integer.valueOf(9).equals(current) && current < 2) {
                update.setOrderStatus(2);
            }
            invoiceOrderMapper.updateById(update);
        }
        try {
            IcbcGatewayResult<InvoiceInfo> result = icbcGateway.queryInvoiceInfo(InvoiceQueryReq.builder()
                    .outOrderId(order.getPartnerOrderId())
                    .outUserId(order.getPayeeNo())
                    .outVendorId(order.getPayerNo())
                    .build());
            if (result.isSuccess() && result.getData() != null) {
                applyInvoiceInfo(partnerOrderId, result.getData());
            }
        } catch (RuntimeException e) {
            // 查询失败不影响付款收敛：开票 / 缴税 / 上传由通知与后续查询补齐
            log.warn("付款成功后确认开票状态失败，等待通知或后续查询收敛 - partnerOrderId: {}", partnerOrderId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyInvoiceCancelled(String partnerOrderId) {
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (order == null) {
            // 通知早于平台数据落库：抛业务异常，通知落失败、数据落库后可重放
            throw exception(ErrorCodeConstants.CALLBACK_BUSINESS_NOT_EXISTS);
        }
        InvoiceOrderDO update = new InvoiceOrderDO();
        update.setId(order.getId());
        update.setPreInvoiceStatus(PreInvoiceStatusEnum.CANCELLED.getStatus());
        // 已付款 / 已开票不因取消通知被抹掉；未支付的预开票取消后订单即已取消
        if (!PaymentStatusEnum.isSuccess(order.getPaymentStatus())
                && !InvoiceIssueStatusEnum.isIssued(order.getInvoiceStatus())) {
            update.setOrderStatus(9);
        }
        invoiceOrderMapper.updateById(update);
        log.info("发票取消状态收敛 - partnerOrderId: {}", partnerOrderId);
    }

    /**
     * 开票状态：发票号码是最硬的凭证，有号码即已开票；预开票失败则开票失败；
     * 已付款但工行还没出票时进入开票中。
     */
    private Integer resolveIssueStatus(InvoiceOrderDO order, InvoiceInfo info, Integer preInvoiceStatus) {
        if (StrUtil.isNotBlank(info.getInvoiceCode()) || StrUtil.isNotBlank(info.getInvoiceNo())) {
            return InvoiceIssueStatusEnum.ISSUED.getStatus();
        }
        if (PreInvoiceStatusEnum.FAILED.getStatus().equals(preInvoiceStatus)) {
            return InvoiceIssueStatusEnum.FAILED.getStatus();
        }
        if (PaymentStatusEnum.isSuccess(order.getPaymentStatus())) {
            return InvoiceIssueStatusEnum.ISSUING.getStatus();
        }
        return null;
    }

    /**
     * 已开票是正终态，不被任何后续状态回退；开票失败也不被旧的进行中态回退。
     */
    private boolean shouldApplyIssue(Integer current, Integer next) {
        if (current == null) {
            return true;
        }
        if (InvoiceIssueStatusEnum.isIssued(next)) {
            return true;
        }
        if (InvoiceIssueStatusEnum.isIssued(current)) {
            return false;
        }
        return next >= current;
    }

    /**
     * 缴税已结清（成功 / 无需缴税）后，旧通知里的缴税中 / 异常不回退。
     */
    private boolean shouldApplyTax(Integer current, Integer next) {
        if (current == null) {
            return true;
        }
        if (TaxStatusEnum.isPaid(current)) {
            return TaxStatusEnum.isPaid(next);
        }
        return true;
    }

    /**
     * 上传成功后，旧通知里的处理中 / 已受理 / 上传中不回退。
     */
    private boolean shouldApplyUpload(Integer current, Integer next) {
        if (current == null) {
            return true;
        }
        if (UploadStatusEnum.isSuccess(current)) {
            return UploadStatusEnum.isSuccess(next);
        }
        return true;
    }

    private void applyInvoiceFields(InvoiceOrderDO update, InvoiceOrderDO order, InvoiceInfo info,
                                    Integer issueStatus) {
        if (StrUtil.isNotBlank(info.getInvoiceCode())) {
            update.setInvoiceCode(info.getInvoiceCode());
        }
        String invoiceNo = StrUtil.isNotBlank(info.getInvoiceNo())
                ? info.getInvoiceNo() : info.getInvoiceCode();
        if (StrUtil.isNotBlank(invoiceNo)) {
            update.setInvoiceNo(invoiceNo);
        }
        LocalDateTime invoiceDate = IcbcTimeUtils.parse(info.getInvoiceDate());
        if (invoiceDate != null) {
            update.setInvoiceDate(invoiceDate);
        }
        BigDecimal invoiceAmount = AmountUtils.parse(info.getPayAmount());
        if (invoiceAmount != null) {
            update.setInvoiceAmount(invoiceAmount);
        } else if (InvoiceIssueStatusEnum.isIssued(issueStatus) && order.getInvoiceAmount() == null) {
            update.setInvoiceAmount(order.getTotalAmount());
        }
        BigDecimal taxAmount = AmountUtils.parse(info.getTaxAmount());
        if (taxAmount != null) {
            update.setTaxAmount(taxAmount);
        }
    }

    private void applyTaxFields(InvoiceOrderDO update, InvoiceInfo info) {
        BigDecimal taxRealAmount = AmountUtils.parse(info.getTaxRealAmount());
        if (taxRealAmount != null) {
            update.setTaxRealAmount(taxRealAmount);
        }
        LocalDateTime taxTime = IcbcTimeUtils.parse(info.getTradeTime());
        if (taxTime != null) {
            update.setTaxTime(taxTime);
        }
        if (StrUtil.isNotBlank(info.getTaxPaymentMethod())) {
            update.setTaxPaymentMethod(info.getTaxPaymentMethod());
        }
        String voucherNo = resolveTaxVoucherNo(info);
        if (StrUtil.isNotBlank(voucherNo)) {
            update.setTaxVoucherNo(voucherNo);
        }
    }

    /**
     * 缴税凭证编号取征收信息明细里的应征凭证序号；多条明细时取第一条。
     */
    private String resolveTaxVoucherNo(InvoiceInfo info) {
        if (StrUtil.isNotBlank(info.getTaxVoucherNo())) {
            return info.getTaxVoucherNo();
        }
        if (info.getLevyItems() == null) {
            return null;
        }
        for (InvoiceInfo.LevyItem item : info.getLevyItems()) {
            if (item != null && StrUtil.isNotBlank(item.getVoucherNum())) {
                return item.getVoucherNum();
            }
        }
        return null;
    }

    /**
     * 订单状态由五条状态线推出：
     * 待确认(0) → 已确认(1) → 已支付(2) → 已开票(3) → 已完成(4)。
     */
    private Integer resolveOrderStatus(Integer confirmStatus, Integer preInvoiceStatus, Integer paymentStatus,
                                       Integer issueStatus, Integer taxStatus, Integer uploadStatus) {
        if (InvoiceIssueStatusEnum.isIssued(issueStatus)
                && TaxStatusEnum.isPaid(taxStatus) && UploadStatusEnum.isSuccess(uploadStatus)) {
            return 4;
        }
        if (InvoiceIssueStatusEnum.isIssued(issueStatus)) {
            return 3;
        }
        if (PaymentStatusEnum.isSuccess(paymentStatus)) {
            return 2;
        }
        boolean confirmed = confirmStatus != null
                && confirmStatus >= InvoiceConfirmStatusEnum.NATURAL_PERSON_CONFIRMED.getStatus();
        boolean preInvoiceSuccess = PreInvoiceStatusEnum.SUCCESS.getStatus().equals(preInvoiceStatus);
        return confirmed && preInvoiceSuccess ? 1 : 0;
    }

    /**
     * 三类状态里任一条失败 / 异常时，优先给出异常那条的下一步动作；没有异常时，
     * 再进行中的状态给出提示；全部正常终态时为空。
     */
    private String resolveNextAction(InvoiceOrderDO order) {
        if (InvoiceIssueStatusEnum.isException(order.getInvoiceStatus())) {
            return InvoiceIssueStatusEnum.nextActionOf(order.getInvoiceStatus());
        }
        if (TaxStatusEnum.isException(order.getTaxStatus())) {
            return TaxStatusEnum.nextActionOf(order.getTaxStatus());
        }
        if (UploadStatusEnum.isException(order.getUploadStatus())) {
            return UploadStatusEnum.nextActionOf(order.getUploadStatus());
        }
        String action = InvoiceIssueStatusEnum.nextActionOf(order.getInvoiceStatus());
        if (action != null) {
            return action;
        }
        action = TaxStatusEnum.nextActionOf(order.getTaxStatus());
        if (action != null) {
            return action;
        }
        return UploadStatusEnum.nextActionOf(order.getUploadStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindAcquisition(String partnerOrderId, Long acquisitionId, Long payeeId, Long payerId) {
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (order == null) {
            throw exception(ErrorCodeConstants.INVOICE_ORDER_NOT_EXISTS);
        }
        InvoiceOrderDO update = new InvoiceOrderDO();
        update.setId(order.getId());
        update.setAcquisitionId(acquisitionId);
        update.setPayeeId(payeeId);
        update.setPayerId(payerId);
        invoiceOrderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyPreInvoiceStatus(String partnerOrderId, String confirmStatusCode, String preInvoiceStatusCode) {
        applyInvoiceInfo(partnerOrderId, InvoiceInfo.builder()
                .confirmStatus(confirmStatusCode)
                .invoiceStatus(preInvoiceStatusCode)
                .build());
    }

    @Override
    public InvoiceOrderDO getOrderByPartnerOrderId(String partnerOrderId) {
        return invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
    }

    @Override
    public InvoiceOrderDO getOrderByOrderNo(String orderNo) {
        return invoiceOrderMapper.selectByOrderNo(orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(Long id, Integer orderStatus, Integer invoiceStatus, 
                                 Integer paymentStatus, Integer taxStatus) {
        InvoiceOrderDO updateObj = new InvoiceOrderDO();
        updateObj.setId(id);
        if (orderStatus != null) {
            updateObj.setOrderStatus(orderStatus);
        }
        if (invoiceStatus != null) {
            updateObj.setInvoiceStatus(invoiceStatus);
        }
        if (paymentStatus != null) {
            updateObj.setPaymentStatus(paymentStatus);
        }
        if (taxStatus != null) {
            updateObj.setTaxStatus(taxStatus);
        }
        invoiceOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInvoiceInfo(Long id, String invoiceNo, String invoiceCode, 
                                 BigDecimal invoiceAmount, BigDecimal taxAmount) {
        InvoiceOrderDO updateObj = new InvoiceOrderDO();
        updateObj.setId(id);
        updateObj.setInvoiceNo(invoiceNo);
        updateObj.setInvoiceCode(invoiceCode);
        updateObj.setInvoiceAmount(invoiceAmount);
        updateObj.setTaxAmount(taxAmount);
        updateObj.setInvoiceDate(LocalDateTime.now());
        invoiceOrderMapper.updateById(updateObj);
    }

    /**
     * 验证预下单请求参数
     */
    private void validatePreOrderRequest(InvoicePreOrderReqVO request) {
        // 验证特定要素和收购发票类型代码的匹配关系
        if ("16".equals(request.getSpecificElements()) && !"01".equals(request.getBuyerInvTypeCode())) {
            throw exception(ErrorCodeConstants.INVOICE_ORDER_AMOUNT_ERROR);
        }
        if ("24".equals(request.getSpecificElements()) && !"04".equals(request.getBuyerInvTypeCode())) {
            throw exception(ErrorCodeConstants.INVOICE_ORDER_AMOUNT_ERROR);
        }
        
        // 验证农产品收购时必须提供机构编码和U盾ID
        if ("16".equals(request.getSpecificElements())) {
            if (request.getVerifiedCode() == null || request.getVerifiedCode().trim().isEmpty()) {
                throw exception(ErrorCodeConstants.INVOICE_ORDER_AMOUNT_ERROR);
            }
            if (request.getUkeyId() == null || request.getUkeyId().trim().isEmpty()) {
                throw exception(ErrorCodeConstants.INVOICE_ORDER_AMOUNT_ERROR);
            }
        }
        
        // 验证商品信息
        if (request.getGoodsInfo() == null || request.getGoodsInfo().isEmpty()) {
            throw exception(ErrorCodeConstants.INVOICE_ORDER_ITEMS_EMPTY);
        }

        // 简易计税的品类不得开具增值税专用发票（票种 01）
        validateTaxMethodInvoiceType(request);
        
        // 验证商品金额总和是否等于订单总金额
        BigDecimal totalGoodsAmount = request.getGoodsInfo().stream()
            .map(InvoicePreOrderReqVO.GoodsInfoVO::getGoodsAmt)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalGoodsAmount.compareTo(request.getOrderAmount()) != 0) {
            throw exception(ErrorCodeConstants.INVOICE_ORDER_AMOUNT_ERROR);
        }
    }

    /**
     * 按商品明细的税收分类合并编码回查品类计税方法：简易计税只能开普票。
     * 未配置计税方法的品类按一般计税放行，保持对历史数据的兼容。
     */
    private void validateTaxMethodInvoiceType(InvoicePreOrderReqVO request) {
        if (!"01".equals(request.getInvoiceType())) {
            return;
        }
        for (InvoicePreOrderReqVO.GoodsInfoVO goods : request.getGoodsInfo()) {
            IcbcGoodsConfigDO config = goodsConfigService.getGoodsConfigByMergedCode(goods.getMergedCode());
            if (config != null && IcbcTaxMethodEnum.isSimple(config.getTaxMethod())) {
                throw exception(ErrorCodeConstants.SIMPLE_TAX_METHOD_NO_SPECIAL_INVOICE);
            }
        }
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "INV" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
    }

    /**
     * 从请求创建订单对象
     */
    private InvoiceOrderDO createOrderFromRequest(InvoicePreOrderReqVO request, String orderNo) {
        InvoiceOrderDO order = new InvoiceOrderDO();
        order.setOrderNo(orderNo);
        order.setPartnerOrderId(request.getOutOrderId());
        order.setPayeeNo(request.getOutUserId());
        order.setPayerNo(request.getOutVendorId());
        order.setTotalAmount(request.getOrderAmount());
        order.setInvoiceType("01".equals(request.getInvoiceType()) ? 2 : 1); // 01-专票，02-普票
        order.setBusinessType("16".equals(request.getSpecificElements()) ? "AGRICULTURAL" : "SCRAP");
        order.setOrderStatus(0); // 待确认
        order.setInvoiceStatus(0); // 未开票（预下单不产生发票）
        order.setPaymentStatus(0); // 未支付
        order.setTaxStatus(0); // 未缴税
        order.setConfirmStatus(InvoiceConfirmStatusEnum.NOT_CONFIRMED.getStatus());
        order.setPreInvoiceStatus(PreInvoiceStatusEnum.IN_PROGRESS.getStatus());
        order.setPreOrderTime(LocalDateTime.now());
        order.setRemark(request.getNotes());
        return order;
    }

    /**
     * 重复发起时返回既有订单：不再调工行预下单，只把已存在的业务单号与订单号交回。
     */
    private InvoicePreOrderRespVO buildIdempotentResponse(InvoiceOrderDO order) {
        InvoicePreOrderRespVO response = new InvoicePreOrderRespVO();
        response.setReturnCode(0);
        response.setReturnMsg("该收购单已发起过开票申请，未重复下单");
        response.setOrderNo(order.getOrderNo());
        response.setPartnerOrderId(order.getPartnerOrderId());
        return response;
    }

    /**
     * 从请求创建商品明细列表
     */
    private List<OrderItemDO> createOrderItemsFromRequest(InvoicePreOrderReqVO request, Long orderId, String orderNo) {
        List<OrderItemDO> items = new ArrayList<>();
        for (InvoicePreOrderReqVO.GoodsInfoVO goodsInfo : request.getGoodsInfo()) {
            OrderItemDO item = new OrderItemDO();
            item.setOrderId(orderId);
            item.setOrderNo(orderNo);
            item.setItemName(goodsInfo.getProjectName());
            item.setSpecification(goodsInfo.getWeight());
            item.setUnit(goodsInfo.getUnits());
            item.setQuantity(goodsInfo.getGoodsNum());
            item.setUnitPrice(goodsInfo.getPrice());
            item.setAmount(goodsInfo.getGoodsAmt());
            item.setTaxRate(goodsInfo.getTaxRate());
            // 计算税额：金额 * 税率 / (1 + 税率)
            BigDecimal taxAmount = goodsInfo.getGoodsAmt()
                .multiply(goodsInfo.getTaxRate())
                .divide(BigDecimal.ONE.add(goodsInfo.getTaxRate()), 2, BigDecimal.ROUND_HALF_UP);
            item.setTaxAmount(taxAmount);
            items.add(item);
        }
        return items;
    }

    /**
     * 把平台预下单请求转成端口请求
     */
    private PreOrderReq toPreOrderReq(InvoicePreOrderReqVO request) {
        List<PreOrderGoods> goods = new ArrayList<>();
        if (request.getGoodsInfo() != null) {
            for (InvoicePreOrderReqVO.GoodsInfoVO item : request.getGoodsInfo()) {
                goods.add(PreOrderGoods.builder()
                        .goodsSeqno(item.getGoodsSeqno())
                        .projectName(item.getProjectName())
                        .goodsNum(item.getGoodsNum() != null ? item.getGoodsNum().toPlainString() : null)
                        .goodsAmt(item.getGoodsAmt() != null ? item.getGoodsAmt().toPlainString() : null)
                        .weight(item.getWeight())
                        .price(item.getPrice() != null ? item.getPrice().toPlainString() : null)
                        .units(item.getUnits())
                        .taxRate(item.getTaxRate() != null ? item.getTaxRate().toPlainString() : null)
                        .mergedCode(item.getMergedCode())
                        .build());
            }
        }
        return PreOrderReq.builder()
                .outOrderId(request.getOutOrderId())
                .outVendorId(request.getOutVendorId())
                .outUserId(request.getOutUserId())
                .invoiceType(request.getInvoiceType())
                .orderAmount(request.getOrderAmount() != null ? request.getOrderAmount().toPlainString() : null)
                .specificElements(request.getSpecificElements())
                .buyerInvTypeCode(request.getBuyerInvTypeCode())
                .taxpayerNo(request.getTaxpayerNo())
                .taxpayerName(request.getTaxpayerName())
                .drawerName(request.getDrawerName())
                .drawerCardType(request.getDrawerCardType())
                .drawerCardNumber(request.getDrawerCardNumber())
                .naturalPersonName(request.getNaturalPersonName())
                .cardType(request.getCardType())
                .cardNumber(request.getCardNumber())
                .sellerAddress(request.getSellerAddress())
                .sellerTelephone(request.getSellerTelephone())
                .areaCode(request.getAreaCode())
                .payChannel(request.getPayChannel())
                .isSellerPersonProduct(request.getIsSellerPersonProduct())
                .iitProject(request.getIitProject())
                .notes(request.getNotes())
                .taxRate(request.getTaxRate() != null ? request.getTaxRate().toPlainString() : null)
                .supplementaryTax(request.getSupplementaryTax())
                .unuseReduceTaxCode(request.getUnuseReduceTaxCode())
                .taxPayerAccountNo(request.getTaxPayerAccountNo())
                .taxPayerBankCode(request.getTaxPayerBankCode())
                .taxPayerOrgName(request.getTaxPayerOrgName())
                .invoiceNotifyUrl(request.getInvoiceNotifyUrl())
                .payJumpUrl(request.getPayJumpUrl())
                .invoiceJumpUrl(request.getInvoiceJumpUrl())
                .payRem(request.getPayRem())
                .orderRem(request.getOrderRem())
                .goods(goods)
                .build();
    }

} 