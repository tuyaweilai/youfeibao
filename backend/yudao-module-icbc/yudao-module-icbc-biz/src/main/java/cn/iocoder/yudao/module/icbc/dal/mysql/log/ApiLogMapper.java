package cn.iocoder.yudao.module.icbc.dal.mysql.log;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.log.vo.ApiLogPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.log.ApiLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工行接口调用日志 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ApiLogMapper extends BaseMapperX<ApiLogDO> {

    default PageResult<ApiLogDO> selectPage(ApiLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ApiLogDO>()
                .eqIfPresent(ApiLogDO::getMsgId, reqVO.getMsgId())
                .likeIfPresent(ApiLogDO::getApiName, reqVO.getApiName())
                .eqIfPresent(ApiLogDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ApiLogDO::getBusinessId, reqVO.getBusinessId())
                .eqIfPresent(ApiLogDO::getBusinessType, reqVO.getBusinessType())
                .betweenIfPresent(ApiLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ApiLogDO::getId));
    }

    default ApiLogDO selectByMsgId(String msgId) {
        return selectOne(ApiLogDO::getMsgId, msgId);
    }

} 