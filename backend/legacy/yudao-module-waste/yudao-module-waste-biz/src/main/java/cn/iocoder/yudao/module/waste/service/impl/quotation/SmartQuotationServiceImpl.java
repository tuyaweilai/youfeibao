package cn.iocoder.yudao.module.waste.service.impl.quotation;

import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerCustomerPriceDO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.RecyclerPriceConfigDO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.PriceBenchmarkDO;
import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerBusinessConfigDO;
import cn.iocoder.yudao.module.waste.dal.dataobject.appointment.AppointmentDO;
import cn.iocoder.yudao.module.waste.dal.mysql.recycler.RecyclerCustomerPriceMapper;
import cn.iocoder.yudao.module.waste.dal.mysql.price.RecyclerPriceConfigMapper;
import cn.iocoder.yudao.module.waste.dal.mysql.price.PriceBenchmarkMapper;
import cn.iocoder.yudao.module.waste.dal.mysql.quotation.AppointmentQuotationMapper;
import cn.iocoder.yudao.module.waste.dal.mysql.appointment.AppointmentMapper;
import cn.iocoder.yudao.module.waste.service.recycler.RecyclerBusinessConfigService;
import cn.iocoder.yudao.module.waste.service.quotation.SmartQuotationService;
import cn.iocoder.yudao.module.waste.service.quotation.AppointmentQuotationService;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.*;

