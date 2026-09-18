package cn.iocoder.yudao.module.icbc.dal.mysql.warning;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.warning.IcbcExpiryWarningDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 资质到期预警 Mapper
 */
@Mapper
public interface IcbcExpiryWarningMapper extends BaseMapperX<IcbcExpiryWarningDO> {

    default List<IcbcExpiryWarningDO> selectOpenList() {
        return selectList(new LambdaQueryWrapperX<IcbcExpiryWarningDO>()
                .eq(IcbcExpiryWarningDO::getStatus, 0)
                .orderByAsc(IcbcExpiryWarningDO::getValidTo));
    }

    default IcbcExpiryWarningDO selectOpenByQualificationId(Long qualificationId) {
        return selectList(new LambdaQueryWrapperX<IcbcExpiryWarningDO>()
                .eq(IcbcExpiryWarningDO::getQualificationId, qualificationId)
                .eq(IcbcExpiryWarningDO::getStatus, 0)
                .orderByAsc(IcbcExpiryWarningDO::getId))
                .stream().findFirst().orElse(null);
    }

}
