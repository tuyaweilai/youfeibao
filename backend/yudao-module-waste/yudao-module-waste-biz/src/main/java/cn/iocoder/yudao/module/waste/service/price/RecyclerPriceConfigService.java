package cn.iocoder.yudao.module.waste.service.price;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.RecyclerPriceConfigDO;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * 回收企业价格配置 Service 接口
 *
 * @author 芋道源码
 */
public interface RecyclerPriceConfigService {

    /**
     * 创建回收企业价格配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createRecyclerPriceConfig(@Valid RecyclerPriceConfigCreateReqVO createReqVO);

    /**
     * 更新回收企业价格配置
     *
     * @param updateReqVO 更新信息
     */
    void updateRecyclerPriceConfig(@Valid RecyclerPriceConfigUpdateReqVO updateReqVO);

    /**
     * 删除回收企业价格配置
     *
     * @param id 编号
     */
    void deleteRecyclerPriceConfig(Long id);

    /**
     * 获得回收企业价格配置
     *
     * @param id 编号
     * @return 回收企业价格配置
     */
    RecyclerPriceConfigDO getRecyclerPriceConfig(Long id);

    /**
     * 获得回收企业价格配置详情
     *
     * @param id 编号
     * @return 回收企业价格配置详情
     */
    RecyclerPriceConfigRespVO getRecyclerPriceConfigDetail(Long id);

    /**
     * 获得回收企业价格配置分页
     *
     * @param pageReqVO 分页查询
     * @return 回收企业价格配置分页
     */
    PageResult<RecyclerPriceConfigRespVO> getRecyclerPriceConfigPage(RecyclerPriceConfigPageReqVO pageReqVO);

    /**
     * 获得回收企业价格配置列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 回收企业价格配置列表
     */
    List<RecyclerPriceConfigDO> getRecyclerPriceConfigList(RecyclerPriceConfigPageReqVO exportReqVO);

    /**
     * 根据回收企业ID获得价格配置列表
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 价格配置列表
     */
    List<RecyclerPriceConfigDO> getRecyclerPriceConfigListByRecyclingEnterpriseId(Long recyclingEnterpriseId);

    /**
     * 根据废物代码获得价格配置列表
     *
     * @param wasteCode 废物代码
     * @return 价格配置列表
     */
    List<RecyclerPriceConfigDO> getRecyclerPriceConfigListByWasteCode(String wasteCode);

    /**
     * 根据地区代码获得价格配置列表
     *
     * @param regionCode 地区代码
     * @return 价格配置列表
     */
    List<RecyclerPriceConfigDO> getRecyclerPriceConfigListByRegionCode(String regionCode);

    /**
     * 根据状态获得价格配置列表
     *
     * @param status 状态
     * @return 价格配置列表
     */
    List<RecyclerPriceConfigDO> getRecyclerPriceConfigListByStatus(Integer status);

    /**
     * 获得生效中的价格配置列表
     *
     * @return 生效中的价格配置列表
     */
    List<RecyclerPriceConfigDO> getEffectiveRecyclerPriceConfigs();

    /**
     * 根据企业、废物代码和地区获得生效价格配置
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param wasteCode 废物代码
     * @param regionCode 地区代码
     * @return 生效价格配置
     */
    RecyclerPriceConfigDO getEffectivePriceConfigByEnterpriseWasteAndRegion(Long recyclingEnterpriseId, String wasteCode, String regionCode);

    /**
     * 根据企业和废物代码获得生效价格配置（按地区优先级）
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param wasteCode 废物代码
     * @param regionCode 地区代码
     * @return 生效价格配置
     */
    RecyclerPriceConfigDO getEffectivePriceConfigByEnterpriseAndWasteWithRegionPriority(Long recyclingEnterpriseId, String wasteCode, String regionCode);

    /**
     * 获得已过期的价格配置列表
     *
     * @return 已过期的价格配置列表
     */
    List<RecyclerPriceConfigDO> getExpiredRecyclerPriceConfigs();

    /**
     * 根据生效日期范围获得价格配置列表
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 价格配置列表
     */
    List<RecyclerPriceConfigDO> getRecyclerPriceConfigListByEffectiveDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 获得议价配置列表
     *
     * @return 议价配置列表
     */
    List<RecyclerPriceConfigDO> getNegotiableRecyclerPriceConfigs();

    /**
     * 启用价格配置
     *
     * @param id 价格配置ID
     */
    void enableRecyclerPriceConfig(Long id);

    /**
     * 禁用价格配置
     *
     * @param id 价格配置ID
     */
    void disableRecyclerPriceConfig(Long id);

    /**
     * 批量更新价格配置状态
     *
     * @param ids 价格配置ID列表
     * @param status 状态
     */
    void batchUpdateStatus(List<Long> ids, Integer status);

    /**
     * 自动过期价格配置
     */
    void autoExpireRecyclerPriceConfigs();

    /**
     * 校验价格配置是否存在
     *
     * @param id 价格配置ID
     * @return 价格配置信息
     */
    RecyclerPriceConfigDO validateRecyclerPriceConfigExists(Long id);

    /**
     * 获取生效价格配置
     *
     * @param enterpriseId 企业ID
     * @param wasteCode 废物代码
     * @param region 地区
     * @return 生效价格配置
     */
    RecyclerPriceConfigDO getEffectiveConfig(Long enterpriseId, String wasteCode, String region);

    /**
     * 根据企业ID获取价格配置
     *
     * @param enterpriseId 企业ID
     * @return 价格配置列表
     */
    List<RecyclerPriceConfigDO> getConfigsByEnterpriseId(Long enterpriseId);

    /**
     * 根据废物代码获取价格配置
     *
     * @param wasteCode 废物代码
     * @return 价格配置列表
     */
    List<RecyclerPriceConfigDO> getConfigsByWasteCode(String wasteCode);

    /**
     * 根据地区获取价格配置
     *
     * @param region 地区
     * @return 价格配置列表
     */
    List<RecyclerPriceConfigDO> getConfigsByRegion(String region);

    /**
     * 获取所有生效的价格配置
     *
     * @return 生效的价格配置列表
     */
    List<RecyclerPriceConfigDO> getEffectiveConfigs();

    /**
     * 获取议价配置
     *
     * @return 议价配置列表
     */
    List<RecyclerPriceConfigDO> getNegotiableConfigs();

    /**
     * 自动过期处理
     */
    void autoExpireProcess();

} 