package cn.iocoder.yudao.module.icbc.dal.mysql.naturalperson;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 自然人主体 Mapper（平台级，非租户表）。
 */
@Mapper
public interface IcbcNaturalPersonMapper extends BaseMapperX<IcbcNaturalPersonDO> {

    default IcbcNaturalPersonDO selectByIdCardNo(String idCardNo) {
        return selectOne(IcbcNaturalPersonDO::getIdCardNo, idCardNo);
    }

    default IcbcNaturalPersonDO selectByOutUserId(String outUserId) {
        return selectOne(IcbcNaturalPersonDO::getOutUserId, outUserId);
    }

    default List<IcbcNaturalPersonDO> selectListByIds(Collection<Long> ids) {
        return selectBatchIds(ids);
    }

    default PageResult<IcbcNaturalPersonDO> selectPage(NaturalPersonPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcNaturalPersonDO>()
                .likeIfPresent(IcbcNaturalPersonDO::getName, reqVO.getName())
                .eqIfPresent(IcbcNaturalPersonDO::getIdCardNo, reqVO.getIdCardNo())
                .eqIfPresent(IcbcNaturalPersonDO::getMobile, reqVO.getMobile())
                .eqIfPresent(IcbcNaturalPersonDO::getOutUserId, reqVO.getOutUserId())
                .eqIfPresent(IcbcNaturalPersonDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(IcbcNaturalPersonDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(IcbcNaturalPersonDO::getId));
    }

}
