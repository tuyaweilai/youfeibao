package cn.iocoder.yudao.module.waste.service.impl.price;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkCreateReqVO;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.PriceBenchmarkDO;
import cn.iocoder.yudao.module.waste.dal.mysql.price.PriceBenchmarkMapper;
import cn.iocoder.yudao.module.waste.service.price.PriceBenchmarkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.PRICE_BENCHMARK_NOT_EXISTS;

/**
 * 危险废物市场价格基准 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class PriceBenchmarkServiceImpl implements PriceBenchmarkService {

    @Resource
    private PriceBenchmarkMapper priceBenchmarkMapper;

    @Override
    public Long createPriceBenchmark(@Valid PriceBenchmarkCreateReqVO createReqVO) {
        // 插入
        PriceBenchmarkDO priceBenchmark = BeanUtils.toBean(createReqVO, PriceBenchmarkDO.class);
        priceBenchmarkMapper.insert(priceBenchmark);
        // 返回
        return priceBenchmark.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePriceBenchmark(@Valid PriceBenchmarkUpdateReqVO updateReqVO) {
        // 校验存在
        validatePriceBenchmarkExists(updateReqVO.getId());
        // 更新
        PriceBenchmarkDO updateObj = BeanUtils.toBean(updateReqVO, PriceBenchmarkDO.class);
        priceBenchmarkMapper.updateById(updateObj);
    }

    @Override
    public void deletePriceBenchmark(Long id) {
        // 校验存在
        validatePriceBenchmarkExists(id);
        // 删除
        priceBenchmarkMapper.deleteById(id);
    }

    @Override
    public PriceBenchmarkDO getPriceBenchmark(Long id) {
        return priceBenchmarkMapper.selectById(id);
    }

    @Override
    public PriceBenchmarkRespVO getPriceBenchmarkDetail(Long id) {
        PriceBenchmarkDO priceBenchmark = validatePriceBenchmarkExists(id);
        return BeanUtils.toBean(priceBenchmark, PriceBenchmarkRespVO.class);
    }

    @Override
    public PageResult<PriceBenchmarkRespVO> getPriceBenchmarkPage(PriceBenchmarkPageReqVO pageReqVO) {
        PageResult<PriceBenchmarkDO> pageResult = priceBenchmarkMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, PriceBenchmarkRespVO.class);
    }

    @Override
    public List<PriceBenchmarkDO> getPriceBenchmarkList(PriceBenchmarkPageReqVO exportReqVO) {
        return priceBenchmarkMapper.selectList(new LambdaQueryWrapperX<PriceBenchmarkDO>().likeIfPresent(PriceBenchmarkDO::getWasteCode, exportReqVO.getWasteCode()).likeIfPresent(PriceBenchmarkDO::getWasteName, exportReqVO.getWasteName()).eqIfPresent(PriceBenchmarkDO::getRegionCode, exportReqVO.getRegionCode()).likeIfPresent(PriceBenchmarkDO::getRegionName, exportReqVO.getRegionName()).eqIfPresent(PriceBenchmarkDO::getStatus, exportReqVO.getStatus()).likeIfPresent(PriceBenchmarkDO::getPriceSource, exportReqVO.getSource()).betweenIfPresent(PriceBenchmarkDO::getEffectiveDate, exportReqVO.getEffectiveDate()).betweenIfPresent(PriceBenchmarkDO::getExpireDate, exportReqVO.getExpiryDate()).betweenIfPresent(PriceBenchmarkDO::getCreateTime, exportReqVO.getCreateTime()).orderByDesc(PriceBenchmarkDO::getEffectiveDate));
    }

    @Override
    public List<PriceBenchmarkDO> getPriceBenchmarkListByWasteCode(String wasteCode) {
        return priceBenchmarkMapper.selectListByWasteCode(wasteCode);
    }

    @Override
    public List<PriceBenchmarkDO> getPriceBenchmarkListByRegionCode(String regionCode) {
        return priceBenchmarkMapper.selectListByRegionCode(regionCode);
    }

    @Override
    public List<PriceBenchmarkDO> getPriceBenchmarkListByStatus(Integer status) {
        return priceBenchmarkMapper.selectListByStatus(status);
    }

    @Override
    public List<PriceBenchmarkDO> getEffectivePriceBenchmarks() {
        return priceBenchmarkMapper.selectEffectiveBenchmarks();
    }

    @Override
    public PriceBenchmarkDO getEffectivePriceBenchmarkByWasteCodeAndRegion(String wasteCode, String regionCode) {
        return priceBenchmarkMapper.selectEffectiveByWasteCodeAndRegion(wasteCode, regionCode);
    }

    @Override
    public PriceBenchmarkDO getEffectivePriceBenchmarkByWasteCodeWithRegionPriority(String wasteCode, String regionCode) {
        return priceBenchmarkMapper.selectEffectiveByWasteCodeWithRegionPriority(wasteCode, regionCode);
    }

    @Override
    public List<PriceBenchmarkDO> getExpiredPriceBenchmarks() {
        return priceBenchmarkMapper.selectExpiredBenchmarks();
    }

    @Override
    public List<PriceBenchmarkDO> getPriceBenchmarkListByEffectiveDateRange(LocalDate startDate, LocalDate endDate) {
        return priceBenchmarkMapper.selectListByEffectiveDateRange(startDate, endDate);
    }

    @Override
    public List<PriceBenchmarkDO> getPriceBenchmarkListBySource(String source) {
        return priceBenchmarkMapper.selectListBySource(source);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enablePriceBenchmark(Long id) {
        PriceBenchmarkDO benchmark = validatePriceBenchmarkExists(id);
        benchmark.setStatus(1); // 启用
        priceBenchmarkMapper.updateById(benchmark);
        log.info("[enablePriceBenchmark][启用价格基准] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disablePriceBenchmark(Long id) {
        PriceBenchmarkDO benchmark = validatePriceBenchmarkExists(id);
        benchmark.setStatus(0); // 禁用
        priceBenchmarkMapper.updateById(benchmark);
        log.info("[disablePriceBenchmark][禁用价格基准] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        for (Long id : ids) {
            PriceBenchmarkDO benchmark = validatePriceBenchmarkExists(id);
            benchmark.setStatus(status);
            priceBenchmarkMapper.updateById(benchmark);
        }
        log.info("[batchUpdateStatus][批量更新价格基准状态] ids={}, status={}", ids, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoExpirePriceBenchmarks() {
        List<PriceBenchmarkDO> expiredBenchmarks = getExpiredPriceBenchmarks();
        for (PriceBenchmarkDO benchmark : expiredBenchmarks) {
            benchmark.setStatus(2); // 已过期
            priceBenchmarkMapper.updateById(benchmark);
        }
        log.info("[autoExpirePriceBenchmarks][自动过期价格基准] count={}", expiredBenchmarks.size());
    }

    @Override
    public PriceBenchmarkDO validatePriceBenchmarkExists(Long id) {
        PriceBenchmarkDO priceBenchmark = priceBenchmarkMapper.selectById(id);
        if (priceBenchmark == null) {
            throw exception(PRICE_BENCHMARK_NOT_EXISTS);
        }
        return priceBenchmark;
    }

    @Override
    public PriceBenchmarkDO getEffectivePrice(String wasteCode, String region) {
        return priceBenchmarkMapper.selectEffectiveByWasteCodeAndRegion(wasteCode, region);
    }

    @Override
    public List<PriceBenchmarkDO> getPricesByWasteCode(String wasteCode) {
        return priceBenchmarkMapper.selectListByWasteCode(wasteCode);
    }

    @Override
    public List<PriceBenchmarkDO> getPricesByRegion(String region) {
        return priceBenchmarkMapper.selectListByRegionCode(region);
    }

    @Override
    public List<PriceBenchmarkDO> getEffectivePrices() {
        return priceBenchmarkMapper.selectEffectiveBenchmarks();
    }

    @Override
    public List<PriceBenchmarkDO> getExpiredPrices() {
        return priceBenchmarkMapper.selectExpiredBenchmarks();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoExpireProcess() {
        autoExpirePriceBenchmarks();
    }

} 