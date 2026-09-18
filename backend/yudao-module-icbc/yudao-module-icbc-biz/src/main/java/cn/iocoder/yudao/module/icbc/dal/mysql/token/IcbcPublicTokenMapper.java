package cn.iocoder.yudao.module.icbc.dal.mysql.token;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.token.IcbcPublicTokenDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

/**
 * 公开令牌 Mapper。表已登记进 {@code ignore-tables}，故不受多租户拦截。
 */
@Mapper
public interface IcbcPublicTokenMapper extends BaseMapperX<IcbcPublicTokenDO> {

    default IcbcPublicTokenDO selectByJti(String jti) {
        return selectOne(IcbcPublicTokenDO::getJti, jti);
    }

    /**
     * 原子占用一次使用次数：仅当 {@code used_count < max_uses} 时自增。
     *
     * @param id 令牌记录 ID
     * @return 是否占用成功
     */
    default boolean consume(Long id) {
        return update(null, new LambdaUpdateWrapper<IcbcPublicTokenDO>()
                .setSql("used_count = used_count + 1")
                .set(IcbcPublicTokenDO::getLastUsedTime, LocalDateTime.now())
                .eq(IcbcPublicTokenDO::getId, id)
                .apply("used_count < max_uses")) > 0;
    }

}
