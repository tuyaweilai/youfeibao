package cn.iocoder.yudao.module.icbc.service.tenant.impl;

import cn.iocoder.yudao.module.icbc.enums.RecyclingRoleEnum;
import cn.iocoder.yudao.module.icbc.service.tenant.RecyclingTenantRoleService;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import org.springframework.stereotype.Service;

/**
 * 回收企业租户内置角色初始化 Service 实现。
 *
 * <p>角色按 {@link RecyclingRoleEnum} 的定义补齐：管理员复用 yudao 建租户时生成的
 * {@code tenant_admin}，收货员 / 开票员 / 财务按各自 code 建出来。平台运营是跨租户
 * 角色，不在租户内建。
 *
 * <p>幂等：每次都先按 code 查，存在就跳过，因此可被租户开通流程或管理员手动重复调用。
 */
@Service
public class RecyclingTenantRoleServiceImpl implements RecyclingTenantRoleService {

    private final RoleApi roleApi;

    public RecyclingTenantRoleServiceImpl(RoleApi roleApi) {
        this.roleApi = roleApi;
    }

    @Override
    public void initTenantRoles() {
        for (RecyclingRoleEnum role : RecyclingRoleEnum.values()) {
            if (role.isCrossTenant()) {
                continue; // 平台运营不在租户内初始化
            }
            if (roleApi.getRoleIdByCode(role.getCode()) != null) {
                continue; // 已存在，幂等
            }
            roleApi.createRole(role.getCode(), role.getName());
        }
    }

}
