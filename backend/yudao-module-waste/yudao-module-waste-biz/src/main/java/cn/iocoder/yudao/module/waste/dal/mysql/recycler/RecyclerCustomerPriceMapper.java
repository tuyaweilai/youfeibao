package cn.iocoder.yudao.module.waste.dal.mysql.recycler;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerCustomerPricePageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerCustomerPriceDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * 回收企业客户专属价格配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface RecyclerCustomerPriceMapper extends BaseMapperX<RecyclerCustomerPriceDO> {

    default PageResult<RecyclerCustomerPriceDO> selectPage(RecyclerCustomerPricePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .eqIfPresent(RecyclerCustomerPriceDO::getRecyclingEnterpriseId, reqVO.getRecyclingEnterpriseId())
                .eqIfPresent(RecyclerCustomerPriceDO::getCustomerEnterpriseId, reqVO.getCustomerEnterpriseId())
                .likeIfPresent(RecyclerCustomerPriceDO::getWasteCode, reqVO.getWasteCode())
                .likeIfPresent(RecyclerCustomerPriceDO::getWasteName, reqVO.getWasteName())
                .eqIfPresent(RecyclerCustomerPriceDO::getPriceType, reqVO.getPriceType())
                .eqIfPresent(RecyclerCustomerPriceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(RecyclerCustomerPriceDO::getContractId, reqVO.getContractId())
                .betweenIfPresent(RecyclerCustomerPriceDO::getEffectiveDate, reqVO.getEffectiveDate())
                .betweenIfPresent(RecyclerCustomerPriceDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(RecyclerCustomerPriceDO::getEffectiveDate));
    }

    default List<RecyclerCustomerPriceDO> selectList(RecyclerCustomerPricePageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .eqIfPresent(RecyclerCustomerPriceDO::getRecyclingEnterpriseId, reqVO.getRecyclingEnterpriseId())
                .eqIfPresent(RecyclerCustomerPriceDO::getCustomerEnterpriseId, reqVO.getCustomerEnterpriseId())
                .likeIfPresent(RecyclerCustomerPriceDO::getWasteCode, reqVO.getWasteCode())
                .likeIfPresent(RecyclerCustomerPriceDO::getWasteName, reqVO.getWasteName())
                .eqIfPresent(RecyclerCustomerPriceDO::getPriceType, reqVO.getPriceType())
                .eqIfPresent(RecyclerCustomerPriceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(RecyclerCustomerPriceDO::getContractId, reqVO.getContractId())
                .betweenIfPresent(RecyclerCustomerPriceDO::getEffectiveDate, reqVO.getEffectiveDate())
                .betweenIfPresent(RecyclerCustomerPriceDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(RecyclerCustomerPriceDO::getEffectiveDate));
    }

    default List<RecyclerCustomerPriceDO> selectByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return selectList(RecyclerCustomerPriceDO::getRecyclingEnterpriseId, recyclingEnterpriseId);
    }

    default List<RecyclerCustomerPriceDO> selectByCustomerEnterpriseId(Long customerEnterpriseId) {
        return selectList(RecyclerCustomerPriceDO::getCustomerEnterpriseId, customerEnterpriseId);
    }

    default List<RecyclerCustomerPriceDO> selectByWasteCode(String wasteCode) {
        return selectList(RecyclerCustomerPriceDO::getWasteCode, wasteCode);
    }

    default List<RecyclerCustomerPriceDO> selectByPriceType(Integer priceType) {
        return selectList(RecyclerCustomerPriceDO::getPriceType, priceType);
    }

    default List<RecyclerCustomerPriceDO> selectByStatus(Integer status) {
        return selectList(RecyclerCustomerPriceDO::getStatus, status);
    }

    default List<RecyclerCustomerPriceDO> selectEffectivePrices() {
        return selectList(new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .eq(RecyclerCustomerPriceDO::getStatus, 1) // 生效中
                .le(RecyclerCustomerPriceDO::getEffectiveDate, LocalDate.now()) // 已生效
                .and(wrapper -> wrapper
                        .isNull(RecyclerCustomerPriceDO::getExpireDate) // 无失效日期
                        .or()
                        .gt(RecyclerCustomerPriceDO::getExpireDate, LocalDate.now()) // 或未失效
                )
                .orderByDesc(RecyclerCustomerPriceDO::getEffectiveDate));
    }

    default RecyclerCustomerPriceDO selectEffectivePriceByCustomerAndWaste(
            Long recyclingEnterpriseId, Long customerEnterpriseId, String wasteCode) {
        return selectOne(new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .eq(RecyclerCustomerPriceDO::getRecyclingEnterpriseId, recyclingEnterpriseId)
                .eq(RecyclerCustomerPriceDO::getCustomerEnterpriseId, customerEnterpriseId)
                .eq(RecyclerCustomerPriceDO::getWasteCode, wasteCode)
                .eq(RecyclerCustomerPriceDO::getStatus, 1) // 生效中
                .le(RecyclerCustomerPriceDO::getEffectiveDate, LocalDate.now()) // 已生效
                .and(wrapper -> wrapper
                        .isNull(RecyclerCustomerPriceDO::getExpireDate) // 无失效日期
                        .or()
                        .gt(RecyclerCustomerPriceDO::getExpireDate, LocalDate.now()) // 或未失效
                )
                .orderByDesc(RecyclerCustomerPriceDO::getEffectiveDate)
                .last("LIMIT 1"));
    }

    default List<RecyclerCustomerPriceDO> selectByCustomerAndRecycler(Long customerEnterpriseId, Long recyclingEnterpriseId) {
        return selectList(new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .eq(RecyclerCustomerPriceDO::getCustomerEnterpriseId, customerEnterpriseId)
                .eq(RecyclerCustomerPriceDO::getRecyclingEnterpriseId, recyclingEnterpriseId)
                .orderByDesc(RecyclerCustomerPriceDO::getEffectiveDate));
    }

    default List<RecyclerCustomerPriceDO> selectByContractId(Long contractId) {
        return selectList(RecyclerCustomerPriceDO::getContractId, contractId);
    }

    default List<RecyclerCustomerPriceDO> selectExpiredPrices() {
        return selectList(new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .eq(RecyclerCustomerPriceDO::getStatus, 1) // 生效中但已过期
                .isNotNull(RecyclerCustomerPriceDO::getExpireDate)
                .le(RecyclerCustomerPriceDO::getExpireDate, LocalDate.now())
                .orderByDesc(RecyclerCustomerPriceDO::getExpireDate));
    }

    default List<RecyclerCustomerPriceDO> selectByEffectiveDateRange(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .between(RecyclerCustomerPriceDO::getEffectiveDate, startDate, endDate)
                .orderByDesc(RecyclerCustomerPriceDO::getEffectiveDate));
    }

    default List<RecyclerCustomerPriceDO> selectFixedPrices() {
        return selectList(new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .eq(RecyclerCustomerPriceDO::getPriceType, 1) // 固定价格
                .eq(RecyclerCustomerPriceDO::getStatus, 1) // 生效中
                .orderByDesc(RecyclerCustomerPriceDO::getEffectiveDate));
    }

    default List<RecyclerCustomerPriceDO> selectFloatingPrices() {
        return selectList(new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .eq(RecyclerCustomerPriceDO::getPriceType, 2) // 浮动价格
                .eq(RecyclerCustomerPriceDO::getStatus, 1) // 生效中
                .orderByDesc(RecyclerCustomerPriceDO::getEffectiveDate));
    }

    default List<RecyclerCustomerPriceDO> selectTieredPrices() {
        return selectList(new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .eq(RecyclerCustomerPriceDO::getPriceType, 3) // 阶梯价格
                .eq(RecyclerCustomerPriceDO::getStatus, 1) // 生效中
                .orderByDesc(RecyclerCustomerPriceDO::getEffectiveDate));
    }

    // ==================== Service实现类调用的方法 ====================

    default List<RecyclerCustomerPriceDO> selectListByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return selectList(RecyclerCustomerPriceDO::getRecyclingEnterpriseId, recyclingEnterpriseId);
    }

    default List<RecyclerCustomerPriceDO> selectListByCustomerEnterpriseId(Long customerEnterpriseId) {
        return selectList(RecyclerCustomerPriceDO::getCustomerEnterpriseId, customerEnterpriseId);
    }

    default List<RecyclerCustomerPriceDO> selectListByWasteCode(String wasteCode) {
        return selectList(RecyclerCustomerPriceDO::getWasteCode, wasteCode);
    }

    default List<RecyclerCustomerPriceDO> selectListByPriceType(Integer priceType) {
        return selectList(RecyclerCustomerPriceDO::getPriceType, priceType);
    }

    default List<RecyclerCustomerPriceDO> selectListByCustomerAndRecycler(Long customerEnterpriseId, Long recyclingEnterpriseId) {
        return selectList(new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .eq(RecyclerCustomerPriceDO::getCustomerEnterpriseId, customerEnterpriseId)
                .eq(RecyclerCustomerPriceDO::getRecyclingEnterpriseId, recyclingEnterpriseId)
                .orderByDesc(RecyclerCustomerPriceDO::getEffectiveDate));
    }

    default List<RecyclerCustomerPriceDO> selectListByContractId(Long contractId) {
        return selectList(RecyclerCustomerPriceDO::getContractId, contractId);
    }

    default List<RecyclerCustomerPriceDO> selectListByEffectiveDateRange(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<RecyclerCustomerPriceDO>()
                .between(RecyclerCustomerPriceDO::getEffectiveDate, startDate, endDate)
                .orderByDesc(RecyclerCustomerPriceDO::getEffectiveDate));
    }

    default List<RecyclerCustomerPriceDO> selectFixedPriceConfigs() {
        return selectFixedPrices();
    }

    default List<RecyclerCustomerPriceDO> selectFloatingPriceConfigs() {
        return selectFloatingPrices();
    }

    default List<RecyclerCustomerPriceDO> selectTieredPriceConfigs() {
        return selectTieredPrices();
    }

} 