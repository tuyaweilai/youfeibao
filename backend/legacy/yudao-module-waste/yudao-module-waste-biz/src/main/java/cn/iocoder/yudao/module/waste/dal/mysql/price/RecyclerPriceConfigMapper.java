package cn.iocoder.yudao.module.waste.dal.mysql.price;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.RecyclerPriceConfigPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.RecyclerPriceConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * 回收企业价格配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface RecyclerPriceConfigMapper extends BaseMapperX<RecyclerPriceConfigDO> {

    default PageResult<RecyclerPriceConfigDO> selectPage(RecyclerPriceConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RecyclerPriceConfigDO>()
                .eqIfPresent(RecyclerPriceConfigDO::getRecyclingEnterpriseId, reqVO.getRecyclingEnterpriseId())
                .likeIfPresent(RecyclerPriceConfigDO::getWasteCode, reqVO.getWasteCode())
                .likeIfPresent(RecyclerPriceConfigDO::getWasteName, reqVO.getWasteName())
                .eqIfPresent(RecyclerPriceConfigDO::getRegionCode, reqVO.getRegionCode())
                .likeIfPresent(RecyclerPriceConfigDO::getRegionName, reqVO.getRegionName())
                .eqIfPresent(RecyclerPriceConfigDO::getStatus, reqVO.getStatus())
                .eqIfPresent(RecyclerPriceConfigDO::getIsNegotiable, reqVO.getNegotiable())
                .geIfPresent(RecyclerPriceConfigDO::getPurchasePrice, reqVO.getMinBasePrice())
                .leIfPresent(RecyclerPriceConfigDO::getPurchasePrice, reqVO.getMaxBasePrice())
                .betweenIfPresent(RecyclerPriceConfigDO::getEffectiveDate, reqVO.getEffectiveDate())
                .betweenIfPresent(RecyclerPriceConfigDO::getExpireDate, reqVO.getExpiryDate())
                .betweenIfPresent(RecyclerPriceConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(RecyclerPriceConfigDO::getEffectiveDate));
    }

    default List<RecyclerPriceConfigDO> selectList(RecyclerPriceConfigPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<RecyclerPriceConfigDO>()
                .eqIfPresent(RecyclerPriceConfigDO::getRecyclingEnterpriseId, reqVO.getRecyclingEnterpriseId())
                .likeIfPresent(RecyclerPriceConfigDO::getWasteCode, reqVO.getWasteCode())
                .likeIfPresent(RecyclerPriceConfigDO::getWasteName, reqVO.getWasteName())
                .eqIfPresent(RecyclerPriceConfigDO::getRegionCode, reqVO.getRegionCode())
                .likeIfPresent(RecyclerPriceConfigDO::getRegionName, reqVO.getRegionName())
                .eqIfPresent(RecyclerPriceConfigDO::getStatus, reqVO.getStatus())
                .eqIfPresent(RecyclerPriceConfigDO::getIsNegotiable, reqVO.getNegotiable())
                .geIfPresent(RecyclerPriceConfigDO::getPurchasePrice, reqVO.getMinBasePrice())
                .leIfPresent(RecyclerPriceConfigDO::getPurchasePrice, reqVO.getMaxBasePrice())
                .betweenIfPresent(RecyclerPriceConfigDO::getEffectiveDate, reqVO.getEffectiveDate())
                .betweenIfPresent(RecyclerPriceConfigDO::getExpireDate, reqVO.getExpiryDate())
                .betweenIfPresent(RecyclerPriceConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(RecyclerPriceConfigDO::getEffectiveDate));
    }

    default List<RecyclerPriceConfigDO> selectByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return selectList(RecyclerPriceConfigDO::getRecyclingEnterpriseId, recyclingEnterpriseId);
    }

    default List<RecyclerPriceConfigDO> selectByWasteCode(String wasteCode) {
        return selectList(RecyclerPriceConfigDO::getWasteCode, wasteCode);
    }

    default List<RecyclerPriceConfigDO> selectByRegionCode(String regionCode) {
        return selectList(RecyclerPriceConfigDO::getRegionCode, regionCode);
    }

    default List<RecyclerPriceConfigDO> selectByStatus(Integer status) {
        return selectList(RecyclerPriceConfigDO::getStatus, status);
    }

    default List<RecyclerPriceConfigDO> selectEffectiveConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerPriceConfigDO>()
                .eq(RecyclerPriceConfigDO::getStatus, 1) // 生效中
                .le(RecyclerPriceConfigDO::getEffectiveDate, LocalDate.now()) // 已生效
                .and(wrapper -> wrapper
                        .isNull(RecyclerPriceConfigDO::getExpireDate) // 无失效日期
                        .or()
                        .gt(RecyclerPriceConfigDO::getExpireDate, LocalDate.now()) // 或未失效
                )
                .orderByDesc(RecyclerPriceConfigDO::getEffectiveDate));
    }

    default List<RecyclerPriceConfigDO> selectByEnterpriseIdAndWasteCode(Long recyclingEnterpriseId, String wasteCode) {
        return selectList(new LambdaQueryWrapperX<RecyclerPriceConfigDO>()
                .eq(RecyclerPriceConfigDO::getRecyclingEnterpriseId, recyclingEnterpriseId)
                .eq(RecyclerPriceConfigDO::getWasteCode, wasteCode)
                .orderByDesc(RecyclerPriceConfigDO::getEffectiveDate));
    }

    default RecyclerPriceConfigDO selectEffectiveConfigByEnterpriseAndWasteAndRegion(
            Long recyclingEnterpriseId, String wasteCode, String regionCode) {
        return selectOne(new LambdaQueryWrapperX<RecyclerPriceConfigDO>()
                .eq(RecyclerPriceConfigDO::getRecyclingEnterpriseId, recyclingEnterpriseId)
                .eq(RecyclerPriceConfigDO::getWasteCode, wasteCode)
                .eq(RecyclerPriceConfigDO::getStatus, 1) // 生效中
                .le(RecyclerPriceConfigDO::getEffectiveDate, LocalDate.now()) // 已生效
                .and(wrapper -> wrapper
                        .isNull(RecyclerPriceConfigDO::getExpireDate) // 无失效日期
                        .or()
                        .gt(RecyclerPriceConfigDO::getExpireDate, LocalDate.now()) // 或未失效
                )
                .and(wrapper -> wrapper
                        .eq(RecyclerPriceConfigDO::getRegionCode, regionCode) // 指定地区
                        .or()
                        .isNull(RecyclerPriceConfigDO::getRegionCode) // 或不限地区
                )
                .orderByDesc(RecyclerPriceConfigDO::getEffectiveDate)
                .orderByAsc(RecyclerPriceConfigDO::getRegionCode) // 优先地区价格
                .last("LIMIT 1"));
    }

    default List<RecyclerPriceConfigDO> selectNegotiableConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerPriceConfigDO>()
                .eq(RecyclerPriceConfigDO::getIsNegotiable, true)
                .eq(RecyclerPriceConfigDO::getStatus, 1) // 生效中
                .orderByDesc(RecyclerPriceConfigDO::getEffectiveDate));
    }

    default List<RecyclerPriceConfigDO> selectExpiredConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerPriceConfigDO>()
                .eq(RecyclerPriceConfigDO::getStatus, 1) // 生效中但已过期
                .isNotNull(RecyclerPriceConfigDO::getExpireDate)
                .le(RecyclerPriceConfigDO::getExpireDate, LocalDate.now())
                .orderByDesc(RecyclerPriceConfigDO::getExpireDate));
    }

    default List<RecyclerPriceConfigDO> selectByEnterpriseIdAndRegion(Long recyclingEnterpriseId, String regionCode) {
        return selectList(new LambdaQueryWrapperX<RecyclerPriceConfigDO>()
                .eq(RecyclerPriceConfigDO::getRecyclingEnterpriseId, recyclingEnterpriseId)
                .and(wrapper -> wrapper
                        .eq(RecyclerPriceConfigDO::getRegionCode, regionCode) // 指定地区
                        .or()
                        .isNull(RecyclerPriceConfigDO::getRegionCode) // 或不限地区
                )
                .orderByDesc(RecyclerPriceConfigDO::getEffectiveDate));
    }

    default List<RecyclerPriceConfigDO> selectByEffectiveDateRange(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<RecyclerPriceConfigDO>()
                .between(RecyclerPriceConfigDO::getEffectiveDate, startDate, endDate)
                .orderByDesc(RecyclerPriceConfigDO::getEffectiveDate));
    }

    // Service实现类需要的方法
    default List<RecyclerPriceConfigDO> selectListByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return selectByRecyclingEnterpriseId(recyclingEnterpriseId);
    }

    default List<RecyclerPriceConfigDO> selectListByWasteCode(String wasteCode) {
        return selectByWasteCode(wasteCode);
    }

    default List<RecyclerPriceConfigDO> selectListByRegionCode(String regionCode) {
        return selectByRegionCode(regionCode);
    }

    default List<RecyclerPriceConfigDO> selectListByStatus(Integer status) {
        return selectByStatus(status);
    }

    default RecyclerPriceConfigDO selectEffectiveByEnterpriseWasteAndRegion(Long recyclingEnterpriseId, String wasteCode, String regionCode) {
        return selectEffectiveConfigByEnterpriseAndWasteAndRegion(recyclingEnterpriseId, wasteCode, regionCode);
    }

    default RecyclerPriceConfigDO selectEffectiveByEnterpriseAndWasteWithRegionPriority(Long recyclingEnterpriseId, String wasteCode, String regionCode) {
        return selectEffectiveConfigByEnterpriseAndWasteAndRegion(recyclingEnterpriseId, wasteCode, regionCode);
    }

    default List<RecyclerPriceConfigDO> selectListByEffectiveDateRange(LocalDate startDate, LocalDate endDate) {
        return selectByEffectiveDateRange(startDate, endDate);
    }

    // ==================== 智能报价服务需要的方法 ====================

    /**
     * 根据回收企业、废物代码和地区查询价格配置
     * 用于智能报价服务的区域价格查询
     */
    default RecyclerPriceConfigDO selectByRecyclerAndWasteAndRegion(Long recyclingEnterpriseId, String wasteCode, String region) {
        return selectOne(new LambdaQueryWrapperX<RecyclerPriceConfigDO>()
                .eq(RecyclerPriceConfigDO::getRecyclingEnterpriseId, recyclingEnterpriseId)
                .eq(RecyclerPriceConfigDO::getWasteCode, wasteCode)
                .eq(RecyclerPriceConfigDO::getStatus, 1) // 生效中
                .le(RecyclerPriceConfigDO::getEffectiveDate, LocalDate.now()) // 已生效
                .and(wrapper -> wrapper
                        .isNull(RecyclerPriceConfigDO::getExpireDate) // 无失效日期
                        .or()
                        .gt(RecyclerPriceConfigDO::getExpireDate, LocalDate.now()) // 或未失效
                )
                .and(wrapper -> {
                    if (region != null) {
                        // 如果指定了地区，优先查找该地区的价格
                        wrapper.eq(RecyclerPriceConfigDO::getRegionCode, region)
                               .or()
                               .isNull(RecyclerPriceConfigDO::getRegionCode); // 或全国通用价格
                    } else {
                        // 如果没有指定地区，查找全国通用价格
                        wrapper.isNull(RecyclerPriceConfigDO::getRegionCode);
                    }
                })
                .orderByDesc(RecyclerPriceConfigDO::getEffectiveDate)
                .orderByAsc(RecyclerPriceConfigDO::getRegionCode) // 优先地区价格
                .last("LIMIT 1"));
    }

} 