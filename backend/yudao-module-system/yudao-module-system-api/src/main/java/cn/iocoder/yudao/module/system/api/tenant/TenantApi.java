package cn.iocoder.yudao.module.system.api.tenant;

import java.util.Collection;
import java.util.List;

/**
 * 多租户的 API 接口
 *
 * @author 芋道源码
 */
public interface TenantApi {

    /**
     * 获得所有租户
     *
     * @return 租户编号数组
     */
    List<Long> getTenantIdList();

    /**
     * 校验租户是否合法
     *
     * @param id 租户编号
     */
    void validateTenant(Long id);

    /**
     * 获得租户名称（回收企业的对外名称）。查不到返回 {@code null}。
     *
     * <p>自然人端免登录首屏只展示公开信息，需要把租户编号翻译成企业名称。
     *
     * @param id 租户编号
     * @return 租户名称
     */
    String getTenantName(Long id);

    /**
     * 给租户套餐追加菜单（幂等：只新增，不改已有集合；套餐不存在时忽略）。
     *
     * <p>业务模块新增权限后，用它将权限菜单补进「回收企业套餐」，新开的租户才会开箱即得。
     *
     * @param packageId 套餐编号
     * @param menuIds   菜单编号集合
     * @return 实际新增的菜单数量
     */
    int addTenantPackageMenuIds(Long packageId, Collection<Long> menuIds);

}
