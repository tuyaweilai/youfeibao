package cn.iocoder.yudao.module.waste.service.impl.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.ProducerPaymentConfigDO;
import cn.iocoder.yudao.module.waste.dal.mysql.payment.ProducerPaymentConfigMapper;
import cn.iocoder.yudao.module.waste.service.payment.ProducerPaymentConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.PRODUCER_PAYMENT_CONFIG_NOT_EXISTS;

/**
 * 产废企业付款配置 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ProducerPaymentConfigServiceImpl implements ProducerPaymentConfigService {

    @Resource
    private ProducerPaymentConfigMapper producerPaymentConfigMapper;

    @Override
    public Long createProducerPaymentConfig(@Valid ProducerPaymentConfigCreateReqVO createReqVO) {
        // 插入
        ProducerPaymentConfigDO producerPaymentConfig = BeanUtils.toBean(createReqVO, ProducerPaymentConfigDO.class);
        producerPaymentConfigMapper.insert(producerPaymentConfig);
        // 返回
        return producerPaymentConfig.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProducerPaymentConfig(@Valid ProducerPaymentConfigUpdateReqVO updateReqVO) {
        // 校验存在
        validateProducerPaymentConfigExists(updateReqVO.getId());
        // 更新
        ProducerPaymentConfigDO updateObj = BeanUtils.toBean(updateReqVO, ProducerPaymentConfigDO.class);
        producerPaymentConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteProducerPaymentConfig(Long id) {
        // 校验存在
        validateProducerPaymentConfigExists(id);
        // 删除
        producerPaymentConfigMapper.deleteById(id);
    }

    @Override
    public ProducerPaymentConfigDO getProducerPaymentConfig(Long id) {
        return producerPaymentConfigMapper.selectById(id);
    }

    @Override
    public ProducerPaymentConfigRespVO getProducerPaymentConfigDetail(Long id) {
        ProducerPaymentConfigDO producerPaymentConfig = validateProducerPaymentConfigExists(id);
        return BeanUtils.toBean(producerPaymentConfig, ProducerPaymentConfigRespVO.class);
    }

    @Override
    public PageResult<ProducerPaymentConfigDO> getProducerPaymentConfigPage(ProducerPaymentConfigPageReqVO pageReqVO) {
        PageResult<ProducerPaymentConfigDO> pageResult = producerPaymentConfigMapper.selectPage(pageReqVO);
        return pageResult;
    }

    @Override
    public List<ProducerPaymentConfigDO> getProducerPaymentConfigList(ProducerPaymentConfigPageReqVO exportReqVO) {
        return producerPaymentConfigMapper.selectList(exportReqVO);
    }

    @Override
    public ProducerPaymentConfigDO getProducerPaymentConfigByEnterpriseId(Long enterpriseId) {
        return producerPaymentConfigMapper.selectByEnterpriseId(enterpriseId);
    }

    @Override
    public List<ProducerPaymentConfigDO> getProducerPaymentConfigListByStoreId(Long storeId) {
        return producerPaymentConfigMapper.selectListByStoreId(storeId);
    }

    @Override
    public List<ProducerPaymentConfigDO> getProducerPaymentConfigListByPaymentMethod(Integer paymentMethod) {
        return producerPaymentConfigMapper.selectListByPaymentMethod(paymentMethod);
    }

    @Override
    public List<ProducerPaymentConfigDO> getDefaultProducerPaymentConfigs() {
        return producerPaymentConfigMapper.selectDefaultConfigs();
    }

    @Override
    public List<ProducerPaymentConfigDO> getValidProducerPaymentConfigs() {
        return producerPaymentConfigMapper.selectValidConfigs();
    }

    @Override
    public List<ProducerPaymentConfigDO> getAutoPaymentConfigs() {
        return producerPaymentConfigMapper.selectAutoPaymentConfigs();
    }

    @Override
    public List<ProducerPaymentConfigDO> getCorporateSettlementConfigs() {
        return producerPaymentConfigMapper.selectCorporateSettlementConfigs();
    }

    @Override
    public List<ProducerPaymentConfigDO> getPersonalSettlementConfigs() {
        return producerPaymentConfigMapper.selectPersonalSettlementConfigs();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableProducerPaymentConfig(Long id) {
        ProducerPaymentConfigDO config = validateProducerPaymentConfigExists(id);
        config.setConfigStatus(1); // 启用
        producerPaymentConfigMapper.updateById(config);
        log.info("[enableProducerPaymentConfig][启用付款配置] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableProducerPaymentConfig(Long id) {
        ProducerPaymentConfigDO config = validateProducerPaymentConfigExists(id);
        config.setConfigStatus(0); // 禁用
        producerPaymentConfigMapper.updateById(config);
        log.info("[disableProducerPaymentConfig][禁用付款配置] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAsDefaultConfig(Long id) {
        ProducerPaymentConfigDO config = validateProducerPaymentConfigExists(id);
        
        // 先取消同企业的其他默认配置
        if (config.getProducingEnterpriseId() != null) {
            producerPaymentConfigMapper.unsetDefaultByEnterpriseId(config.getProducingEnterpriseId());
        }
        
        // 设置为默认配置
        config.setIsDefaultConfig(true);
        producerPaymentConfigMapper.updateById(config);
        log.info("[setAsDefaultConfig][设置为默认配置] id={}, enterpriseId={}", id, config.getProducingEnterpriseId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unsetDefaultConfig(Long id) {
        ProducerPaymentConfigDO config = validateProducerPaymentConfigExists(id);
        config.setIsDefaultConfig(false);
        producerPaymentConfigMapper.updateById(config);
        log.info("[unsetDefaultConfig][取消默认配置] id={}", id);
    }

    @Override
    public ProducerPaymentConfigDO validateProducerPaymentConfigExists(Long id) {
        ProducerPaymentConfigDO producerPaymentConfig = producerPaymentConfigMapper.selectById(id);
        if (producerPaymentConfig == null) {
            throw exception(PRODUCER_PAYMENT_CONFIG_NOT_EXISTS);
        }
        return producerPaymentConfig;
    }

    @Override
    public ProducerPaymentConfigDO getConfigByEnterpriseId(Long enterpriseId) {
        return producerPaymentConfigMapper.selectByEnterpriseId(enterpriseId);
    }

    @Override
    public List<ProducerPaymentConfigDO> getValidConfigs() {
        return producerPaymentConfigMapper.selectValidConfigs();
    }

    @Override
    public List<ProducerPaymentConfigDO> getDefaultConfigs() {
        return producerPaymentConfigMapper.selectDefaultConfigs();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableConfig(Long id) {
        ProducerPaymentConfigDO config = validateProducerPaymentConfigExists(id);
        config.setConfigStatus(1); // 启用
        producerPaymentConfigMapper.updateById(config);
        log.info("[enableConfig][启用配置] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableConfig(Long id) {
        ProducerPaymentConfigDO config = validateProducerPaymentConfigExists(id);
        config.setConfigStatus(0); // 禁用
        producerPaymentConfigMapper.updateById(config);
        log.info("[disableConfig][禁用配置] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        ProducerPaymentConfigDO config = validateProducerPaymentConfigExists(id);
        
        // 先取消同企业的其他默认配置
        if (config.getProducingEnterpriseId() != null) {
            producerPaymentConfigMapper.unsetDefaultByEnterpriseId(config.getProducingEnterpriseId());
        }
        
        // 设置为默认配置
        config.setIsDefaultConfig(true);
        producerPaymentConfigMapper.updateById(config);
        log.info("[setDefault][设置默认配置] id={}, enterpriseId={}", id, config.getProducingEnterpriseId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unsetDefault(Long id) {
        ProducerPaymentConfigDO config = validateProducerPaymentConfigExists(id);
        config.setIsDefaultConfig(false);
        producerPaymentConfigMapper.updateById(config);
        log.info("[unsetDefault][取消默认配置] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultConfig(Long id, Long enterpriseId) {
        ProducerPaymentConfigDO config = validateProducerPaymentConfigExists(id);
        
        // 先取消同企业的其他默认配置
        if (enterpriseId != null) {
            producerPaymentConfigMapper.unsetDefaultByEnterpriseId(enterpriseId);
        }
        
        // 设置为默认配置
        config.setIsDefaultConfig(true);
        producerPaymentConfigMapper.updateById(config);
        log.info("[setDefaultConfig][设置默认配置] id={}, enterpriseId={}", id, enterpriseId);
    }

    @Override
    public List<ProducerPaymentConfigDO> getConfigsByEnterpriseId(Long enterpriseId) {
        return producerPaymentConfigMapper.selectListByEnterpriseId(enterpriseId);
    }

    @Override
    public ProducerPaymentConfigDO getDefaultConfig(Long enterpriseId) {
        return producerPaymentConfigMapper.selectDefaultByEnterpriseId(enterpriseId);
    }

    @Override
    public List<ProducerPaymentConfigDO> getConfigsByPaymentMethod(Long enterpriseId, Integer paymentMethod) {
        return producerPaymentConfigMapper.selectListByEnterpriseIdAndPaymentMethod(enterpriseId, paymentMethod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        for (Long id : ids) {
            ProducerPaymentConfigDO config = validateProducerPaymentConfigExists(id);
            config.setConfigStatus(status);
            producerPaymentConfigMapper.updateById(config);
        }
        log.info("[batchUpdateStatus][批量更新状态] ids={}, status={}", ids, status);
    }

} 