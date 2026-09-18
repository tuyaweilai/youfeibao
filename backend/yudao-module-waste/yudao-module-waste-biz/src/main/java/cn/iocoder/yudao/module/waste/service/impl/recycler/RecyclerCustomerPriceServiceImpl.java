package cn.iocoder.yudao.module.waste.service.impl.recycler;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPriceCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPricePageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPriceRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPriceUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerCustomerPriceDO;
import cn.iocoder.yudao.module.waste.dal.mysql.recycler.RecyclerCustomerPriceMapper;
import cn.iocoder.yudao.module.waste.service.recycler.RecyclerCustomerPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.RECYCLER_CUSTOMER_PRICE_NOT_EXISTS;

/**
 * 回收企业客户专属价格配置 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class RecyclerCustomerPriceServiceImpl implements RecyclerCustomerPriceService {

    @Resource
    private RecyclerCustomerPriceMapper recyclerCustomerPriceMapper;

    @Override
    public Long createRecyclerCustomerPrice(@Valid RecyclerCustomerPriceCreateReqVO createReqVO) {
        // 插入
        RecyclerCustomerPriceDO recyclerCustomerPrice = BeanUtils.toBean(createReqVO, RecyclerCustomerPriceDO.class);
        recyclerCustomerPriceMapper.insert(recyclerCustomerPrice);
        // 返回
        return recyclerCustomerPrice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecyclerCustomerPrice(@Valid RecyclerCustomerPriceUpdateReqVO updateReqVO) {
        // 校验存在
        validateRecyclerCustomerPriceExists(updateReqVO.getId());
        // 更新
        RecyclerCustomerPriceDO updateObj = BeanUtils.toBean(updateReqVO, RecyclerCustomerPriceDO.class);
        recyclerCustomerPriceMapper.updateById(updateObj);
    }

    @Override
    public void deleteRecyclerCustomerPrice(Long id) {
        // 校验存在
        validateRecyclerCustomerPriceExists(id);
        // 删除
        recyclerCustomerPriceMapper.deleteById(id);
    }

    @Override
    public RecyclerCustomerPriceDO getRecyclerCustomerPrice(Long id) {
        return recyclerCustomerPriceMapper.selectById(id);
    }

    @Override
    public RecyclerCustomerPriceRespVO getRecyclerCustomerPriceDetail(Long id) {
        RecyclerCustomerPriceDO recyclerCustomerPrice = validateRecyclerCustomerPriceExists(id);
        return BeanUtils.toBean(recyclerCustomerPrice, RecyclerCustomerPriceRespVO.class);
    }

    @Override
    public PageResult<RecyclerCustomerPriceDO> getRecyclerCustomerPricePage(RecyclerCustomerPricePageReqVO pageReqVO) {
        return recyclerCustomerPriceMapper.selectPage(pageReqVO);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceList(RecyclerCustomerPricePageReqVO exportReqVO) {
        return recyclerCustomerPriceMapper.selectList(exportReqVO);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return recyclerCustomerPriceMapper.selectListByRecyclingEnterpriseId(recyclingEnterpriseId);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByCustomerEnterpriseId(Long customerEnterpriseId) {
        return recyclerCustomerPriceMapper.selectListByCustomerEnterpriseId(customerEnterpriseId);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByWasteCode(String wasteCode) {
        return recyclerCustomerPriceMapper.selectListByWasteCode(wasteCode);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByPriceType(Integer priceType) {
        return recyclerCustomerPriceMapper.selectListByPriceType(priceType);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getEffectiveRecyclerCustomerPrices() {
        return recyclerCustomerPriceMapper.selectEffectivePrices();
    }

    @Override
    public RecyclerCustomerPriceDO getEffectivePriceByCustomerAndWaste(Long recyclingEnterpriseId, Long customerEnterpriseId, String wasteCode) {
        return recyclerCustomerPriceMapper.selectEffectivePriceByCustomerAndWaste(recyclingEnterpriseId, customerEnterpriseId, wasteCode);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByCustomerAndRecycler(Long customerEnterpriseId, Long recyclingEnterpriseId) {
        return recyclerCustomerPriceMapper.selectListByCustomerAndRecycler(customerEnterpriseId, recyclingEnterpriseId);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByContractId(Long contractId) {
        return recyclerCustomerPriceMapper.selectListByContractId(contractId);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getExpiredRecyclerCustomerPrices() {
        return recyclerCustomerPriceMapper.selectExpiredPrices();
    }

    @Override
    public List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByEffectiveDateRange(LocalDate startDate, LocalDate endDate) {
        return recyclerCustomerPriceMapper.selectListByEffectiveDateRange(startDate, endDate);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getFixedPriceConfigs() {
        return recyclerCustomerPriceMapper.selectFixedPriceConfigs();
    }

    @Override
    public List<RecyclerCustomerPriceDO> getFloatingPriceConfigs() {
        return recyclerCustomerPriceMapper.selectFloatingPriceConfigs();
    }

    @Override
    public List<RecyclerCustomerPriceDO> getTieredPriceConfigs() {
        return recyclerCustomerPriceMapper.selectTieredPriceConfigs();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableRecyclerCustomerPrice(Long id) {
        RecyclerCustomerPriceDO price = validateRecyclerCustomerPriceExists(id);
        price.setStatus(1); // 启用
        recyclerCustomerPriceMapper.updateById(price);
        log.info("[enableRecyclerCustomerPrice][启用专属价格配置] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableRecyclerCustomerPrice(Long id) {
        RecyclerCustomerPriceDO price = validateRecyclerCustomerPriceExists(id);
        price.setStatus(0); // 禁用
        recyclerCustomerPriceMapper.updateById(price);
        log.info("[disableRecyclerCustomerPrice][禁用专属价格配置] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        for (Long id : ids) {
            RecyclerCustomerPriceDO price = validateRecyclerCustomerPriceExists(id);
            price.setStatus(status);
            recyclerCustomerPriceMapper.updateById(price);
        }
        log.info("[batchUpdateStatus][批量更新专属价格配置状态] ids={}, status={}", ids, status);
    }

    @Override
    public BigDecimal calculateSpecialPrice(Long recyclingEnterpriseId, Long customerEnterpriseId, String wasteCode, BigDecimal quantity) {
        RecyclerCustomerPriceDO priceConfig = getEffectivePriceByCustomerAndWaste(recyclingEnterpriseId, customerEnterpriseId, wasteCode);
        if (priceConfig == null) {
            return null;
        }
        
        BigDecimal price = priceConfig.getSpecialPrice();
        Integer priceType = priceConfig.getPriceType();
        
        // 根据价格类型计算最终价格
        if (priceType == 1) {
            // 固定价格
            return price.multiply(quantity);
        } else if (priceType == 2) {
            // 浮动价格 - 基于市场价格的浮动比例
            // TODO: 实现浮动价格计算逻辑
            return price.multiply(quantity);
        } else if (priceType == 3) {
            // 阶梯价格 - 根据数量区间计算
            // TODO: 实现阶梯价格计算逻辑
            return price.multiply(quantity);
        }
        
        return price.multiply(quantity);
    }

    @Override
    public RecyclerCustomerPriceDO validateRecyclerCustomerPriceExists(Long id) {
        RecyclerCustomerPriceDO recyclerCustomerPrice = recyclerCustomerPriceMapper.selectById(id);
        if (recyclerCustomerPrice == null) {
            throw exception(RECYCLER_CUSTOMER_PRICE_NOT_EXISTS);
        }
        return recyclerCustomerPrice;
    }

    // ==================== Controller调用的方法 ====================

    @Override
    public BigDecimal calculatePrice(Long recyclerEnterpriseId, Long customerEnterpriseId, String wasteCode, BigDecimal quantity) {
        return calculateSpecialPrice(recyclerEnterpriseId, customerEnterpriseId, wasteCode, quantity);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getPricesByRecyclerAndCustomer(Long recyclerEnterpriseId, Long customerEnterpriseId) {
        return recyclerCustomerPriceMapper.selectListByCustomerAndRecycler(customerEnterpriseId, recyclerEnterpriseId);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getPricesByWasteCode(String wasteCode) {
        return recyclerCustomerPriceMapper.selectListByWasteCode(wasteCode);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getPricesByPriceType(Integer priceType) {
        return recyclerCustomerPriceMapper.selectListByPriceType(priceType);
    }

    @Override
    public List<RecyclerCustomerPriceDO> getEffectivePrices() {
        return recyclerCustomerPriceMapper.selectEffectivePrices();
    }

    @Override
    public List<RecyclerCustomerPriceDO> getPricesByContract(Long contractId) {
        return recyclerCustomerPriceMapper.selectListByContractId(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setContractPrice(Long id, Long contractId) {
        RecyclerCustomerPriceDO price = validateRecyclerCustomerPriceExists(id);
        price.setContractId(contractId);
        recyclerCustomerPriceMapper.updateById(price);
        log.info("[setContractPrice][设置合同关联价格] id={}, contractId={}", id, contractId);
    }

} 