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
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.OrderItemMapper;
import cn.iocoder.yudao.module.icbc.enums.IcbcTaxMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceConfirmStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceQueryReq;
import cn.iocoder.yudao.module.icbc.gateway.model.PreOrderGoods;
import cn.iocoder.yudao.module.icbc.gateway.model.PreOrderReq;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
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
            applyIcbcInvoiceInfo(order, result.getData());
            order = invoiceOrderMapper.selectById(order.getId());
        }
        log.info("反向开票查询成功 - orderNo: {}, partnerOrderId: {}, confirmStatus: {}, preInvoiceStatus: {}",
                order.getOrderNo(), order.getPartnerOrderId(), order.getConfirmStatus(), order.getPreInvoiceStatus());
        return buildQueryResponse(order);
    }

    private InvoiceQueryRespVO buildQueryResponse(InvoiceOrderDO order) {
        InvoiceQueryRespVO response = new InvoiceQueryRespVO();
        response.setReturnCode(0);
        response.setReturnMsg("成功");
        response.setOrderNo(order.getOrderNo());
        response.setPartnerOrderId(order.getPartnerOrderId());
        response.setOrderStatus(order.getOrderStatus());
        response.setInvoiceStatus(order.getInvoiceStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setTaxStatus(order.getTaxStatus());
        response.setConfirmStatus(order.getConfirmStatus());
        response.setPreInvoiceStatus(order.getPreInvoiceStatus());
        response.setInvoiceNo(order.getInvoiceNo());
        response.setInvoiceCode(order.getInvoiceCode());
        response.setInvoiceDate(order.getInvoiceDate());
        response.setInvoiceAmount(order.getInvoiceAmount());
        response.setTaxAmount(order.getTaxAmount());
        return response;
    }

    /**
     * 把工行预查询结果收敛回本地订单：自然人确认与预开票两条状态线独立更新，
     * 并同步发票代码 / 税额等已落库字段。
     */
    private void applyIcbcInvoiceInfo(InvoiceOrderDO order, InvoiceInfo info) {
        InvoiceOrderDO update = buildPreInvoiceUpdate(order,
                info.getConfirmStatus(), info.getInvoiceStatus(), false);
        if (StrUtil.isNotBlank(info.getInvoiceCode())) {
            update.setInvoiceCode(info.getInvoiceCode());
        }
        if (StrUtil.isNotBlank(info.getTaxAmount())) {
            update.setTaxAmount(parseAmount(info.getTaxAmount()));
        }
        invoiceOrderMapper.updateById(update);
    }

    private BigDecimal parseAmount(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 用工行状态码构造一条状态更新：两条状态线独立更新，未上送的不覆盖。
     *
     * @param resolveOrderStatusFromSnapshot 无状态码时是否仍按快照重算订单状态：
     *        通知路径为 true（确保收敛），预查询路径为 false（未拿到新状态则保持本地快照）
     */
    private InvoiceOrderDO buildPreInvoiceUpdate(InvoiceOrderDO order, String confirmStatusCode,
                                                 String preInvoiceStatusCode, boolean resolveOrderStatusFromSnapshot) {
        InvoiceOrderDO update = new InvoiceOrderDO();
        update.setId(order.getId());
        Integer confirm = order.getConfirmStatus();
        Integer preInvoice = order.getPreInvoiceStatus();
        boolean hasStatus = false;
        if (StrUtil.isNotBlank(confirmStatusCode)) {
            confirm = InvoiceConfirmStatusEnum.toStatus(confirmStatusCode);
            update.setConfirmStatus(confirm);
            hasStatus = true;
        }
        if (StrUtil.isNotBlank(preInvoiceStatusCode)) {
            preInvoice = PreInvoiceStatusEnum.toStatus(preInvoiceStatusCode);
            update.setPreInvoiceStatus(preInvoice);
            hasStatus = true;
        }
        if (hasStatus || resolveOrderStatusFromSnapshot) {
            update.setOrderStatus(resolveOrderStatus(confirm, preInvoice));
        }
        return update;
    }

    /**
     * 订单状态由两条状态线推出：自然人确认完成且预开票成功才算「已确认」。
     */
    private Integer resolveOrderStatus(Integer confirmStatus, Integer preInvoiceStatus) {
        boolean confirmed = confirmStatus != null
                && confirmStatus >= InvoiceConfirmStatusEnum.NATURAL_PERSON_CONFIRMED.getStatus();
        boolean preInvoiceSuccess = PreInvoiceStatusEnum.SUCCESS.getStatus().equals(preInvoiceStatus);
        return confirmed && preInvoiceSuccess ? 1 : 0;
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
        InvoiceOrderDO order = invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
        if (order == null) {
            // 通知早于平台数据落库：抛出业务异常，让通知落为失败、数据落库后可重放
            throw exception(ErrorCodeConstants.CALLBACK_BUSINESS_NOT_EXISTS);
        }
        InvoiceOrderDO update = buildPreInvoiceUpdate(order, confirmStatusCode, preInvoiceStatusCode, true);
        invoiceOrderMapper.updateById(update);
        log.info("开票状态收敛 - partnerOrderId: {}, confirmStatus: {}, preInvoiceStatus: {}",
                partnerOrderId, update.getConfirmStatus(), update.getPreInvoiceStatus());
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