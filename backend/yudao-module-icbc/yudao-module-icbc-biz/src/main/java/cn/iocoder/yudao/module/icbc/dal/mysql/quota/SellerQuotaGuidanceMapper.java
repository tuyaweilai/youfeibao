package cn.iocoder.yudao.module.icbc.dal.mysql.quota;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaGuidancePageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.quota.SellerQuotaGuidanceDO;
import cn.iocoder.yudao.module.icbc.enums.SellerQuotaGuidanceStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 出售者额度超限引导记录 Mapper
 */
@Mapper
public interface SellerQuotaGuidanceMapper extends BaseMapperX<SellerQuotaGuidanceDO> {

    /**
     * 某出售者在当前租户下尚未办结的引导记录（有则复用，不重复新增）
     */
    default SellerQuotaGuidanceDO selectOpenByPayeeId(Long payeeId) {
        return selectList(new LambdaQueryWrapperX<SellerQuotaGuidanceDO>()
                .eq(SellerQuotaGuidanceDO::getPayeeId, payeeId)
                .ne(SellerQuotaGuidanceDO::getStatus, SellerQuotaGuidanceStatusEnum.RESOLVED.getStatus())
                .orderByAsc(SellerQuotaGuidanceDO::getId))
                .stream().findFirst().orElse(null);
    }

    /**
     * 待跟进（未办结）的引导记录，最近触发的排在前面
     */
    default PageResult<SellerQuotaGuidanceDO> selectPage(SellerQuotaGuidancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SellerQuotaGuidanceDO>()
                .eqIfPresent(SellerQuotaGuidanceDO::getPayeeId, reqVO.getPayeeId())
                .eqIfPresent(SellerQuotaGuidanceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SellerQuotaGuidanceDO::getTriggerScene, reqVO.getTriggerScene())
                .likeIfPresent(SellerQuotaGuidanceDO::getSellerName, reqVO.getSellerName())
                .orderByDesc(SellerQuotaGuidanceDO::getLastTriggeredAt)
                .orderByDesc(SellerQuotaGuidanceDO::getId));
    }

    // ==================== 工作台预警（#56 T18） ====================

    /** 工作台「额度」预警条数：本租户尚未办结的引导记录。 */
    default long selectCountOpen() {
        return selectCount(new LambdaQueryWrapperX<SellerQuotaGuidanceDO>()
                .ne(SellerQuotaGuidanceDO::getStatus, SellerQuotaGuidanceStatusEnum.RESOLVED.getStatus()));
    }

    /** 工作台「额度」预警明细：超限最久的排在前面，最多 {@code limit} 条。 */
    default List<SellerQuotaGuidanceDO> selectListOpen(int limit) {
        return selectList(new LambdaQueryWrapperX<SellerQuotaGuidanceDO>()
                .ne(SellerQuotaGuidanceDO::getStatus, SellerQuotaGuidanceStatusEnum.RESOLVED.getStatus())
                .orderByAsc(SellerQuotaGuidanceDO::getTriggeredAt)
                .orderByAsc(SellerQuotaGuidanceDO::getId)
                .last("LIMIT " + limit));
    }

}
