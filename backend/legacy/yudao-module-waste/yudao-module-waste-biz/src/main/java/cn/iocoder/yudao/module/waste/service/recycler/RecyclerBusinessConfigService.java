package cn.iocoder.yudao.module.waste.service.recycler;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerBusinessConfigDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 回收企业业务模式配置 Service 接口
 *
 * @author 芋道源码
 */
public interface RecyclerBusinessConfigService {

    /**
     * 创建回收企业业务模式配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createRecyclerBusinessConfig(@Valid RecyclerBusinessConfigCreateReqVO createReqVO);

    /**
     * 更新回收企业业务模式配置
     *
     * @param updateReqVO 更新信息
     */
    void updateRecyclerBusinessConfig(@Valid RecyclerBusinessConfigUpdateReqVO updateReqVO);

    /**
     * 删除回收企业业务模式配置
     *
     * @param id 编号
     */
    void deleteRecyclerBusinessConfig(Long id);

    /**
     * 获得回收企业业务模式配置
     *
     * @param id 编号
     * @return 回收企业业务模式配置
     */
    RecyclerBusinessConfigDO getRecyclerBusinessConfig(Long id);

    /**
     * 获得回收企业业务模式配置详情
     *
     * @param id 编号
     * @return 回收企业业务模式配置详情
     */
    RecyclerBusinessConfigRespVO getRecyclerBusinessConfigDetail(Long id);

    /**
     * 获得回收企业业务模式配置分页
     *
     * @param pageReqVO 分页查询
     * @return 回收企业业务模式配置分页
     */
    PageResult<RecyclerBusinessConfigDO> getRecyclerBusinessConfigPage(RecyclerBusinessConfigPageReqVO pageReqVO);

    /**
     * 获得回收企业业务模式配置列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 回收企业业务模式配置列表
     */
    List<RecyclerBusinessConfigDO> getRecyclerBusinessConfigList(RecyclerBusinessConfigPageReqVO exportReqVO);

    /**
     * 根据回收企业ID获得业务模式配置
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 业务模式配置
     */
    RecyclerBusinessConfigDO getRecyclerBusinessConfigByRecyclingEnterpriseId(Long recyclingEnterpriseId);

    /**
     * 根据业务模式获得配置列表
     *
     * @param businessMode 业务模式
     * @return 配置列表
     */
    List<RecyclerBusinessConfigDO> getRecyclerBusinessConfigListByBusinessMode(Integer businessMode);

    /**
     * 根据默认报价模式获得配置列表
     *
     * @param defaultQuotationMode 默认报价模式
     * @return 配置列表
     */
    List<RecyclerBusinessConfigDO> getRecyclerBusinessConfigListByDefaultQuotationMode(Integer defaultQuotationMode);

    /**
     * 获得启用的配置列表
     *
     * @return 启用的配置列表
     */
    List<RecyclerBusinessConfigDO> getEnabledRecyclerBusinessConfigs();

    /**
     * 获得禁用的配置列表
     *
     * @return 禁用的配置列表
     */
    List<RecyclerBusinessConfigDO> getDisabledRecyclerBusinessConfigs();

    /**
     * 获得竞价模式配置列表
     *
     * @return 竞价模式配置列表
     */
    List<RecyclerBusinessConfigDO> getCompetitiveBiddingConfigs();

    /**
     * 获得议价模式配置列表
     *
     * @return 议价模式配置列表
     */
    List<RecyclerBusinessConfigDO> getNegotiationConfigs();

    /**
     * 获得固定价格模式配置列表
     *
     * @return 固定价格模式配置列表
     */
    List<RecyclerBusinessConfigDO> getFixedPriceConfigs();

    /**
     * 获得自动报价配置列表
     *
     * @return 自动报价配置列表
     */
    List<RecyclerBusinessConfigDO> getAutoQuotationConfigs();

    /**
     * 获得手动报价配置列表
     *
     * @return 手动报价配置列表
     */
    List<RecyclerBusinessConfigDO> getManualQuotationConfigs();

    /**
     * 获得允许客户选择模式的配置列表
     *
     * @return 允许客户选择模式的配置列表
     */
    List<RecyclerBusinessConfigDO> getAllowClientModeSelectionConfigs();

    /**
     * 获得自动接受单一报价的配置列表
     *
     * @return 自动接受单一报价的配置列表
     */
    List<RecyclerBusinessConfigDO> getAutoAcceptSingleQuotationConfigs();

