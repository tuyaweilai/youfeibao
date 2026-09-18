package cn.iocoder.yudao.module.icbc.dal.mysql.authorization;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.authorization.IcbcSellerAuthorizationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 出售者首次授权 Mapper
 */
@Mapper
public interface IcbcSellerAuthorizationMapper extends BaseMapperX<IcbcSellerAuthorizationDO> {

    default IcbcSellerAuthorizationDO selectLatestByPayeeId(Long payeeId) {
        return selectOne(new LambdaQueryWrapperX<IcbcSellerAuthorizationDO>()
                .eq(IcbcSellerAuthorizationDO::getPayeeId, payeeId)
                .orderByDesc(IcbcSellerAuthorizationDO::getId)
                .last("LIMIT 1"));
    }

    default List<IcbcSellerAuthorizationDO> selectListByPayeeId(Long payeeId) {
        return selectList(new LambdaQueryWrapperX<IcbcSellerAuthorizationDO>()
                .eq(IcbcSellerAuthorizationDO::getPayeeId, payeeId)
                .orderByDesc(IcbcSellerAuthorizationDO::getId));
    }

}
