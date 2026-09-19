package cn.iocoder.yudao.module.icbc.dal.mysql.notify;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.notify.vo.NotifyPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.notify.IcbcSellerNotifyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 出售者触达记录 Mapper（#36）。
 */
@Mapper
public interface IcbcSellerNotifyMapper extends BaseMapperX<IcbcSellerNotifyDO> {

    /**
     * 按幂等键查记录：查得到就说明这条触达已经处理过，不再发。
     */
    default IcbcSellerNotifyDO selectByBizKey(String bizType, String bizKey) {
        return selectOne(new LambdaQueryWrapperX<IcbcSellerNotifyDO>()
                .eq(IcbcSellerNotifyDO::getBizType, bizType)
                .eq(IcbcSellerNotifyDO::getBizKey, bizKey));
    }

    /**
     * 某个自然人主体在本租户的触达记录（倒序）：收货员转达 / 排查用。
     */
    default List<IcbcSellerNotifyDO> selectListByNaturalPersonId(Long naturalPersonId) {
        if (naturalPersonId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<IcbcSellerNotifyDO>()
                .eq(IcbcSellerNotifyDO::getNaturalPersonId, naturalPersonId)
                .orderByDesc(IcbcSellerNotifyDO::getId));
    }

    default PageResult<IcbcSellerNotifyDO> selectPage(NotifyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcSellerNotifyDO>()
                .eqIfPresent(IcbcSellerNotifyDO::getBizType, reqVO.getBizType())
                .eqIfPresent(IcbcSellerNotifyDO::getStatus, reqVO.getStatus())
                .likeIfPresent(IcbcSellerNotifyDO::getSellerName, reqVO.getSellerName())
                .orderByDesc(IcbcSellerNotifyDO::getId));
    }

}