/**
 * 智能报价生成服务实现类
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class SmartQuotationServiceImpl implements SmartQuotationService {

    @Resource
    private AppointmentQuotationMapper appointmentQuotationMapper;

    @Resource
    private RecyclerCustomerPriceMapper recyclerCustomerPriceMapper;

    @Resource
    private RecyclerPriceConfigMapper recyclerPriceConfigMapper;

    @Resource
    private PriceBenchmarkMapper priceBenchmarkMapper;

    @Resource
    private RecyclerBusinessConfigService recyclerBusinessConfigService;

    @Resource
    private AppointmentQuotationService appointmentQuotationService;

    @Resource
    private AppointmentMapper appointmentMapper;

    @Override
    public BigDecimal generateSmartQuotation(Long appointmentId, Long recyclingEnterpriseId, 
                                           String wasteCode, BigDecimal quantity, 
                                           Long customerEnterpriseId, String region) {
        // 1. 应用价格策略计算单价
        BigDecimal unitPrice = calculatePriceByStrategy(recyclingEnterpriseId, wasteCode, 
                                                       quantity, customerEnterpriseId, region);
        
        if (unitPrice == null) {
            log.warn("[generateSmartQuotation][未找到适用的价格策略] recyclingEnterpriseId={}, wasteCode={}", 
                    recyclingEnterpriseId, wasteCode);
            return BigDecimal.ZERO;
        }
        
        // 2. 计算总金额
        BigDecimal totalAmount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        
        // 3. 应用业务规则调整（如批量折扣、VIP折扣等）
        totalAmount = applyBusinessRuleAdjustments(recyclingEnterpriseId, customerEnterpriseId, 
                                                  wasteCode, quantity, totalAmount);
        
        log.info("[generateSmartQuotation][智能报价生成成功] appointmentId={}, recyclingEnterpriseId={}, " +
                "wasteCode={}, quantity={}, unitPrice={}, totalAmount={}", 
                appointmentId, recyclingEnterpriseId, wasteCode, quantity, unitPrice, totalAmount);
        
        return totalAmount;
    }

    @Override
    public BigDecimal calculatePriceByStrategy(Long recyclingEnterpriseId, String wasteCode, 
                                             BigDecimal quantity, Long customerEnterpriseId, String region) {
        // 优先级1：客户专属价格
        BigDecimal customerPrice = getCustomerSpecificPrice(recyclingEnterpriseId, customerEnterpriseId, wasteCode);
        if (customerPrice != null) {
            log.debug("[calculatePriceByStrategy][使用客户专属价格] price={}", customerPrice);
            return customerPrice;
        }
        
        // 优先级2：区域价格
        BigDecimal regionalPrice = getRegionalPrice(recyclingEnterpriseId, wasteCode, region);
        if (regionalPrice != null) {
            log.debug("[calculatePriceByStrategy][使用区域价格] price={}", regionalPrice);
            return regionalPrice;
        }
        
        // 优先级3：基础价格
        BigDecimal basePrice = getBasePrice(recyclingEnterpriseId, wasteCode);
        if (basePrice != null) {
            log.debug("[calculatePriceByStrategy][使用基础价格] price={}", basePrice);
            return basePrice;
        }
        
        log.warn("[calculatePriceByStrategy][未找到任何适用价格] recyclingEnterpriseId={}, wasteCode={}", 
                recyclingEnterpriseId, wasteCode);
        return null;
    }

    @Override
    public BigDecimal getCustomerSpecificPrice(Long recyclingEnterpriseId, Long customerEnterpriseId, String wasteCode) {
        RecyclerCustomerPriceDO customerPrice = recyclerCustomerPriceMapper
                .selectEffectivePriceByCustomerAndWaste(recyclingEnterpriseId, customerEnterpriseId, wasteCode);
        
        if (customerPrice != null && customerPrice.getStatus() == 1 && 
            (customerPrice.getEffectiveDate() == null || !customerPrice.getEffectiveDate().isAfter(LocalDateTime.now().toLocalDate())) &&
            (customerPrice.getExpireDate() == null || !customerPrice.getExpireDate().isBefore(LocalDateTime.now().toLocalDate()))) {
            return customerPrice.getSpecialPrice();
        }
        
        return null;
    }

    @Override
    public BigDecimal getRegionalPrice(Long recyclingEnterpriseId, String wasteCode, String region) {
        RecyclerPriceConfigDO regionalPrice = recyclerPriceConfigMapper
                .selectByRecyclerAndWasteAndRegion(recyclingEnterpriseId, wasteCode, region);
        
        if (regionalPrice != null && regionalPrice.getStatus() == 1 &&
            (regionalPrice.getEffectiveDate() == null || !regionalPrice.getEffectiveDate().isAfter(LocalDateTime.now().toLocalDate())) &&
            (regionalPrice.getExpireDate() == null || !regionalPrice.getExpireDate().isBefore(LocalDateTime.now().toLocalDate()))) {
            return regionalPrice.getPurchasePrice();
        }
        
        return null;
    }

    @Override
    public BigDecimal getBasePrice(Long recyclingEnterpriseId, String wasteCode) {
        // 先查找回收企业的基础价格配置
        RecyclerPriceConfigDO basePrice = recyclerPriceConfigMapper
                .selectByRecyclerAndWasteAndRegion(recyclingEnterpriseId, wasteCode, null);
        
        if (basePrice != null && basePrice.getStatus() == 1 &&
            (basePrice.getEffectiveDate() == null || !basePrice.getEffectiveDate().isAfter(LocalDateTime.now().toLocalDate())) &&
            (basePrice.getExpireDate() == null || !basePrice.getExpireDate().isBefore(LocalDateTime.now().toLocalDate()))) {
            return basePrice.getPurchasePrice();
        }
        
        // 如果没有企业基础价格，使用市场基准价格
        PriceBenchmarkDO benchmarkPrice = priceBenchmarkMapper.selectSingleByWasteCode(wasteCode);
        if (benchmarkPrice != null && benchmarkPrice.getStatus() == 1) {
            return benchmarkPrice.getPrice();
        }
        
        return null;
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchGenerateSmartQuotations(Long appointmentId, List<Long> recyclingEnterpriseIds,
                                                  String wasteCode, BigDecimal quantity,
                                                  Long customerEnterpriseId, String region) {
        List<Long> quotationIds = new ArrayList<>();
        
        for (Long recyclingEnterpriseId : recyclingEnterpriseIds) {
            try {
                // 检查企业业务配置
                RecyclerBusinessConfigDO businessConfig = recyclerBusinessConfigService
                        .getConfigByEnterpriseId(recyclingEnterpriseId);
                
                if (businessConfig == null || !businessConfig.getIsEnabled()) {
                    log.warn("[batchGenerateSmartQuotations][企业业务配置未启用] recyclingEnterpriseId={}", 
                            recyclingEnterpriseId);
                    continue;
                }
                
                // 只有自动报价模式才生成智能报价
                if (businessConfig.getDefaultQuotationMode() != 1) {
                    log.debug("[batchGenerateSmartQuotations][企业非自动报价模式] recyclingEnterpriseId={}", 
                            recyclingEnterpriseId);
                    continue;
                }
                
                // 生成智能报价
                BigDecimal quotedPrice = generateSmartQuotation(appointmentId, recyclingEnterpriseId, 
                                                              wasteCode, quantity, customerEnterpriseId, region);
                
                if (quotedPrice.compareTo(BigDecimal.ZERO) > 0) {
                    // 提交报价
                    Long quotationId = appointmentQuotationService.submitQuotation(
                            appointmentId, recyclingEnterpriseId, quotedPrice, 
                            "系统智能报价", LocalDateTime.now().plusHours(businessConfig.getQuotationTimeoutHours()));
                    
                    quotationIds.add(quotationId);
                    
                    log.info("[batchGenerateSmartQuotations][智能报价提交成功] quotationId={}, price={}", 
                            quotationId, quotedPrice);
                }
                
            } catch (Exception e) {
                log.error("[batchGenerateSmartQuotations][企业报价生成失败] recyclingEnterpriseId={}", 
                        recyclingEnterpriseId, e);
            }
        }
        
        return quotationIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoGenerateQuotationsByBusinessMode(Long appointmentId, String wasteCode, 
                                                   BigDecimal quantity, Long customerEnterpriseId, String region) {
        // 获取所有启用自动报价的回收企业
        List<RecyclerBusinessConfigDO> autoQuotationConfigs = recyclerBusinessConfigService.getAutoQuotationConfigs();
        
        List<Long> recyclingEnterpriseIds = new ArrayList<>();
        for (RecyclerBusinessConfigDO config : autoQuotationConfigs) {
            recyclingEnterpriseIds.add(config.getRecyclingEnterpriseId());
        }
        
        if (recyclingEnterpriseIds.isEmpty()) {
            log.warn("[autoGenerateQuotationsByBusinessMode][没有启用自动报价的企业] appointmentId={}", appointmentId);
            return 0;
        }
        
        List<Long> quotationIds = batchGenerateSmartQuotations(appointmentId, recyclingEnterpriseIds, 
                                                              wasteCode, quantity, customerEnterpriseId, region);
        
        log.info("[autoGenerateQuotationsByBusinessMode][自动生成报价完成] appointmentId={}, count={}", 
                appointmentId, quotationIds.size());
        
        return quotationIds.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal adjustQuotationPrice(Long quotationId, BigDecimal adjustmentFactor, String adjustmentReason) {
        AppointmentQuotationDO quotation = appointmentQuotationMapper.selectById(quotationId);
        if (quotation == null) {
            throw new IllegalArgumentException("报价记录不存在");
        }
        
        BigDecimal originalPrice = quotation.getQuotedPrice();
        BigDecimal adjustedPrice = originalPrice.multiply(adjustmentFactor).setScale(2, RoundingMode.HALF_UP);
        
        quotation.setQuotedPrice(adjustedPrice);
        quotation.setTotalAmount(adjustedPrice.multiply(BigDecimal.ONE)); // 假设数量为1，实际应该从预约单获取
        quotation.setQuotationRemark(quotation.getQuotationRemark() + 
                " [价格调整: " + adjustmentReason + ", 调整系数: " + adjustmentFactor + "]");
        
        appointmentQuotationMapper.updateById(quotation);
        
        log.info("[adjustQuotationPrice][报价价格调整] quotationId={}, originalPrice={}, adjustedPrice={}, factor={}", 
                quotationId, originalPrice, adjustedPrice, adjustmentFactor);
        
        return adjustedPrice;
    }

    @Override
    public boolean validateQuotationReasonableness(Long recyclingEnterpriseId, String wasteCode, 
                                                  BigDecimal quotedPrice, BigDecimal quantity) {
        // 获取市场基准价格
        PriceBenchmarkDO benchmarkPrice = priceBenchmarkMapper.selectSingleByWasteCode(wasteCode);
        if (benchmarkPrice == null) {
            log.warn("[validateQuotationReasonableness][未找到市场基准价格] wasteCode={}", wasteCode);
            return true; // 没有基准价格时认为合理
        }
        
        BigDecimal benchmarkUnitPrice = benchmarkPrice.getPrice();
        BigDecimal quotedUnitPrice = quotedPrice.divide(quantity, 2, RoundingMode.HALF_UP);
        
        // 允许的价格波动范围（±30%）
        BigDecimal lowerBound = benchmarkUnitPrice.multiply(new BigDecimal("0.7"));
        BigDecimal upperBound = benchmarkUnitPrice.multiply(new BigDecimal("1.3"));
        
        boolean isReasonable = quotedUnitPrice.compareTo(lowerBound) >= 0 && 
                              quotedUnitPrice.compareTo(upperBound) <= 0;
        
        log.debug("[validateQuotationReasonableness][报价合理性验证] quotedUnitPrice={}, benchmarkPrice={}, " +
                "lowerBound={}, upperBound={}, isReasonable={}", 
                quotedUnitPrice, benchmarkUnitPrice, lowerBound, upperBound, isReasonable);
        
        return isReasonable;
    }

    /**
     * 应用业务规则调整
     */
    private BigDecimal applyBusinessRuleAdjustments(Long recyclingEnterpriseId, Long customerEnterpriseId,
                                                   String wasteCode, BigDecimal quantity, BigDecimal baseAmount) {
        BigDecimal adjustedAmount = baseAmount;
        
        // 1. 批量折扣
        if (quantity.compareTo(new BigDecimal("100")) >= 0) {
            adjustedAmount = adjustedAmount.multiply(new BigDecimal("0.95")); // 5%折扣
            log.debug("[applyBusinessRuleAdjustments][应用批量折扣] discount=5%");
        }
        
        // 2. VIP客户折扣（这里简化处理，实际应该查询客户等级）
        // 可以根据客户历史交易量、信用等级等因素给予折扣
        
        // 3. 季节性调整（可根据实际业务需求实现）
        
        return adjustedAmount.setScale(2, RoundingMode.HALF_UP);
    }


} 