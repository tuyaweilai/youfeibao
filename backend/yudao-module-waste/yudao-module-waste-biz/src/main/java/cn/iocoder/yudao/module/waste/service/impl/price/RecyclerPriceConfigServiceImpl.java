package cn.iocoder.yudao.module.waste.service.impl.price;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.RecyclerPriceConfigDO;
import cn.iocoder.yudao.module.waste.dal.mysql.price.RecyclerPriceConfigMapper;
import cn.iocoder.yudao.module.waste.service.price.RecyclerPriceConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.RECYCLER_PRICE_CONFIG_NOT_EXISTS;

/**
 * 回收企业价格配置 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class RecyclerPriceConfigServiceImpl implements RecyclerPriceConfigService {

    @Resource
    private RecyclerPriceConfigMapper recyclerPriceConfigMapper;

    @Override
    public Long createRecyclerPriceConfig(@Valid RecyclerPriceConfigCreateReqVO createReqVO) {
        // 插入
        RecyclerPriceConfigDO recyclerPriceConfig = BeanUtils.toBean(createReqVO, RecyclerPriceConfigDO.class);
        recyclerPriceConfigMapper.insert(recyclerPriceConfig);
        // 返回
        return recyclerPriceConfig.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecyclerPriceConfig(@Valid RecyclerPriceConfigUpdateReqVO updateReqVO) {
        // 校验存在
        validateRecyclerPriceConfigExists(updateReqVO.getId());
        // 更新
        RecyclerPriceConfigDO updateObj = BeanUtils.toBean(updateReqVO, RecyclerPriceConfigDO.class);
        recyclerPriceConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteRecyclerPriceConfig(Long id) {
        // 校验存在
        validateRecyclerPriceConfigExists(id);
        // 删除
        recyclerPriceConfigMapper.deleteById(id);
    }

    @Override
    public RecyclerPriceConfigDO getRecyclerPriceConfig(Long id) {
        return recyclerPriceConfigMapper.selectById(id);
    }

    @Override
    public RecyclerPriceConfigRespVO getRecyclerPriceConfigDetail(Long id) {
        RecyclerPriceConfigDO recyclerPriceConfig = validateRecyclerPriceConfigExists(id);
        return BeanUtils.toBean(recyclerPriceConfig, RecyclerPriceConfigRespVO.class);
    }

    @Override
    public PageResult<RecyclerPriceConfigRespVO> getRecyclerPriceConfigPage(RecyclerPriceConfigPageReqVO pageReqVO) {
        PageResult<RecyclerPriceConfigDO> pageResult = recyclerPriceConfigMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, RecyclerPriceConfigRespVO.class);
    }

    @Override
    public List<RecyclerPriceConfigDO> getRecyclerPriceConfigList(RecyclerPriceConfigPageReqVO exportReqVO) {
        return recyclerPriceConfigMapper.selectList(exportReqVO);
    }

    @Override
    public List<RecyclerPriceConfigDO> getRecyclerPriceConfigListByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return recyclerPriceConfigMapper.selectListByRecyclingEnterpriseId(recyclingEnterpriseId);
    }

    @Override
    public List<RecyclerPriceConfigDO> getRecyclerPriceConfigListByWasteCode(String wasteCode) {
        return recyclerPriceConfigMapper.selectListByWasteCode(wasteCode);
    }

    @Override
    public List<RecyclerPriceConfigDO> getRecyclerPriceConfigListByRegionCode(String regionCode) {
        return recyclerPriceConfigMapper.selectListByRegionCode(regionCode);
    }

    @Override
    public List<RecyclerPriceConfigDO> getRecyclerPriceConfigListByStatus(Integer status) {
        return recyclerPriceConfigMapper.selectListByStatus(status);
    }

    @Override
    public List<RecyclerPriceConfigDO> getEffectiveRecyclerPriceConfigs() {
        return recyclerPriceConfigMapper.selectEffectiveConfigs();
    }

    @Override
    public RecyclerPriceConfigDO getEffectivePriceConfigByEnterpriseWasteAndRegion(Long recyclingEnterpriseId, String wasteCode, String regionCode) {
        return recyclerPriceConfigMapper.selectEffectiveByEnterpriseWasteAndRegion(recyclingEnterpriseId, wasteCode, regionCode);
    }

    @Override
    public RecyclerPriceConfigDO getEffectivePriceConfigByEnterpriseAndWasteWithRegionPriority(Long recyclingEnterpriseId, String wasteCode, String regionCode) {
        return recyclerPriceConfigMapper.selectEffectiveByEnterpriseAndWasteWithRegionPriority(recyclingEnterpriseId, wasteCode, regionCode);
    }

    @Override
    public List<RecyclerPriceConfigDO> getExpiredRecyclerPriceConfigs() {
        return recyclerPriceConfigMapper.selectExpiredConfigs();
    }

    @Override
    public List<RecyclerPriceConfigDO> getRecyclerPriceConfigListByEffectiveDateRange(LocalDate startDate, LocalDate endDate) {
        return recyclerPriceConfigMapper.selectListByEffectiveDateRange(startDate, endDate);
    }

    @Override
    public List<RecyclerPriceConfigDO> getNegotiableRecyclerPriceConfigs() {
        return recyclerPriceConfigMapper.selectNegotiableConfigs();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableRecyclerPriceConfig(Long id) {
        RecyclerPriceConfigDO config = validateRecyclerPriceConfigExists(id);
        config.setStatus(1); // 启用
        recyclerPriceConfigMapper.updateById(config);
        log.info("[enableRecyclerPriceConfig][启用价格配置] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableRecyclerPriceConfig(Long id) {
        RecyclerPriceConfigDO config = validateRecyclerPriceConfigExists(id);
        config.setStatus(0); // 禁用
        recyclerPriceConfigMapper.updateById(config);
        log.info("[disableRecyclerPriceConfig][禁用价格配置] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        for (Long id : ids) {
            RecyclerPriceConfigDO config = validateRecyclerPriceConfigExists(id);
            config.setStatus(status);
            recyclerPriceConfigMapper.updateById(config);
        }
        log.info("[batchUpdateStatus][批量更新价格配置状态] ids={}, status={}", ids, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoExpireRecyclerPriceConfigs() {
        List<RecyclerPriceConfigDO> expiredConfigs = getExpiredRecyclerPriceConfigs();
        for (RecyclerPriceConfigDO config : expiredConfigs) {
            config.setStatus(2); // 已过期
            recyclerPriceConfigMapper.updateById(config);
        }
        log.info("[autoExpireRecyclerPriceConfigs][自动过期价格配置] count={}", expiredConfigs.size());
    }

    @Override
    public RecyclerPriceConfigDO validateRecyclerPriceConfigExists(Long id) {
        RecyclerPriceConfigDO recyclerPriceConfig = recyclerPriceConfigMapper.selectById(id);
        if (recyclerPriceConfig == null) {
            throw exception(RECYCLER_PRICE_CONFIG_NOT_EXISTS);
        }
        return recyclerPriceConfig;
    }

    @Override
    public RecyclerPriceConfigDO getEffectiveConfig(Long enterpriseId, String wasteCode, String region) {
        return recyclerPriceConfigMapper.selectEffectiveByEnterpriseWasteAndRegion(enterpriseId, wasteCode, region);
    }

    @Override
    public List<RecyclerPriceConfigDO> getConfigsByEnterpriseId(Long enterpriseId) {
        return recyclerPriceConfigMapper.selectListByRecyclingEnterpriseId(enterpriseId);
    }

    @Override
    public List<RecyclerPriceConfigDO> getConfigsByWasteCode(String wasteCode) {
        return recyclerPriceConfigMapper.selectListByWasteCode(wasteCode);
    }

    @Override
    public List<RecyclerPriceConfigDO> getConfigsByRegion(String region) {
        return recyclerPriceConfigMapper.selectListByRegionCode(region);
    }

    @Override
    public List<RecyclerPriceConfigDO> getEffectiveConfigs() {
        return recyclerPriceConfigMapper.selectEffectiveConfigs();
    }

    @Override
    public List<RecyclerPriceConfigDO> getNegotiableConfigs() {
        return recyclerPriceConfigMapper.selectNegotiableConfigs();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoExpireProcess() {
        autoExpireRecyclerPriceConfigs();
    }

} 