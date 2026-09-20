package cn.iocoder.yudao.module.system.api.tenant;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.system.controller.admin.tenant.vo.packages.TenantPackageSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantPackageDO;
import cn.iocoder.yudao.module.system.service.tenant.TenantPackageService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 多租户的 API 实现类
 *
 * @author 芋道源码
 */
@Service
public class TenantApiImpl implements TenantApi {

    @Resource
    private TenantService tenantService;

    @Resource
    private TenantPackageService tenantPackageService;

    @Override
    public List<Long> getTenantIdList() {
        return tenantService.getTenantIdList();
    }

    @Override
    public void validateTenant(Long id) {
        tenantService.validTenant(id);
    }

    @Override
    public String getTenantName(Long id) {
        if (id == null) {
            return null;
        }
        TenantDO tenant = tenantService.getTenant(id);
        return tenant == null ? null : tenant.getName();
    }

    @Override
    public int addTenantPackageMenuIds(Long packageId, Collection<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return 0;
        }
        TenantPackageDO tenantPackage = tenantPackageService.getTenantPackage(packageId);
        if (tenantPackage == null) {
            return 0; // 套餐不存在（例如未导入回收企业套餐），忽略
        }
        Set<Long> currentMenuIds = CollUtil.emptyIfNull(tenantPackage.getMenuIds());
        Set<Long> toAddMenuIds = new LinkedHashSet<>(menuIds);
        toAddMenuIds.removeAll(currentMenuIds);
        if (toAddMenuIds.isEmpty()) {
            return 0;
        }
        Set<Long> unionMenuIds = new LinkedHashSet<>(currentMenuIds);
        unionMenuIds.addAll(menuIds);
        TenantPackageSaveReqVO updateReqVO = new TenantPackageSaveReqVO();
        updateReqVO.setId(tenantPackage.getId());
        updateReqVO.setName(tenantPackage.getName());
        updateReqVO.setStatus(tenantPackage.getStatus());
        updateReqVO.setRemark(tenantPackage.getRemark());
        updateReqVO.setMenuIds(unionMenuIds);
        tenantPackageService.updateTenantPackage(updateReqVO);
        return toAddMenuIds.size();
    }

}
