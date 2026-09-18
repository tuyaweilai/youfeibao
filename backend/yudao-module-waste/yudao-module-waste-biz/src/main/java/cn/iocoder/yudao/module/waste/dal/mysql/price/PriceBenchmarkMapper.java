package cn.iocoder.yudao.module.waste.dal.mysql.price;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.PriceBenchmarkDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * 价格基准 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface PriceBenchmarkMapper extends BaseMapperX<PriceBenchmarkDO> {

    default PageResult<PriceBenchmarkDO> selectPage(PriceBenchmarkPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PriceBenchmarkDO>()
                .likeIfPresent(PriceBenchmarkDO::getWasteCode, reqVO.getWasteCode())
                .likeIfPresent(PriceBenchmarkDO::getWasteName, reqVO.getWasteName())
                .eqIfPresent(PriceBenchmarkDO::getRegionCode, reqVO.getRegionCode())
                .likeIfPresent(PriceBenchmarkDO::getRegionName, reqVO.getRegionName())
                .eqIfPresent(PriceBenchmarkDO::getStatus, reqVO.getStatus())
                .likeIfPresent(PriceBenchmarkDO::getPriceSource, reqVO.getSource())
                .betweenIfPresent(PriceBenchmarkDO::getEffectiveDate, reqVO.getEffectiveDate())
                .betweenIfPresent(PriceBenchmarkDO::getExpireDate, reqVO.getExpiryDate())
                .betweenIfPresent(PriceBenchmarkDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PriceBenchmarkDO::getEffectiveDate));
    }

    default List<PriceBenchmarkDO> selectByWasteCode(String wasteCode) {
        return selectList(PriceBenchmarkDO::getWasteCode, wasteCode);
    }

    default PriceBenchmarkDO selectSingleByWasteCode(String wasteCode) {
        return selectOne(new LambdaQueryWrapperX<PriceBenchmarkDO>()
                .eq(PriceBenchmarkDO::getWasteCode, wasteCode)
                .eq(PriceBenchmarkDO::getStatus, 1)
                .orderByDesc(PriceBenchmarkDO::getEffectiveDate)
                .last("LIMIT 1"));
    }

    default List<PriceBenchmarkDO> selectPopularWastePriceRanking(String regionCode, Integer limit) {
        LambdaQueryWrapperX<PriceBenchmarkDO> wrapper = new LambdaQueryWrapperX<PriceBenchmarkDO>()
                .eq(PriceBenchmarkDO::getStatus, 1)
                .orderByDesc(PriceBenchmarkDO::getPrice);
        
        if (regionCode != null) {
            wrapper.eq(PriceBenchmarkDO::getRegionCode, regionCode);
        }
        
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        
        return selectList(wrapper);
    }

    default List<PriceBenchmarkDO> selectListByWasteCode(String wasteCode) {
        return selectList(PriceBenchmarkDO::getWasteCode, wasteCode);
    }

    default List<PriceBenchmarkDO> selectListByRegionCode(String regionCode) {
        return selectList(PriceBenchmarkDO::getRegionCode, regionCode);
    }

    default List<PriceBenchmarkDO> selectListByStatus(Integer status) {
        return selectList(PriceBenchmarkDO::getStatus, status);
    }

    default List<PriceBenchmarkDO> selectEffectiveBenchmarks() {
        return selectList(new LambdaQueryWrapperX<PriceBenchmarkDO>()
                .eq(PriceBenchmarkDO::getStatus, 1)
                .ge(PriceBenchmarkDO::getExpireDate, LocalDate.now())
                .orderByDesc(PriceBenchmarkDO::getEffectiveDate));
    }

    default PriceBenchmarkDO selectEffectiveByWasteCodeAndRegion(String wasteCode, String regionCode) {
        return selectOne(new LambdaQueryWrapperX<PriceBenchmarkDO>()
                .eq(PriceBenchmarkDO::getWasteCode, wasteCode)
                .eq(PriceBenchmarkDO::getRegionCode, regionCode)
                .eq(PriceBenchmarkDO::getStatus, 1)
                .ge(PriceBenchmarkDO::getExpireDate, LocalDate.now())
                .orderByDesc(PriceBenchmarkDO::getEffectiveDate)
                .last("LIMIT 1"));
    }

    default PriceBenchmarkDO selectEffectiveByWasteCodeWithRegionPriority(String wasteCode, String regionCode) {
        return selectOne(new LambdaQueryWrapperX<PriceBenchmarkDO>()
                .eq(PriceBenchmarkDO::getWasteCode, wasteCode)
                .eq(PriceBenchmarkDO::getStatus, 1)
                .ge(PriceBenchmarkDO::getExpireDate, LocalDate.now())
                .last("ORDER BY CASE WHEN region_code =  + regionCode +  THEN 0 ELSE 1 END, effective_date DESC LIMIT 1"));
    }

    default List<PriceBenchmarkDO> selectExpiredBenchmarks() {
        return selectList(new LambdaQueryWrapperX<PriceBenchmarkDO>()
                .lt(PriceBenchmarkDO::getExpireDate, LocalDate.now())
                .orderByDesc(PriceBenchmarkDO::getExpireDate));
    }

    default List<PriceBenchmarkDO> selectListByEffectiveDateRange(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<PriceBenchmarkDO>()
                .between(PriceBenchmarkDO::getEffectiveDate, startDate, endDate)
                .orderByDesc(PriceBenchmarkDO::getEffectiveDate));
    }

    default List<PriceBenchmarkDO> selectListBySource(String source) {
        return selectList(PriceBenchmarkDO::getPriceSource, source);
    }

    default List<PriceBenchmarkDO> selectPriceTrendByWasteCode(String wasteCode, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<PriceBenchmarkDO>()
                .eq(PriceBenchmarkDO::getWasteCode, wasteCode)
                .between(PriceBenchmarkDO::getEffectiveDate, startDate, endDate)
                .orderByAsc(PriceBenchmarkDO::getEffectiveDate));
    }

    default List<PriceBenchmarkDO> selectPriceTrendByRegion(String regionCode, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<PriceBenchmarkDO>()
                .eq(PriceBenchmarkDO::getRegionCode, regionCode)
                .between(PriceBenchmarkDO::getEffectiveDate, startDate, endDate)
                .orderByAsc(PriceBenchmarkDO::getEffectiveDate));
    }

}
