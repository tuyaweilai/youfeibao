package cn.iocoder.yudao.module.icbc.service.tenant;

/**
 * 回收企业租户内置角色初始化 Service 接口。
 *
 * <p>回收企业租户开出来后，管理员需要能给员工授予「收货员 / 开票员 / 财务」。
 * 管理员角色由 yudao 建租户时自动生成（{@code tenant_admin}），另外三个需要在
 * 租户内建出来。本服务把这四个角色的 {@code system_role} 记录补齐，且可重复调用。
 */
public interface RecyclingTenantRoleService {

    /**
     * 为当前租户补齐反向开票内置角色（已存在的跳过）。
     */
    void initTenantRoles();

}
