package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * 角色 API 实现类
 *
 * @author 芋道源码
 */
@Service
public class RoleApiImpl implements RoleApi {

    @Resource
    private RoleService roleService;

    @Override
    public void validRoleList(Collection<Long> ids) {
        roleService.validateRoleList(ids);
    }

    @Override
    public Long getRoleIdByCode(String code) {
        RoleDO role = roleService.getRoleByCode(code);
        return role != null ? role.getId() : null;
    }

    @Override
    public Long createRole(String code, String name) {
        RoleSaveReqVO reqVO = new RoleSaveReqVO();
        reqVO.setCode(code);
        reqVO.setName(name);
        reqVO.setSort(0);
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setRemark("系统内置角色");
        return roleService.createRole(reqVO, RoleTypeEnum.CUSTOM.getType());
    }

}
