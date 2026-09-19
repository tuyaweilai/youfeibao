package cn.iocoder.yudao.module.system.api.tenant;

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

}
