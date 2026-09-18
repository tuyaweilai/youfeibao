package cn.iocoder.yudao.module.icbc.service.invoice.impl;

import cn.hutool.core.util.RandomUtil;
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
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.gateway.model.PreOrderGoods;
import cn.iocoder.yudao.module.icbc.gateway.model.PreOrderReq;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import cn.iocoder.yudao.module.icbc.service.onboarding.SellerOnboardingService;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import lombok.extern.slf4j.Slf4j;
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
        
        // 2. 检查订单是否已存在
        InvoiceOrderDO existingOrder = invoiceOrderMapper.selectByPartnerOrderId(createReqVO.getOutOrderId());
        if (existingOrder != null) {
            throw exception(ErrorCodeConstants.INVOICE_ORDER_AMOUNT_ERROR);
        }
        
        // 3. 生成订单号
        String orderNo = generateOrderNo();
        
        // 4. 创建订单记录
        InvoiceOrderDO order = createOrderFromRequest(createReqVO, orderNo);
        invoiceOrderMapper.insert(order);
        
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
        
        // 2. 调用工行查询接口
        try {
            // 这里暂时返回本地数据，后续集成工行查询接口
            InvoiceQueryRespVO response = new InvoiceQueryRespVO();
            response.setReturnCode(0);
            response.setReturnMsg("成功");
            response.setOrderNo(order.getOrderNo());
            response.setPartnerOrderId(order.getPartnerOrderId());
            response.setOrderStatus(order.getOrderStatus());
            response.setInvoiceStatus(order.getInvoiceStatus());
            response.setPaymentStatus(order.getPaymentStatus());
            response.setTaxStatus(order.getTaxStatus());
            response.setInvoiceNo(order.getInvoiceNo());
            response.setInvoiceCode(order.getInvoiceCode());
            response.setInvoiceDate(order.getInvoiceDate());
            response.setInvoiceAmount(order.getInvoiceAmount());
            response.setTaxAmount(order.getTaxAmount());
            
            log.info("反向开票查询成功 - orderNo: {}, partnerOrderId: {}", 
                order.getOrderNo(), order.getPartnerOrderId());
            return response;
            
        } catch (Exception e) {
            log.error("调用工行查询接口失败 - partnerOrderId: {}", queryReqVO.getOutOrderId(), e);
            throw exception(ErrorCodeConstants.ICBC_API_CALL_FAILED);
        }
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
        order.setInvoiceStatus(0); // 未开票
        order.setPaymentStatus(0); // 未支付
        order.setTaxStatus(0); // 未缴税
        order.setRemark(request.getNotes());
        return order;
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