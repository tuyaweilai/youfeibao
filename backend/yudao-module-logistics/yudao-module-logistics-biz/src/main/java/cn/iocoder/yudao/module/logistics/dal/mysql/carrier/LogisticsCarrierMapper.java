package cn.iocoder.yudao.module.logistics.dal.mysql.carrier;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierPageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 承运商档案 Mapper。分页与导出共用同一条件（见 {@link #buildQuery}）。
 */
@Mapper
public interface LogisticsCarrierMapper extends BaseMapperX<LogisticsCarrierDO> {

    /**
     * 按名称查询（租户内唯一；租户由框架自动加条件）。
     */
    default LogisticsCarrierDO selectByName(String name) {
        return selectOne(LogisticsCarrierDO::getName, name);
    }

    default PageResult<LogisticsCarrierDO> selectPage(LogisticsCarrierPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<LogisticsCarrierDO> selectList(LogisticsCarrierPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<LogisticsCarrierDO> buildQuery(LogisticsCarrierPageReqVO reqVO) {
        return new LambdaQueryWrapperX<LogisticsCarrierDO>()
                .likeIfPresent(LogisticsCarrierDO::getName, reqVO.getName())
                .likeIfPresent(LogisticsCarrierDO::getContactName, reqVO.getContactName())
                .likeIfPresent(LogisticsCarrierDO::getContactMobile, reqVO.getContactMobile())
                .eqIfPresent(LogisticsCarrierDO::getStatus, reqVO.getStatus())
                .orderByDesc(LogisticsCarrierDO::getId);
    }

}
