package cn.iocoder.yudao.module.waste.service.recycler;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPriceCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPricePageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPriceRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPriceUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerCustomerPriceDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 回收企业客户专属价格配置 Service 接口
 *
 * @author 芋道源码
 */
public interface RecyclerCustomerPriceService {

    /**
     * 创建回收企业客户专属价格配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createRecyclerCustomerPrice(@Valid RecyclerCustomerPriceCreateReqVO createReqVO);

    /**
     * 更新回收企业客户专属价格配置
     *
     * @param updateReqVO 更新信息
     */
    void updateRecyclerCustomerPrice(@Valid RecyclerCustomerPriceUpdateReqVO updateReqVO);

    /**
     * 删除回收企业客户专属价格配置
     *
     * @param id 编号
     */
    void deleteRecyclerCustomerPrice(Long id);

    /**
     * 获得回收企业客户专属价格配置
     *
     * @param id 编号
     * @return 回收企业客户专属价格配置
     */
    RecyclerCustomerPriceDO getRecyclerCustomerPrice(Long id);

    /**
     * 获得回收企业客户专属价格配置详情
     *
     * @param id 编号
     * @return 回收企业客户专属价格配置详情
     */
    RecyclerCustomerPriceRespVO getRecyclerCustomerPriceDetail(Long id);

    /**
     * 获得回收企业客户专属价格配置分页
     *
     * @param pageReqVO 分页查询
     * @return 回收企业客户专属价格配置分页
     */
    PageResult<RecyclerCustomerPriceDO> getRecyclerCustomerPricePage(RecyclerCustomerPricePageReqVO pageReqVO);

    /**
     * 获得回收企业客户专属价格配置列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 回收企业客户专属价格配置列表
     */
    List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceList(RecyclerCustomerPricePageReqVO exportReqVO);

    /**
     * 根据回收企业ID获得价格配置列表
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 价格配置列表
     */
    List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByRecyclingEnterpriseId(Long recyclingEnterpriseId);

    /**
     * 根据客户企业ID获得价格配置列表
     *
     * @param customerEnterpriseId 客户企业ID
     * @return 价格配置列表
     */
    List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByCustomerEnterpriseId(Long customerEnterpriseId);

    /**
     * 根据废物代码获得价格配置列表
     *
     * @param wasteCode 废物代码
     * @return 价格配置列表
     */
    List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByWasteCode(String wasteCode);

    /**
     * 根据价格类型获得价格配置列表
     *
     * @param priceType 价格类型
     * @return 价格配置列表
     */
    List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByPriceType(Integer priceType);

    /**
     * 获得生效中的价格配置列表
     *
     * @return 生效中的价格配置列表
     */
    List<RecyclerCustomerPriceDO> getEffectiveRecyclerCustomerPrices();

    /**
     * 获得客户和废物的生效价格
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param customerEnterpriseId 客户企业ID
     * @param wasteCode 废物代码
     * @return 生效价格配置
     */
    RecyclerCustomerPriceDO getEffectivePriceByCustomerAndWaste(Long recyclingEnterpriseId, Long customerEnterpriseId, String wasteCode);

    /**
     * 根据客户和回收企业获得价格配置列表
     *
     * @param customerEnterpriseId 客户企业ID
     * @param recyclingEnterpriseId 回收企业ID
     * @return 价格配置列表
     */
    List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByCustomerAndRecycler(Long customerEnterpriseId, Long recyclingEnterpriseId);

    /**
     * 根据合同ID获得价格配置列表
     *
     * @param contractId 合同ID
     * @return 价格配置列表
     */
    List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByContractId(Long contractId);

    /**
     * 获得已过期的价格配置列表
     *
     * @return 已过期的价格配置列表
     */
    List<RecyclerCustomerPriceDO> getExpiredRecyclerCustomerPrices();

    /**
     * 根据生效日期范围获得价格配置列表
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 价格配置列表
     */
    List<RecyclerCustomerPriceDO> getRecyclerCustomerPriceListByEffectiveDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 获得固定价格配置列表
     *
     * @return 固定价格配置列表
     */
    List<RecyclerCustomerPriceDO> getFixedPriceConfigs();

    /**
     * 获得浮动价格配置列表
     *
     * @return 浮动价格配置列表
     */
    List<RecyclerCustomerPriceDO> getFloatingPriceConfigs();

    /**
     * 获得阶梯价格配置列表
     *
     * @return 阶梯价格配置列表
     */
    List<RecyclerCustomerPriceDO> getTieredPriceConfigs();

    /**
     * 启用价格配置
     *
     * @param id 价格配置ID
     */
    void enableRecyclerCustomerPrice(Long id);

    /**
     * 禁用价格配置
     *
     * @param id 价格配置ID
     */
    void disableRecyclerCustomerPrice(Long id);

    /**
     * 批量更新价格配置状态
     *
     * @param ids 价格配置ID列表
     * @param status 状态
     */
    void batchUpdateStatus(List<Long> ids, Integer status);

    /**
     * 计算专属价格
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param customerEnterpriseId 客户企业ID
     * @param wasteCode 废物代码
     * @param quantity 数量
     * @return 计算后的价格
     */
    BigDecimal calculateSpecialPrice(Long recyclingEnterpriseId, Long customerEnterpriseId, String wasteCode, BigDecimal quantity);

    /**
     * 校验价格配置是否存在
     *
     * @param id 价格配置ID
     * @return 价格配置信息
     */
    RecyclerCustomerPriceDO validateRecyclerCustomerPriceExists(Long id);

    // ==================== Controller调用的方法 ====================

    /**
     * 计算价格
     *
     * @param recyclerEnterpriseId 回收企业ID
     * @param customerEnterpriseId 客户企业ID
     * @param wasteCode 废物代码
     * @param quantity 数量
     * @return 计算后的价格
     */
    BigDecimal calculatePrice(Long recyclerEnterpriseId, Long customerEnterpriseId, String wasteCode, BigDecimal quantity);

    /**
     * 根据回收企业和客户获取专属价格
     *
     * @param recyclerEnterpriseId 回收企业ID
     * @param customerEnterpriseId 客户企业ID
     * @return 专属价格列表
     */
    List<RecyclerCustomerPriceDO> getPricesByRecyclerAndCustomer(Long recyclerEnterpriseId, Long customerEnterpriseId);

    /**
     * 根据废物代码获取专属价格
     *
     * @param wasteCode 废物代码
     * @return 专属价格列表
     */
    List<RecyclerCustomerPriceDO> getPricesByWasteCode(String wasteCode);

    /**
     * 根据价格类型获取专属价格
     *
     * @param priceType 价格类型
     * @return 专属价格列表
     */
    List<RecyclerCustomerPriceDO> getPricesByPriceType(Integer priceType);

    /**
     * 获取生效的专属价格
     *
     * @return 生效的专属价格列表
     */
    List<RecyclerCustomerPriceDO> getEffectivePrices();

    /**
     * 根据合同获取专属价格
     *
     * @param contractId 合同ID
     * @return 专属价格列表
     */
    List<RecyclerCustomerPriceDO> getPricesByContract(Long contractId);

    /**
     * 设置合同关联价格
     *
     * @param id 价格配置ID
     * @param contractId 合同ID
     */
    void setContractPrice(Long id, Long contractId);

} 