package cn.iocoder.yudao.module.enterprise.controller.admin.enterprise;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.*;
import cn.iocoder.yudao.module.enterprise.convert.EnterpriseInfoConvert;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseUserRelationDO;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseUserRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 企业信息
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 企业信息")
@RestController
@RequestMapping("/enterprise/info")
@Validated
public class EnterpriseInfoController {

    @Resource
    private EnterpriseInfoService enterpriseInfoService;

    @Resource
    private EnterpriseUserRelationService enterpriseUserRelationService;

    @PostMapping("/create")
    @Operation(summary = "创建企业信息")
    @PreAuthorize("@ss.hasPermission('enterprise:info:create')")
    public CommonResult<Long> createEnterpriseInfo(@Valid @RequestBody EnterpriseInfoCreateReqVO createReqVO) {
        return success(enterpriseInfoService.createEnterpriseInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新企业信息")
    @PreAuthorize("@ss.hasPermission('enterprise:info:update')")
    public CommonResult<Boolean> updateEnterpriseInfo(@Valid @RequestBody EnterpriseInfoUpdateReqVO updateReqVO) {
        enterpriseInfoService.updateEnterpriseInfo(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除企业信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('enterprise:info:delete')")
    public CommonResult<Boolean> deleteEnterpriseInfo(@RequestParam("id") Long id) {
        enterpriseInfoService.deleteEnterpriseInfo(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得企业信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('enterprise:info:query')")
    public CommonResult<EnterpriseInfoRespVO> getEnterpriseInfo(@RequestParam("id") Long id) {
        EnterpriseInfoDO enterpriseInfo = enterpriseInfoService.getEnterpriseInfo(id);
        return success(EnterpriseInfoConvert.INSTANCE.convert(enterpriseInfo));
    }

    @GetMapping("/page")
    @Operation(summary = "获得企业信息分页")
    @PreAuthorize("@ss.hasPermission('enterprise:info:query')")
    public CommonResult<PageResult<EnterpriseInfoRespVO>> pageEnterpriseInfo(@Valid EnterpriseInfoPageReqVO pageVO) {
        PageResult<EnterpriseInfoDO> pageResult = enterpriseInfoService.pageEnterpriseInfo(pageVO);
        return success(EnterpriseInfoConvert.INSTANCE.convertPage(pageResult));
    }
    
    @GetMapping("/simple-list")
    @Operation(summary = "获得企业信息精简列表", description = "主要用于下拉选择")
    @Parameter(name = "name", description = "企业名称，模糊匹配")
    @Parameter(name = "status", description = "企业状态")
    @Parameter(name = "enterpriseType", description = "企业类型")
    @PreAuthorize("@ss.hasPermission('enterprise:info:query')")
    public CommonResult<List<EnterpriseInfoSimpleRespVO>> getEnterpriseInfoSimpleList(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "enterpriseType", required = false) Integer enterpriseType) {
        List<EnterpriseInfoDO> list = enterpriseInfoService.getEnterpriseInfoSimpleList(name, status, enterpriseType);
        return success(EnterpriseInfoConvert.INSTANCE.convertSimpleList(list));
    }

    @GetMapping("/my-application")
    @Operation(summary = "获得我的企业入驻申请信息", description = "供企业申请人查看自己的入驻申请和审核状态")
    @PreAuthorize("@ss.hasPermission('enterprise:info:my-application')")
    public CommonResult<List<EnterpriseInfoRespVO>> getMyApplicationInfo() {
        // 获取当前登录用户ID
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        
        // 查询该用户关联的所有企业
        List<EnterpriseUserRelationDO> relations = enterpriseUserRelationService.getUserRelationListByUserId(userId);
        if (relations.isEmpty()) {
            return success(Collections.emptyList());
        }
        
        // 获取企业ID列表
        List<Long> enterpriseIds = relations.stream()
                .map(EnterpriseUserRelationDO::getEnterpriseId)
                .collect(Collectors.toList());
        
        // 查询企业详细信息
        List<EnterpriseInfoDO> enterprises = enterpriseInfoService.getEnterpriseInfoList(enterpriseIds);
        
        // 转换为VO对象并返回
        List<EnterpriseInfoRespVO> result = EnterpriseInfoConvert.INSTANCE.convertList(enterprises);
        return success(result);
    }

    @PutMapping("/audit")
    @Operation(summary = "审核企业入驻", description = "平台管理员审核企业入驻申请，批准或拒绝")
    @PreAuthorize("@ss.hasPermission('enterprise:info:audit')")
    public CommonResult<Boolean> auditEnterpriseInfo(@Valid @RequestBody EnterpriseInfoAuditReqVO auditReqVO) {
        enterpriseInfoService.auditEnterpriseInfo(auditReqVO);
        return success(true);
    }

} 