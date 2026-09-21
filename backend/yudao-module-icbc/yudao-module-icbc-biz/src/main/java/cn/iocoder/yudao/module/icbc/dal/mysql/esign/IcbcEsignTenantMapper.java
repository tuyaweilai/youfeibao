package cn.iocoder.yudao.module.icbc.dal.mysql.esign;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.esign.IcbcEsignTenantDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 租户级电子签章配置 Mapper（#92）。租户表：每个租户最多一条。
 */
@Mapper
public interface IcbcEsignTenantMapper extends BaseMapperX<IcbcEsignTenantDO> {

    /**
     * 本租户的配置（租户拦截器保证只读到本租户的那条），没有就返回 {@code null}。
     */
    default IcbcEsignTenantDO selectCurrent() {
        return selectOne(new LambdaQueryWrapperX<IcbcEsignTenantDO>()
                .orderByAsc(IcbcEsignTenantDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 按子客编号反查（跨租户，调用方须在 {@code TenantUtils.executeIgnore} 下执行）。
     */
    default IcbcEsignTenantDO selectBySubCustomerNo(String subCustomerNo) {
        return selectOne(new LambdaQueryWrapperX<IcbcEsignTenantDO>()
                .eq(IcbcEsignTenantDO::getSubCustomerNo, subCustomerNo));
    }

    /**
     * 全平台配置（跨租户，调用方须在 {@code TenantUtils.executeIgnore} 下执行）。
     */
    default List<IcbcEsignTenantDO> selectAll() {
        return selectList(new LambdaQueryWrapperX<IcbcEsignTenantDO>()
                .orderByAsc(IcbcEsignTenantDO::getTenantId));
    }

}
