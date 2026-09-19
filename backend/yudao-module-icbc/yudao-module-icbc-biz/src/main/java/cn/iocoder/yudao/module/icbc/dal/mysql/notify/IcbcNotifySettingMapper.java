package cn.iocoder.yudao.module.icbc.dal.mysql.notify;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.notify.IcbcNotifySettingDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户级触达设置 Mapper（#36）。租户表：每个租户最多一条。
 */
@Mapper
public interface IcbcNotifySettingMapper extends BaseMapperX<IcbcNotifySettingDO> {

    /**
     * 当前租户的设置（租户拦截器保证只读到本租户的那条），没有就返回 null。
     */
    default IcbcNotifySettingDO selectCurrent() {
        return selectOne(new LambdaQueryWrapperX<IcbcNotifySettingDO>()
                .orderByAsc(IcbcNotifySettingDO::getId)
                .last("LIMIT 1"));
    }

}
