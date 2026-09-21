package cn.iocoder.yudao.module.icbc.dal.mysql.esign;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.esign.IcbcEsignConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电子签章平台级参数 Mapper（#92）。
 *
 * <p>平台级唯一一份：表是全局表，只该有一行；查询一律取第一条，保存一律原地更新。
 */
@Mapper
public interface IcbcEsignConfigMapper extends BaseMapperX<IcbcEsignConfigDO> {

    /**
     * 平台参数（唯一一份）；没配置过时返回 {@code null}。
     */
    default IcbcEsignConfigDO selectConfig() {
        return selectOne(new LambdaQueryWrapperX<IcbcEsignConfigDO>()
                .orderByAsc(IcbcEsignConfigDO::getId)
                .last("LIMIT 1"));
    }

}
