package cn.iocoder.yudao.module.waste.service.quotation;

import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerBusinessConfigDO;

import java.util.List;

/**
 * 业务模式配置管理服务接口
 *
 * @author 芋道源码
 */
public interface BusinessModeConfigService {

    /**
     * 配置企业业务模式
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param businessMode 业务模式：0-独立运营，1-平台竞价，2-混合模式
     * @param defaultQuotationMode 默认报价模式：1-自动报价，2-手动报价
     * @param allowClientModeSelection 是否允许客户选择模式
     * @param quotationTimeoutHours 报价超时时间（小时）
     * @param autoAcceptSingleQuotation 是否自动接受单一报价
     * @param enablePriceNegotiation 是否启用价格协商
     * @return 配置ID
     */
    Long configureBusinessMode(Long recyclingEnterpriseId, Integer businessMode, 
                              Integer defaultQuotationMode, Boolean allowClientModeSelection,
                              Integer quotationTimeoutHours, Boolean autoAcceptSingleQuotation,
                              Boolean enablePriceNegotiation);

    /**
     * 获取企业业务模式配置
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 业务模式配置
     */
    RecyclerBusinessConfigDO getBusinessModeConfig(Long recyclingEnterpriseId);

    /**
     * 更新企业业务模式
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param businessMode 新的业务模式
     */
    void updateBusinessMode(Long recyclingEnterpriseId, Integer businessMode);

    /**
     * 启用/禁用企业业务配置
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param enabled 是否启用
     */
    void toggleBusinessConfig(Long recyclingEnterpriseId, Boolean enabled);

    /**
     * 获取指定业务模式的企业列表
     *
     * @param businessMode 业务模式
     * @return 企业配置列表
     */
    List<RecyclerBusinessConfigDO> getEnterprisesByBusinessMode(Integer businessMode);

    /**
     * 获取支持竞价模式的企业列表
     *
     * @return 支持竞价的企业配置列表
     */
    List<RecyclerBusinessConfigDO> getBiddingModeEnterprises();

    /**
     * 获取支持独立运营模式的企业列表
     *
     * @return 支持独立运营的企业配置列表
     */
    List<RecyclerBusinessConfigDO> getIndependentModeEnterprises();

    /**
     * 获取支持混合模式的企业列表
     *
     * @return 支持混合模式的企业配置列表
     */
    List<RecyclerBusinessConfigDO> getHybridModeEnterprises();

    /**
     * 检查企业是否支持指定业务模式
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param businessMode 业务模式
     * @return 是否支持
     */
    boolean supportsBusinessMode(Long recyclingEnterpriseId, Integer businessMode);

    /**
     * 获取企业的默认报价模式
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 默认报价模式：1-自动报价，2-手动报价
     */
    Integer getDefaultQuotationMode(Long recyclingEnterpriseId);

    /**
     * 检查企业是否允许客户选择业务模式
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 是否允许客户选择
     */
    boolean allowsClientModeSelection(Long recyclingEnterpriseId);

    /**
     * 获取企业报价超时时间
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 超时时间（小时）
     */
    Integer getQuotationTimeoutHours(Long recyclingEnterpriseId);

    /**
     * 检查企业是否自动接受单一报价
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 是否自动接受
     */
    boolean autoAcceptsSingleQuotation(Long recyclingEnterpriseId);

    /**
     * 检查企业是否启用价格协商
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 是否启用价格协商
     */
    boolean enablesPriceNegotiation(Long recyclingEnterpriseId);

    /**
     * 批量配置企业业务模式
     *
     * @param recyclingEnterpriseIds 回收企业ID列表
     * @param businessMode 业务模式
     * @param defaultQuotationMode 默认报价模式
     * @return 成功配置的数量
     */
    int batchConfigureBusinessMode(List<Long> recyclingEnterpriseIds, Integer businessMode, 
                                  Integer defaultQuotationMode);

    /**
     * 重置企业业务配置为默认值
     *
     * @param recyclingEnterpriseId 回收企业ID
     */
    void resetToDefaultConfig(Long recyclingEnterpriseId);

    /**
     * 验证业务模式配置的有效性
     *
     * @param config 业务配置
     * @return 验证结果
     */
    boolean validateBusinessConfig(RecyclerBusinessConfigDO config);
} 