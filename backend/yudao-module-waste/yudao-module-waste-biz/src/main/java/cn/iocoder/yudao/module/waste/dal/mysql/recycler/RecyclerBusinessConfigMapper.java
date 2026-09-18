package cn.iocoder.yudao.module.waste.dal.mysql.recycler;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerBusinessConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 回收企业业务模式配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface RecyclerBusinessConfigMapper extends BaseMapperX<RecyclerBusinessConfigDO> {

    default PageResult<RecyclerBusinessConfigDO> selectPage(RecyclerBusinessConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eqIfPresent(RecyclerBusinessConfigDO::getRecyclingEnterpriseId, reqVO.getRecyclingEnterpriseId())
                .eqIfPresent(RecyclerBusinessConfigDO::getBusinessMode, reqVO.getBusinessMode())
                .eqIfPresent(RecyclerBusinessConfigDO::getDefaultQuotationMode, reqVO.getQuotationMode())
                .eqIfPresent(RecyclerBusinessConfigDO::getAllowClientModeSelection, reqVO.getCustomerModeSelection() != null ? reqVO.getCustomerModeSelection() == 1 : null)
                .eqIfPresent(RecyclerBusinessConfigDO::getAutoAcceptSingleQuotation, reqVO.getAutoAcceptEnabled())
                .eqIfPresent(RecyclerBusinessConfigDO::getEnablePriceNegotiation, reqVO.getPriceNegotiationEnabled())
                .eqIfPresent(RecyclerBusinessConfigDO::getIsEnabled, reqVO.getStatus() != null ? reqVO.getStatus() == 1 : null)
                .betweenIfPresent(RecyclerBusinessConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default List<RecyclerBusinessConfigDO> selectList(RecyclerBusinessConfigPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eqIfPresent(RecyclerBusinessConfigDO::getRecyclingEnterpriseId, reqVO.getRecyclingEnterpriseId())
                .eqIfPresent(RecyclerBusinessConfigDO::getBusinessMode, reqVO.getBusinessMode())
                .eqIfPresent(RecyclerBusinessConfigDO::getDefaultQuotationMode, reqVO.getQuotationMode())
                .eqIfPresent(RecyclerBusinessConfigDO::getAllowClientModeSelection, reqVO.getCustomerModeSelection() != null ? reqVO.getCustomerModeSelection() == 1 : null)
                .eqIfPresent(RecyclerBusinessConfigDO::getAutoAcceptSingleQuotation, reqVO.getAutoAcceptEnabled())
                .eqIfPresent(RecyclerBusinessConfigDO::getEnablePriceNegotiation, reqVO.getPriceNegotiationEnabled())
                .eqIfPresent(RecyclerBusinessConfigDO::getIsEnabled, reqVO.getStatus() != null ? reqVO.getStatus() == 1 : null)
                .betweenIfPresent(RecyclerBusinessConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default RecyclerBusinessConfigDO selectByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return selectOne(RecyclerBusinessConfigDO::getRecyclingEnterpriseId, recyclingEnterpriseId);
    }

    default List<RecyclerBusinessConfigDO> selectByBusinessMode(Integer businessMode) {
        return selectList(RecyclerBusinessConfigDO::getBusinessMode, businessMode);
    }

    default List<RecyclerBusinessConfigDO> selectByDefaultQuotationMode(Integer defaultQuotationMode) {
        return selectList(RecyclerBusinessConfigDO::getDefaultQuotationMode, defaultQuotationMode);
    }

    default List<RecyclerBusinessConfigDO> selectEnabledConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eq(RecyclerBusinessConfigDO::getIsEnabled, true)
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default List<RecyclerBusinessConfigDO> selectDisabledConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eq(RecyclerBusinessConfigDO::getIsEnabled, false)
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default List<RecyclerBusinessConfigDO> selectCompetitiveBiddingConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eq(RecyclerBusinessConfigDO::getBusinessMode, 1) // 竞价模式
                .eq(RecyclerBusinessConfigDO::getIsEnabled, true)
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default List<RecyclerBusinessConfigDO> selectNegotiationConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eq(RecyclerBusinessConfigDO::getBusinessMode, 2) // 议价模式
                .eq(RecyclerBusinessConfigDO::getIsEnabled, true)
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default List<RecyclerBusinessConfigDO> selectFixedPriceConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eq(RecyclerBusinessConfigDO::getBusinessMode, 3) // 固定价格模式
                .eq(RecyclerBusinessConfigDO::getIsEnabled, true)
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default List<RecyclerBusinessConfigDO> selectAutoQuotationConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eq(RecyclerBusinessConfigDO::getDefaultQuotationMode, 1) // 自动报价
                .eq(RecyclerBusinessConfigDO::getIsEnabled, true)
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default List<RecyclerBusinessConfigDO> selectManualQuotationConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eq(RecyclerBusinessConfigDO::getDefaultQuotationMode, 2) // 手动报价
                .eq(RecyclerBusinessConfigDO::getIsEnabled, true)
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default List<RecyclerBusinessConfigDO> selectAllowClientModeSelectionConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eq(RecyclerBusinessConfigDO::getAllowClientModeSelection, true)
                .eq(RecyclerBusinessConfigDO::getIsEnabled, true)
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default List<RecyclerBusinessConfigDO> selectAutoAcceptSingleQuotationConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eq(RecyclerBusinessConfigDO::getAutoAcceptSingleQuotation, true)
                .eq(RecyclerBusinessConfigDO::getIsEnabled, true)
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default List<RecyclerBusinessConfigDO> selectPriceNegotiationEnabledConfigs() {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .eq(RecyclerBusinessConfigDO::getEnablePriceNegotiation, true)
                .eq(RecyclerBusinessConfigDO::getIsEnabled, true)
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

    default List<RecyclerBusinessConfigDO> selectByQuotationTimeoutRange(Integer minHours, Integer maxHours) {
        return selectList(new LambdaQueryWrapperX<RecyclerBusinessConfigDO>()
                .between(RecyclerBusinessConfigDO::getQuotationTimeoutHours, minHours, maxHours)
                .eq(RecyclerBusinessConfigDO::getIsEnabled, true)
                .orderByDesc(RecyclerBusinessConfigDO::getCreateTime));
    }

} 