    /**
     * 获得启用价格协商的配置列表
     *
     * @return 启用价格协商的配置列表
     */
    List<RecyclerBusinessConfigDO> getPriceNegotiationEnabledConfigs();

    /**
     * 根据报价超时时间范围获得配置列表
     *
     * @param minHours 最小小时数
     * @param maxHours 最大小时数
     * @return 配置列表
     */
    List<RecyclerBusinessConfigDO> getRecyclerBusinessConfigListByQuotationTimeoutRange(Integer minHours, Integer maxHours);

    /**
     * 启用业务模式配置
     *
     * @param id 配置ID
     */
    void enableRecyclerBusinessConfig(Long id);

    /**
     * 禁用业务模式配置
     *
     * @param id 配置ID
     */
    void disableRecyclerBusinessConfig(Long id);

    /**
     * 更新业务模式
     *
     * @param id 配置ID
     * @param businessMode 业务模式
     */
    void updateBusinessMode(Long id, Integer businessMode);

    /**
     * 更新默认报价模式
     *
     * @param id 配置ID
     * @param defaultQuotationMode 默认报价模式
     */
    void updateDefaultQuotationMode(Long id, Integer defaultQuotationMode);

    /**
     * 更新报价超时时间
     *
     * @param id 配置ID
     * @param quotationTimeoutHours 报价超时时间（小时）
     */
    void updateQuotationTimeoutHours(Long id, Integer quotationTimeoutHours);

    /**
     * 设置是否允许客户选择模式
     *
     * @param id 配置ID
     * @param allowClientModeSelection 是否允许客户选择模式
     */
    void setAllowClientModeSelection(Long id, Boolean allowClientModeSelection);

    /**
     * 设置是否自动接受单一报价
     *
     * @param id 配置ID
     * @param autoAcceptSingleQuotation 是否自动接受单一报价
     */
    void setAutoAcceptSingleQuotation(Long id, Boolean autoAcceptSingleQuotation);

    /**
     * 设置是否启用价格协商
     *
     * @param id 配置ID
     * @param enablePriceNegotiation 是否启用价格协商
     */
    void setEnablePriceNegotiation(Long id, Boolean enablePriceNegotiation);

    /**
     * 校验业务模式配置是否存在
     *
     * @param id 配置ID
     * @return 业务模式配置信息
     */
    RecyclerBusinessConfigDO validateRecyclerBusinessConfigExists(Long id);

    // ==================== Controller调用的方法 ====================

    /**
     * 根据企业ID获取业务模式配置
     *
     * @param enterpriseId 企业ID
     * @return 业务模式配置
     */
    RecyclerBusinessConfigDO getConfigByEnterpriseId(Long enterpriseId);

    /**
     * 根据业务模式获取配置列表
     *
     * @param businessMode 业务模式
     * @return 配置列表
     */
    List<RecyclerBusinessConfigDO> getConfigsByBusinessMode(Integer businessMode);

    /**
     * 根据报价模式获取配置列表
     *
     * @param quotationMode 报价模式
     * @return 配置列表
     */
    List<RecyclerBusinessConfigDO> getConfigsByQuotationMode(Integer quotationMode);

    /**
     * 设置报价规则
     *
     * @param enterpriseId 企业ID
     * @param quotationTimeoutMinutes 报价超时时间（分钟）
     * @param autoAcceptEnabled 是否启用自动接受
     * @param autoAcceptThreshold 自动接受阈值
     */
    void setQuotationRules(Long enterpriseId, Integer quotationTimeoutMinutes, Boolean autoAcceptEnabled, String autoAcceptThreshold);

    /**
     * 设置价格协商配置
     *
     * @param enterpriseId 企业ID
     * @param priceNegotiationEnabled 是否启用价格协商
     * @param maxNegotiationRounds 最大协商轮数
     * @param negotiationTimeoutHours 协商超时时间（小时）
     */
    void setNegotiationConfig(Long enterpriseId, Boolean priceNegotiationEnabled, Integer maxNegotiationRounds, Integer negotiationTimeoutHours);

    /**
     * 设置客户模式选择
     *
     * @param enterpriseId 企业ID
     * @param customerModeSelection 客户模式选择
     */
    void setCustomerMode(Long enterpriseId, Integer customerModeSelection);

} 