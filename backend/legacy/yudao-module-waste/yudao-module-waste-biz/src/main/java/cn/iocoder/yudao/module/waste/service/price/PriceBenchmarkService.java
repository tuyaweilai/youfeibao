package cn.iocoder.yudao.module.waste.service.price;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.PriceBenchmarkDO;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * 危险废物市场价格基准 Service 接口
 *
 * @author 芋道源码
 */
public interface PriceBenchmarkService {

    /**
     * 创建危险废物市场价格基准
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPriceBenchmark(@Valid PriceBenchmarkCreateReqVO createReqVO);

    /**
     * 更新危险废物市场价格基准
     *
     * @param updateReqVO 更新信息
     */
    void updatePriceBenchmark(@Valid PriceBenchmarkUpdateReqVO updateReqVO);

    /**
     * 删除危险废物市场价格基准
     *
     * @param id 编号
     */
    void deletePriceBenchmark(Long id);

    /**
     * 获得危险废物市场价格基准
     *
     * @param id 编号
     * @return 危险废物市场价格基准
     */
    PriceBenchmarkDO getPriceBenchmark(Long id);

    /**
     * 获得危险废物市场价格基准详情
     *
     * @param id 编号
     * @return 危险废物市场价格基准详情
     */
    PriceBenchmarkRespVO getPriceBenchmarkDetail(Long id);

    /**
     * 获得危险废物市场价格基准分页
     *
     * @param pageReqVO 分页查询
     * @return 危险废物市场价格基准分页
     */
    PageResult<PriceBenchmarkRespVO> getPriceBenchmarkPage(PriceBenchmarkPageReqVO pageReqVO);

    /**
     * 获得危险废物市场价格基准列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 危险废物市场价格基准列表
     */
    List<PriceBenchmarkDO> getPriceBenchmarkList(PriceBenchmarkPageReqVO exportReqVO);

    /**
     * 根据废物代码获得价格基准列表
     *
     * @param wasteCode 废物代码
     * @return 价格基准列表
     */
    List<PriceBenchmarkDO> getPriceBenchmarkListByWasteCode(String wasteCode);

    /**
     * 根据地区代码获得价格基准列表
     *
     * @param regionCode 地区代码
     * @return 价格基准列表
     */
    List<PriceBenchmarkDO> getPriceBenchmarkListByRegionCode(String regionCode);

    /**
     * 根据状态获得价格基准列表
     *
     * @param status 状态
     * @return 价格基准列表
     */
    List<PriceBenchmarkDO> getPriceBenchmarkListByStatus(Integer status);

    /**
     * 获得生效中的价格基准列表
     *
     * @return 生效中的价格基准列表
     */
    List<PriceBenchmarkDO> getEffectivePriceBenchmarks();

    /**
     * 根据废物代码和地区获得生效价格基准
     *
     * @param wasteCode 废物代码
     * @param regionCode 地区代码
     * @return 生效价格基准
     */
    PriceBenchmarkDO getEffectivePriceBenchmarkByWasteCodeAndRegion(String wasteCode, String regionCode);

    /**
     * 根据废物代码获得生效价格基准（按地区优先级）
     *
     * @param wasteCode 废物代码
     * @param regionCode 地区代码
     * @return 生效价格基准
     */
    PriceBenchmarkDO getEffectivePriceBenchmarkByWasteCodeWithRegionPriority(String wasteCode, String regionCode);

    /**
     * 获得已过期的价格基准列表
     *
     * @return 已过期的价格基准列表
     */
    List<PriceBenchmarkDO> getExpiredPriceBenchmarks();

    /**
     * 根据生效日期范围获得价格基准列表
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 价格基准列表
     */
    List<PriceBenchmarkDO> getPriceBenchmarkListByEffectiveDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 根据来源获得价格基准列表
     *
     * @param source 来源
     * @return 价格基准列表
     */
    List<PriceBenchmarkDO> getPriceBenchmarkListBySource(String source);

    /**
     * 启用价格基准
     *
     * @param id 价格基准ID
     */
    void enablePriceBenchmark(Long id);

    /**
     * 禁用价格基准
     *
     * @param id 价格基准ID
     */
    void disablePriceBenchmark(Long id);

    /**
     * 批量更新价格基准状态
     *
     * @param ids 价格基准ID列表
     * @param status 状态
     */
    void batchUpdateStatus(List<Long> ids, Integer status);

    /**
     * 自动过期价格基准
     */
    void autoExpirePriceBenchmarks();

    /**
     * 校验价格基准是否存在
     *
     * @param id 价格基准ID
     * @return 价格基准信息
     */
    PriceBenchmarkDO validatePriceBenchmarkExists(Long id);

    /**
     * 获取生效价格基准
     *
     * @param wasteCode 废物代码
     * @param region 地区
     * @return 生效价格基准
     */
    PriceBenchmarkDO getEffectivePrice(String wasteCode, String region);

    /**
     * 根据废物代码获取价格基准
     *
     * @param wasteCode 废物代码
     * @return 价格基准列表
     */
    List<PriceBenchmarkDO> getPricesByWasteCode(String wasteCode);

    /**
     * 根据地区获取价格基准
     *
     * @param region 地区
     * @return 价格基准列表
     */
    List<PriceBenchmarkDO> getPricesByRegion(String region);

    /**
     * 获取所有生效的价格基准
     *
     * @return 生效的价格基准列表
     */
    List<PriceBenchmarkDO> getEffectivePrices();

    /**
     * 获取已过期的价格基准
     *
     * @return 已过期的价格基准列表
     */
    List<PriceBenchmarkDO> getExpiredPrices();

    /**
     * 自动过期处理
     */
    void autoExpireProcess();

} 