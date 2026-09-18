package cn.iocoder.yudao.module.enterprise.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseDeptLinkDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 部门与企业关联 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface EnterpriseDeptLinkMapper extends BaseMapperX<EnterpriseDeptLinkDO> {

    /**
     * 根据部门ID和租户ID查询关联关系
     *
     * @param deptId 部门ID
     * @param tenantId 租户ID
     * @return 关联关系
     */
    default EnterpriseDeptLinkDO selectByDeptIdAndTenantId(@Param("deptId") Long deptId, @Param("tenantId") Long tenantId) {
        return selectOne(EnterpriseDeptLinkDO::getDeptId, deptId, 
                         EnterpriseDeptLinkDO::getTenantId, tenantId);
    }
} 