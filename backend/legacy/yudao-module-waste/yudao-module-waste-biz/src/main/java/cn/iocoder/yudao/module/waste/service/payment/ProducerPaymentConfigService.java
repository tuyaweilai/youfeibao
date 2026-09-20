package cn.iocoder.yudao.module.waste.service.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.ProducerPaymentConfigDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 产废企业付款配置 Service 接口
 *
 * @author 芋道源码
 */
public interface ProducerPaymentConfigService {

    /**
     * 创建产废企业付款配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createProducerPaymentConfig(@Valid ProducerPaymentConfigCreateReqVO createReqVO);

    /**
     * 更新产废企业付款配置
     *
     * @param updateReqVO 更新信息
     */
    void updateProducerPaymentConfig(@Valid ProducerPaymentConfigUpdateReqVO updateReqVO);

    /**
     * 删除产废企业付款配置
     *
     * @param id 编号
     */
    void deleteProducerPaymentConfig(Long id);

    /**
     * 获得产废企业付款配置
     *
     * @param id 编号
     * @return 产废企业付款配置
     */
    ProducerPaymentConfigDO getProducerPaymentConfig(Long id);

    /**
     * 获得产废企业付款配置详情
     *
     * @param id 编号
     * @return 产废企业付款配置详情
     */
    ProducerPaymentConfigRespVO getProducerPaymentConfigDetail(Long id);

    /**
     * 获得产废企业付款配置分页
     *
     * @param pageReqVO 分页查询
     * @return 产废企业付款配置分页
     */
    PageResult<ProducerPaymentConfigDO> getProducerPaymentConfigPage(ProducerPaymentConfigPageReqVO pageReqVO);

    /**
     * 获得产废企业付款配置列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 产废企业付款配置列表
     */
    List<ProducerPaymentConfigDO> getProducerPaymentConfigList(ProducerPaymentConfigPageReqVO exportReqVO);

    /**
     * 根据企业ID获得付款配置
     *
     * @param enterpriseId 企业ID
     * @return 付款配置
     */
    ProducerPaymentConfigDO getProducerPaymentConfigByEnterpriseId(Long enterpriseId);

    /**
     * 根据门店ID获得付款配置列表
     *
     * @param storeId 门店ID
     * @return 付款配置列表
     */
    List<ProducerPaymentConfigDO> getProducerPaymentConfigListByStoreId(Long storeId);

    /**
     * 根据付款方式获得付款配置列表
     *
     * @param paymentMethod 付款方式
     * @return 付款配置列表
     */
    List<ProducerPaymentConfigDO> getProducerPaymentConfigListByPaymentMethod(Integer paymentMethod);

    /**
     * 获得默认付款配置列表
     *
     * @return 默认付款配置列表
     */
    List<ProducerPaymentConfigDO> getDefaultProducerPaymentConfigs();

    /**
     * 获得有效的付款配置列表
     *
     * @return 有效的付款配置列表
     */
    List<ProducerPaymentConfigDO> getValidProducerPaymentConfigs();

    /**
     * 获得自动付款配置列表
     *
     * @return 自动付款配置列表
     */
    List<ProducerPaymentConfigDO> getAutoPaymentConfigs();

    /**
     * 获得对公结算配置列表
     *
     * @return 对公结算配置列表
     */
    List<ProducerPaymentConfigDO> getCorporateSettlementConfigs();

    /**
     * 获得个人结算配置列表
     *
     * @return 个人结算配置列表
     */
    List<ProducerPaymentConfigDO> getPersonalSettlementConfigs();

    /**
     * 启用付款配置
     *
     * @param id 配置ID
     */
    void enableProducerPaymentConfig(Long id);

    /**
     * 禁用付款配置
     *
     * @param id 配置ID
     */
    void disableProducerPaymentConfig(Long id);

    /**
     * 设置为默认配置
     *
     * @param id 配置ID
     */
    void setAsDefaultConfig(Long id);

    /**
     * 取消默认配置
     *
     * @param id 配置ID
     */
    void unsetDefaultConfig(Long id);

    /**
     * 校验付款配置是否存在
     *
     * @param id 配置ID
     * @return 付款配置信息
     */
    ProducerPaymentConfigDO validateProducerPaymentConfigExists(Long id);

    /**
     * 根据企业ID获取配置
     *
     * @param enterpriseId 企业ID
     * @return 配置信息
     */
    ProducerPaymentConfigDO getConfigByEnterpriseId(Long enterpriseId);

    /**
     * 获取有效配置
     *
     * @return 有效配置列表
     */
    List<ProducerPaymentConfigDO> getValidConfigs();

    /**
     * 获取默认配置
     *
     * @return 默认配置列表
     */
    List<ProducerPaymentConfigDO> getDefaultConfigs();

    /**
     * 启用配置
     *
     * @param id 配置ID
     */
    void enableConfig(Long id);

    /**
     * 禁用配置
     *
     * @param id 配置ID
     */
    void disableConfig(Long id);

    /**
     * 设置默认配置
     *
     * @param id 配置ID
     */
    void setDefault(Long id);

    /**
     * 取消默认配置
     *
     * @param id 配置ID
     */
    void unsetDefault(Long id);

    /**
     * 设置默认配置（带企业ID参数）
     *
     * @param id 配置ID
     * @param enterpriseId 企业ID
     */
    void setDefaultConfig(Long id, Long enterpriseId);

    /**
     * 根据企业ID获取配置列表
     *
     * @param enterpriseId 企业ID
     * @return 配置列表
     */
    List<ProducerPaymentConfigDO> getConfigsByEnterpriseId(Long enterpriseId);

    /**
     * 获取默认配置（带企业ID参数）
     *
     * @param enterpriseId 企业ID
     * @return 默认配置
     */
    ProducerPaymentConfigDO getDefaultConfig(Long enterpriseId);

    /**
     * 根据企业ID和付款方式获取配置列表
     *
     * @param enterpriseId 企业ID
     * @param paymentMethod 付款方式
     * @return 配置列表
     */
    List<ProducerPaymentConfigDO> getConfigsByPaymentMethod(Long enterpriseId, Integer paymentMethod);

    /**
     * 批量更新状态
     *
     * @param ids 配置ID列表
     * @param status 状态
     */
    void batchUpdateStatus(List<Long> ids, Integer status);

} 