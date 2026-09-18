package cn.iocoder.yudao.module.icbc.dal.mysql.callback;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifyPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工行回调通知 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface CallbackNotifyMapper extends BaseMapperX<CallbackNotifyDO> {

    default PageResult<CallbackNotifyDO> selectPage(CallbackNotifyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CallbackNotifyDO>()
                .eqIfPresent(CallbackNotifyDO::getNotifyId, reqVO.getNotifyId())
                .eqIfPresent(CallbackNotifyDO::getNotifyType, reqVO.getNotifyType())
                .likeIfPresent(CallbackNotifyDO::getBusinessId, reqVO.getBusinessId())
                .eqIfPresent(CallbackNotifyDO::getProcessStatus, reqVO.getProcessStatus())
                .betweenIfPresent(CallbackNotifyDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CallbackNotifyDO::getId));
    }

    default CallbackNotifyDO selectByNotifyId(String notifyId) {
        return selectOne(CallbackNotifyDO::getNotifyId, notifyId);
    }

    default List<CallbackNotifyDO> selectListByProcessStatus(Integer processStatus) {
        return selectList(CallbackNotifyDO::getProcessStatus, processStatus);
    }

} 