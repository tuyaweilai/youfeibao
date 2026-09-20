package cn.iocoder.yudao.module.waste.service.impl.recycler;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerBusinessConfigDO;
import cn.iocoder.yudao.module.waste.dal.mysql.recycler.RecyclerBusinessConfigMapper;
import cn.iocoder.yudao.module.waste.service.recycler.RecyclerBusinessConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.RECYCLER_BUSINESS_CONFIG_NOT_EXISTS;

/**
 * 回收企业业务模式配置 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class RecyclerBusinessConfigServiceImpl implements RecyclerBusinessConfigService {

    @Resource
    private RecyclerBusinessConfigMapper recyclerBusinessConfigMapper;

    @Override
    public Long createRecyclerBusinessConfig(@Valid RecyclerBusinessConfigCreateReqVO createReqVO) {
        // 插入
        RecyclerBusinessConfigDO recyclerBusinessConfig = BeanUtils.toBean(createReqVO, RecyclerBusinessConfigDO.class);
        recyclerBusinessConfigMapper.insert(recyclerBusinessConfig);
        // 返回
        return recyclerBusinessConfig.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecyclerBusinessConfig(@Valid RecyclerBusinessConfigUpdateReqVO updateReqVO) {
        // 校验存在
        validateRecyclerBusinessConfigExists(updateReqVO.getId());
        // 更新
        RecyclerBusinessConfigDO updateObj = BeanUtils.toBean(updateReqVO, RecyclerBusinessConfigDO.class);
        recyclerBusinessConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteRecyclerBusinessConfig(Long id) {
        // 校验存在
        validateRecyclerBusinessConfigExists(id);
        // 删除
        recyclerBusinessConfigMapper.deleteById(id);
    }

    @Override
    public RecyclerBusinessConfigDO getRecyclerBusinessConfig(Long id) {
        return recyclerBusinessConfigMapper.selectById(id);
    }

    @Override
    public RecyclerBusinessConfigRespVO getRecyclerBusinessConfigDetail(Long id) {
        RecyclerBusinessConfigDO recyclerBusinessConfig = validateRecyclerBusinessConfigExists(id);
        return BeanUtils.toBean(recyclerBusinessConfig, RecyclerBusinessConfigRespVO.class);
    }

    @Override
    public PageResult<RecyclerBusinessConfigDO> getRecyclerBusinessConfigPage(RecyclerBusinessConfigPageReqVO pageReqVO) {
        return recyclerBusinessConfigMapper.selectPage(pageReqVO);
    }

    @Override
    public List<RecyclerBusinessConfigDO> getRecyclerBusinessConfigList(RecyclerBusinessConfigPageReqVO exportReqVO) {
        return recyclerBusinessConfigMapper.selectList(exportReqVO);
    }

    @Override
    public RecyclerBusinessConfigDO getRecyclerBusinessConfigByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return recyclerBusinessConfigMapper.selectByRecyclingEnterpriseId(recyclingEnterpriseId);
    }

    @Override
    public List<RecyclerBusinessConfigDO> getRecyclerBusinessConfigListByBusinessMode(Integer businessMode) {
        return recyclerBusinessConfigMapper.selectByBusinessMode(businessMode);
    }

    @Override
    public List<RecyclerBusinessConfigDO> getRecyclerBusinessConfigListByDefaultQuotationMode(Integer defaultQuotationMode) {
        return recyclerBusinessConfigMapper.selectByDefaultQuotationMode(defaultQuotationMode);
    }

    @Override
    public List<RecyclerBusinessConfigDO> getEnabledRecyclerBusinessConfigs() {
        return recyclerBusinessConfigMapper.selectEnabledConfigs();
    }

    @Override
    public List<RecyclerBusinessConfigDO> getDisabledRecyclerBusinessConfigs() {
        return recyclerBusinessConfigMapper.selectDisabledConfigs();
    }

    @Override
    public List<RecyclerBusinessConfigDO> getCompetitiveBiddingConfigs() {
        return recyclerBusinessConfigMapper.selectCompetitiveBiddingConfigs();
    }

    @Override
    public List<RecyclerBusinessConfigDO> getNegotiationConfigs() {
        return recyclerBusinessConfigMapper.selectNegotiationConfigs();
    }

    @Override
    public List<RecyclerBusinessConfigDO> getFixedPriceConfigs() {
        return recyclerBusinessConfigMapper.selectFixedPriceConfigs();
    }

    @Override
    public List<RecyclerBusinessConfigDO> getAutoQuotationConfigs() {
        return recyclerBusinessConfigMapper.selectAutoQuotationConfigs();
    }

    @Override
    public List<RecyclerBusinessConfigDO> getManualQuotationConfigs() {
        return recyclerBusinessConfigMapper.selectManualQuotationConfigs();
    }

    @Override
    public List<RecyclerBusinessConfigDO> getAllowClientModeSelectionConfigs() {
        return recyclerBusinessConfigMapper.selectAllowClientModeSelectionConfigs();
    }

    @Override
    public List<RecyclerBusinessConfigDO> getAutoAcceptSingleQuotationConfigs() {
        return recyclerBusinessConfigMapper.selectAutoAcceptSingleQuotationConfigs();
    }

    @Override
    public List<RecyclerBusinessConfigDO> getPriceNegotiationEnabledConfigs() {
        return recyclerBusinessConfigMapper.selectPriceNegotiationEnabledConfigs();
    }

    @Override
    public List<RecyclerBusinessConfigDO> getRecyclerBusinessConfigListByQuotationTimeoutRange(Integer minHours, Integer maxHours) {
        return recyclerBusinessConfigMapper.selectByQuotationTimeoutRange(minHours, maxHours);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableRecyclerBusinessConfig(Long id) {
        RecyclerBusinessConfigDO config = validateRecyclerBusinessConfigExists(id);
        config.setIsEnabled(true);
        recyclerBusinessConfigMapper.updateById(config);
        log.info("[enableRecyclerBusinessConfig][启用业务配置] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableRecyclerBusinessConfig(Long id) {
        RecyclerBusinessConfigDO config = validateRecyclerBusinessConfigExists(id);
        config.setIsEnabled(false);
        recyclerBusinessConfigMapper.updateById(config);
        log.info("[disableRecyclerBusinessConfig][禁用业务配置] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBusinessMode(Long id, Integer businessMode) {
        RecyclerBusinessConfigDO config = validateRecyclerBusinessConfigExists(id);
        config.setBusinessMode(businessMode);
        recyclerBusinessConfigMapper.updateById(config);
        log.info("[updateBusinessMode][更新业务模式] id={}, businessMode={}", id, businessMode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDefaultQuotationMode(Long id, Integer defaultQuotationMode) {
        RecyclerBusinessConfigDO config = validateRecyclerBusinessConfigExists(id);
        config.setDefaultQuotationMode(defaultQuotationMode);
        recyclerBusinessConfigMapper.updateById(config);
        log.info("[updateDefaultQuotationMode][更新报价模式] id={}, defaultQuotationMode={}", id, defaultQuotationMode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuotationTimeoutHours(Long id, Integer quotationTimeoutHours) {
        RecyclerBusinessConfigDO config = validateRecyclerBusinessConfigExists(id);
        config.setQuotationTimeoutHours(quotationTimeoutHours);
        recyclerBusinessConfigMapper.updateById(config);
        log.info("[updateQuotationTimeoutHours][更新报价超时时间] id={}, quotationTimeoutHours={}", id, quotationTimeoutHours);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAllowClientModeSelection(Long id, Boolean allowClientModeSelection) {
        RecyclerBusinessConfigDO config = validateRecyclerBusinessConfigExists(id);
        config.setAllowClientModeSelection(allowClientModeSelection);
        recyclerBusinessConfigMapper.updateById(config);
        log.info("[setAllowClientModeSelection][设置客户模式选择] id={}, allowClientModeSelection={}", id, allowClientModeSelection);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAutoAcceptSingleQuotation(Long id, Boolean autoAcceptSingleQuotation) {
        RecyclerBusinessConfigDO config = validateRecyclerBusinessConfigExists(id);
        config.setAutoAcceptSingleQuotation(autoAcceptSingleQuotation);
        recyclerBusinessConfigMapper.updateById(config);
        log.info("[setAutoAcceptSingleQuotation][设置自动接受单一报价] id={}, autoAcceptSingleQuotation={}", id, autoAcceptSingleQuotation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEnablePriceNegotiation(Long id, Boolean enablePriceNegotiation) {
        RecyclerBusinessConfigDO config = validateRecyclerBusinessConfigExists(id);
        config.setEnablePriceNegotiation(enablePriceNegotiation);
        recyclerBusinessConfigMapper.updateById(config);
        log.info("[setEnablePriceNegotiation][设置价格协商] id={}, enablePriceNegotiation={}", id, enablePriceNegotiation);
    }

    @Override
    public RecyclerBusinessConfigDO validateRecyclerBusinessConfigExists(Long id) {
        RecyclerBusinessConfigDO recyclerBusinessConfig = recyclerBusinessConfigMapper.selectById(id);
        if (recyclerBusinessConfig == null) {
            throw exception(RECYCLER_BUSINESS_CONFIG_NOT_EXISTS);
        }
        return recyclerBusinessConfig;
    }

    // ==================== Controller调用的方法 ====================

    @Override
    public RecyclerBusinessConfigDO getConfigByEnterpriseId(Long enterpriseId) {
        return recyclerBusinessConfigMapper.selectByRecyclingEnterpriseId(enterpriseId);
    }

    @Override
    public List<RecyclerBusinessConfigDO> getConfigsByBusinessMode(Integer businessMode) {
        return recyclerBusinessConfigMapper.selectByBusinessMode(businessMode);
    }

    @Override
    public List<RecyclerBusinessConfigDO> getConfigsByQuotationMode(Integer quotationMode) {
        return recyclerBusinessConfigMapper.selectByDefaultQuotationMode(quotationMode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setQuotationRules(Long enterpriseId, Integer quotationTimeoutMinutes, Boolean autoAcceptEnabled, String autoAcceptThreshold) {
        RecyclerBusinessConfigDO config = getConfigByEnterpriseId(enterpriseId);
        if (config != null) {
            // 将分钟转换为小时
            config.setQuotationTimeoutHours(quotationTimeoutMinutes / 60);
            config.setAutoAcceptSingleQuotation(autoAcceptEnabled);
            recyclerBusinessConfigMapper.updateById(config);
            log.info("[setQuotationRules][设置报价规则] enterpriseId={}, quotationTimeoutMinutes={}, autoAcceptEnabled={}", 
                    enterpriseId, quotationTimeoutMinutes, autoAcceptEnabled);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setNegotiationConfig(Long enterpriseId, Boolean priceNegotiationEnabled, Integer maxNegotiationRounds, Integer negotiationTimeoutHours) {
        RecyclerBusinessConfigDO config = getConfigByEnterpriseId(enterpriseId);
        if (config != null) {
            config.setEnablePriceNegotiation(priceNegotiationEnabled);
            recyclerBusinessConfigMapper.updateById(config);
            log.info("[setNegotiationConfig][设置协商配置] enterpriseId={}, priceNegotiationEnabled={}, maxNegotiationRounds={}", 
                    enterpriseId, priceNegotiationEnabled, maxNegotiationRounds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setCustomerMode(Long enterpriseId, Integer customerModeSelection) {
        RecyclerBusinessConfigDO config = getConfigByEnterpriseId(enterpriseId);
        if (config != null) {
            config.setAllowClientModeSelection(customerModeSelection == 1);
            recyclerBusinessConfigMapper.updateById(config);
            log.info("[setCustomerMode][设置客户模式] enterpriseId={}, customerModeSelection={}", 
                    enterpriseId, customerModeSelection);
        }
    }

} 