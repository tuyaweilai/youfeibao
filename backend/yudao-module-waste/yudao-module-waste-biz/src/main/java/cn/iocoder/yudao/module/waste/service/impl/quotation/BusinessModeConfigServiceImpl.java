package cn.iocoder.yudao.module.waste.service.impl.quotation;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerBusinessConfigDO;
import cn.iocoder.yudao.module.waste.dal.mysql.recycler.RecyclerBusinessConfigMapper;
import cn.iocoder.yudao.module.waste.enums.BusinessModeEnum;
import cn.iocoder.yudao.module.waste.service.quotation.BusinessModeConfigService;
import cn.iocoder.yudao.module.waste.service.recycler.RecyclerBusinessConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务模式配置管理服务实现
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class BusinessModeConfigServiceImpl implements BusinessModeConfigService {

    @Resource
    private RecyclerBusinessConfigMapper recyclerBusinessConfigMapper;

    @Resource
    private RecyclerBusinessConfigService recyclerBusinessConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long configureBusinessMode(Long recyclingEnterpriseId, Integer businessMode,
                                     Integer defaultQuotationMode, Boolean allowClientModeSelection,
                                     Integer quotationTimeoutHours, Boolean autoAcceptSingleQuotation,
                                     Boolean enablePriceNegotiation) {
        // 验证业务模式有效性
        if (BusinessModeEnum.valueOf(businessMode) == null) {
            throw new IllegalArgumentException("无效的业务模式: " + businessMode);
        }

        // 检查是否已存在配置
        RecyclerBusinessConfigDO existingConfig = recyclerBusinessConfigMapper
                .selectByRecyclingEnterpriseId(recyclingEnterpriseId);

        if (existingConfig != null) {
            // 更新现有配置
            existingConfig.setBusinessMode(businessMode);
            existingConfig.setDefaultQuotationMode(defaultQuotationMode);
            existingConfig.setAllowClientModeSelection(allowClientModeSelection);
            existingConfig.setQuotationTimeoutHours(quotationTimeoutHours);
            existingConfig.setAutoAcceptSingleQuotation(autoAcceptSingleQuotation);
            existingConfig.setEnablePriceNegotiation(enablePriceNegotiation);
            existingConfig.setIsEnabled(true);

            recyclerBusinessConfigMapper.updateById(existingConfig);
            
            log.info("[configureBusinessMode][更新企业业务模式配置] enterpriseId={}, businessMode={}", 
                    recyclingEnterpriseId, businessMode);
            
            return existingConfig.getId();
        } else {
            // 创建新配置
            RecyclerBusinessConfigDO newConfig = RecyclerBusinessConfigDO.builder()
                    .recyclingEnterpriseId(recyclingEnterpriseId)
                    .businessMode(businessMode)
                    .defaultQuotationMode(defaultQuotationMode != null ? defaultQuotationMode : 2) // 默认手动报价
                    .allowClientModeSelection(allowClientModeSelection != null ? allowClientModeSelection : false)
                    .quotationTimeoutHours(quotationTimeoutHours != null ? quotationTimeoutHours : 24) // 默认24小时
                    .autoAcceptSingleQuotation(autoAcceptSingleQuotation != null ? autoAcceptSingleQuotation : false)
                    .enablePriceNegotiation(enablePriceNegotiation != null ? enablePriceNegotiation : true)
                    .isEnabled(true)
                    .remark("系统自动创建")
                    .build();

            recyclerBusinessConfigMapper.insert(newConfig);
            
            log.info("[configureBusinessMode][创建企业业务模式配置] enterpriseId={}, businessMode={}, configId={}", 
                    recyclingEnterpriseId, businessMode, newConfig.getId());
            
            return newConfig.getId();
        }
    }

    @Override
    public RecyclerBusinessConfigDO getBusinessModeConfig(Long recyclingEnterpriseId) {
        return recyclerBusinessConfigMapper.selectByRecyclingEnterpriseId(recyclingEnterpriseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBusinessMode(Long recyclingEnterpriseId, Integer businessMode) {
        // 验证业务模式有效性
        if (BusinessModeEnum.valueOf(businessMode) == null) {
            throw new IllegalArgumentException("无效的业务模式: " + businessMode);
        }

        RecyclerBusinessConfigDO config = recyclerBusinessConfigMapper
                .selectByRecyclingEnterpriseId(recyclingEnterpriseId);
        
        if (config == null) {
            throw new IllegalArgumentException("企业业务配置不存在: " + recyclingEnterpriseId);
        }

        config.setBusinessMode(businessMode);
        recyclerBusinessConfigMapper.updateById(config);
        
        log.info("[updateBusinessMode][更新企业业务模式] enterpriseId={}, businessMode={}", 
                recyclingEnterpriseId, businessMode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleBusinessConfig(Long recyclingEnterpriseId, Boolean enabled) {
        RecyclerBusinessConfigDO config = recyclerBusinessConfigMapper
                .selectByRecyclingEnterpriseId(recyclingEnterpriseId);
        
        if (config == null) {
            throw new IllegalArgumentException("企业业务配置不存在: " + recyclingEnterpriseId);
        }

        config.setIsEnabled(enabled);
        recyclerBusinessConfigMapper.updateById(config);
        
        log.info("[toggleBusinessConfig][切换企业业务配置状态] enterpriseId={}, enabled={}", 
                recyclingEnterpriseId, enabled);
    }

    @Override
    public List<RecyclerBusinessConfigDO> getEnterprisesByBusinessMode(Integer businessMode) {
        return recyclerBusinessConfigMapper.selectByBusinessMode(businessMode);
    }

    @Override
    public List<RecyclerBusinessConfigDO> getBiddingModeEnterprises() {
        return getEnterprisesByBusinessMode(BusinessModeEnum.PLATFORM_BIDDING.getMode());
    }

    @Override
    public List<RecyclerBusinessConfigDO> getIndependentModeEnterprises() {
        return getEnterprisesByBusinessMode(BusinessModeEnum.INDEPENDENT.getMode());
    }

    @Override
    public List<RecyclerBusinessConfigDO> getHybridModeEnterprises() {
        return getEnterprisesByBusinessMode(BusinessModeEnum.HYBRID.getMode());
    }

    @Override
    public boolean supportsBusinessMode(Long recyclingEnterpriseId, Integer businessMode) {
        RecyclerBusinessConfigDO config = getBusinessModeConfig(recyclingEnterpriseId);
        if (config == null || !config.getIsEnabled()) {
            return false;
        }

        // 混合模式支持所有模式
        if (config.getBusinessMode().equals(BusinessModeEnum.HYBRID.getMode())) {
            return true;
        }

        // 其他模式只支持自己的模式
        return config.getBusinessMode().equals(businessMode);
    }

    @Override
    public Integer getDefaultQuotationMode(Long recyclingEnterpriseId) {
        RecyclerBusinessConfigDO config = getBusinessModeConfig(recyclingEnterpriseId);
        return config != null ? config.getDefaultQuotationMode() : 2; // 默认手动报价
    }

    @Override
    public boolean allowsClientModeSelection(Long recyclingEnterpriseId) {
        RecyclerBusinessConfigDO config = getBusinessModeConfig(recyclingEnterpriseId);
        return config != null && config.getAllowClientModeSelection() != null && config.getAllowClientModeSelection();
    }

    @Override
    public Integer getQuotationTimeoutHours(Long recyclingEnterpriseId) {
        RecyclerBusinessConfigDO config = getBusinessModeConfig(recyclingEnterpriseId);
        return config != null && config.getQuotationTimeoutHours() != null ? 
                config.getQuotationTimeoutHours() : 24; // 默认24小时
    }

    @Override
    public boolean autoAcceptsSingleQuotation(Long recyclingEnterpriseId) {
        RecyclerBusinessConfigDO config = getBusinessModeConfig(recyclingEnterpriseId);
        return config != null && config.getAutoAcceptSingleQuotation() != null && config.getAutoAcceptSingleQuotation();
    }

    @Override
    public boolean enablesPriceNegotiation(Long recyclingEnterpriseId) {
        RecyclerBusinessConfigDO config = getBusinessModeConfig(recyclingEnterpriseId);
        return config != null && config.getEnablePriceNegotiation() != null && config.getEnablePriceNegotiation();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchConfigureBusinessMode(List<Long> recyclingEnterpriseIds, Integer businessMode, 
                                         Integer defaultQuotationMode) {
        int successCount = 0;
        
        for (Long enterpriseId : recyclingEnterpriseIds) {
            try {
                configureBusinessMode(enterpriseId, businessMode, defaultQuotationMode, 
                                    false, 24, false, true);
                successCount++;
            } catch (Exception e) {
                log.error("[batchConfigureBusinessMode][批量配置失败] enterpriseId={}", enterpriseId, e);
            }
        }
        
        log.info("[batchConfigureBusinessMode][批量配置完成] total={}, success={}", 
                recyclingEnterpriseIds.size(), successCount);
        
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetToDefaultConfig(Long recyclingEnterpriseId) {
        configureBusinessMode(recyclingEnterpriseId, 
                            BusinessModeEnum.INDEPENDENT.getMode(), // 默认独立运营
                            2, // 默认手动报价
                            false, // 不允许客户选择模式
                            24, // 24小时超时
                            false, // 不自动接受单一报价
                            true); // 启用价格协商
        
        log.info("[resetToDefaultConfig][重置企业业务配置] enterpriseId={}", recyclingEnterpriseId);
    }

    @Override
    public boolean validateBusinessConfig(RecyclerBusinessConfigDO config) {
        if (config == null) {
            return false;
        }

        // 验证业务模式
        if (config.getBusinessMode() == null || BusinessModeEnum.valueOf(config.getBusinessMode()) == null) {
            log.warn("[validateBusinessConfig][无效的业务模式] businessMode={}", config.getBusinessMode());
            return false;
        }

        // 验证报价模式
        if (config.getDefaultQuotationMode() == null || 
            (config.getDefaultQuotationMode() != 1 && config.getDefaultQuotationMode() != 2)) {
            log.warn("[validateBusinessConfig][无效的报价模式] quotationMode={}", config.getDefaultQuotationMode());
            return false;
        }

        // 验证超时时间
        if (config.getQuotationTimeoutHours() != null && config.getQuotationTimeoutHours() <= 0) {
            log.warn("[validateBusinessConfig][无效的超时时间] timeoutHours={}", config.getQuotationTimeoutHours());
            return false;
        }

        return true;
    }
} 