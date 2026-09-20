package cn.iocoder.yudao.module.logistics.dal.mysql.driver;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverPageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 司机档案 Mapper。
 */
@Mapper
public interface LogisticsDriverMapper extends BaseMapperX<LogisticsDriverDO> {

    /**
     * 按租户内系统用户查询（租户内唯一；租户由框架自动加条件）。
     */
    default LogisticsDriverDO selectByUserId(Long userId) {
        return selectOne(LogisticsDriverDO::getUserId, userId);
    }

    default PageResult<LogisticsDriverDO> selectPage(LogisticsDriverPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LogisticsDriverDO>()
                .likeIfPresent(LogisticsDriverDO::getName, reqVO.getName())
                .likeIfPresent(LogisticsDriverDO::getMobile, reqVO.getMobile())
                .eqIfPresent(LogisticsDriverDO::getSource, reqVO.getSource())
                .eqIfPresent(LogisticsDriverDO::getStatus, reqVO.getStatus())
                .orderByDesc(LogisticsDriverDO::getId));
    }

}
