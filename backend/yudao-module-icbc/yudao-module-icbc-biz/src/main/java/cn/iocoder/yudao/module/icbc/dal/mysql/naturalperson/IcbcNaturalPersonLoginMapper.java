package cn.iocoder.yudao.module.icbc.dal.mysql.naturalperson;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonLoginDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 自然人主体与登录凭证的绑定 Mapper（平台级，非租户表）。
 */
@Mapper
public interface IcbcNaturalPersonLoginMapper extends BaseMapperX<IcbcNaturalPersonLoginDO> {

    default IcbcNaturalPersonLoginDO selectByNaturalPersonAndMember(Long naturalPersonId, Long memberUserId) {
        return selectOne(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<IcbcNaturalPersonLoginDO>()
                .eq(IcbcNaturalPersonLoginDO::getNaturalPersonId, naturalPersonId)
                .eq(IcbcNaturalPersonLoginDO::getMemberUserId, memberUserId));
    }

    default List<IcbcNaturalPersonLoginDO> selectListByMemberUserId(Long memberUserId) {
        return selectList(IcbcNaturalPersonLoginDO::getMemberUserId, memberUserId);
    }

    default List<IcbcNaturalPersonLoginDO> selectListByNaturalPersonId(Long naturalPersonId) {
        return selectList(IcbcNaturalPersonLoginDO::getNaturalPersonId, naturalPersonId);
    }

    default int deleteByNaturalPersonAndMember(Long naturalPersonId, Long memberUserId) {
        return delete(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<IcbcNaturalPersonLoginDO>()
                .eq(IcbcNaturalPersonLoginDO::getNaturalPersonId, naturalPersonId)
                .eq(IcbcNaturalPersonLoginDO::getMemberUserId, memberUserId));
    }

}
