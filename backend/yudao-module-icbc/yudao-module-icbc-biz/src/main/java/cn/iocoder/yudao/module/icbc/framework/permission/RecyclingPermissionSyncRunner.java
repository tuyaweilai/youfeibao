package cn.iocoder.yudao.module.icbc.framework.permission;

import cn.iocoder.yudao.module.icbc.service.tenant.RecyclingPermissionSyncService;
import cn.iocoder.yudao.module.icbc.service.tenant.dto.RecyclingPermissionSyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * 启动时同步回收域的权限行与租户套餐。
 *
 * <p>只做全局部分（{@code system_menu} + 回收企业套餐），不碰租户内的角色授权——那部分依赖租户上下文，
 * 由 {@code POST /icbc/tenant/role/init} 在租户内同步。失败不阻断启动：菜单/套餐不存在时（例如本地库还没导入
 * {@code icbc-menu.sql}）打一条 warn 即可，后续可手工重试。
 *
 * <p>单测里排除：{@code UnitTestConfiguration} 会组件扫描整个模块，而测试上下文没有
 * {@code MenuApi} / {@code RoleApi} 等 system API 的实现。
 */
@Component
@Profile("!unit-test")
@Slf4j
public class RecyclingPermissionSyncRunner implements ApplicationRunner {

    @Resource
    private RecyclingPermissionSyncService recyclingPermissionSyncService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            RecyclingPermissionSyncResult result = recyclingPermissionSyncService.syncGlobal();
            if (!result.isEmpty()) {
                log.info("[run][回收域权限同步完成：新增权限行 {} 个、回收企业套餐补入 {} 个]",
                        result.getCreatedMenuCount(), result.getAddedPackageMenuCount());
            }
        } catch (Exception ex) {
            log.warn("[run][回收域权限同步失败，不影响启动；可稍后调用 POST /icbc/tenant/role/init 重试。args={}]",
                    Arrays.toString(args.getSourceArgs()), ex);
        }
    }

}
