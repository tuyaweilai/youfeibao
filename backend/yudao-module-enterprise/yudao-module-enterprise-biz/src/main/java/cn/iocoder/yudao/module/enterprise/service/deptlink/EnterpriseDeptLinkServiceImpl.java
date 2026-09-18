package cn.iocoder.yudao.module.enterprise.service.deptlink;

import cn.iocoder.yudao.module.enterprise.api.deptlink.EnterpriseDeptLinkApi;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseDeptLinkDO;
import cn.iocoder.yudao.module.enterprise.dal.mysql.EnterpriseDeptLinkMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.enums.dept.DeptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 部门与企业关联Service实现类
 */
@Service
@Slf4j
public class EnterpriseDeptLinkServiceImpl implements EnterpriseDeptLinkApi {

    @Resource
    private EnterpriseDeptLinkMapper enterpriseDeptLinkMapper;
    
    @Resource
    private DeptApi deptApi;

    @Override
    public Long getEnterpriseIdByDeptId(@NotNull Long deptId, @NotNull Long tenantId) {
        log.debug("[getEnterpriseIdByDeptId] 查询部门关联企业, deptId: {}, tenantId: {}", deptId, tenantId);
        
        // 获取部门信息，检查是否为机构类型
        DeptRespDTO dept = deptApi.getDept(deptId);
        if (dept == null) {
            log.warn("[getEnterpriseIdByDeptId] 部门不存在, deptId: {}", deptId);
            return null;
        }
        
        // 如果不是机构类型，直接返回null
        if (!DeptTypeEnum.isInstitution(dept.getDeptType())) {
            log.debug("[getEnterpriseIdByDeptId] 部门不是机构类型, deptId: {}, deptType: {}", deptId, dept.getDeptType());
            return null;
        }
        
        // 查询关联关系
        EnterpriseDeptLinkDO link = enterpriseDeptLinkMapper.selectByDeptIdAndTenantId(deptId, tenantId);
        if (link == null) {
            log.debug("[getEnterpriseIdByDeptId] 部门未关联企业, deptId: {}", deptId);
            return null;
        }
        
        log.debug("[getEnterpriseIdByDeptId] 部门关联企业成功, deptId: {}, enterpriseId: {}", deptId, link.getEnterpriseId());
        return link.getEnterpriseId();
    }
} 