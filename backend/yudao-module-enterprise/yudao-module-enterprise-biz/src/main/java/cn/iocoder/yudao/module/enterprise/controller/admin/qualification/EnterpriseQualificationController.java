package cn.iocoder.yudao.module.enterprise.controller.admin.qualification;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.*;
import cn.iocoder.yudao.module.enterprise.convert.EnterpriseQualificationConvert;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseQualificationDO;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseQualificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 企业资质
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 企业资质")
@RestController
@RequestMapping("/enterprise/qualification")
@Validated
public class EnterpriseQualificationController {

    @Resource
    private EnterpriseQualificationService enterpriseQualificationService;

    @PostMapping("/create")
    @Operation(summary = "创建企业资质")
    @PreAuthorize("@ss.hasPermission('enterprise:cert:create')")
    public CommonResult<Long> createEnterpriseQualification(@Valid @RequestBody EnterpriseQualificationCreateReqVO createReqVO) {
        return success(enterpriseQualificationService.createEnterpriseQualification(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新企业资质")
    @PreAuthorize("@ss.hasPermission('enterprise:cert:update')")
    public CommonResult<Boolean> updateEnterpriseQualification(@Valid @RequestBody EnterpriseQualificationUpdateReqVO updateReqVO) {
        enterpriseQualificationService.updateEnterpriseQualification(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除企业资质")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('enterprise:cert:delete')")
    public CommonResult<Boolean> deleteEnterpriseQualification(@RequestParam("id") Long id) {
        enterpriseQualificationService.deleteEnterpriseQualification(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得企业资质")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('enterprise:cert:query')")
    public CommonResult<EnterpriseQualificationRespVO> getEnterpriseQualification(@RequestParam("id") Long id) {
        EnterpriseQualificationDO qualification = enterpriseQualificationService.getEnterpriseQualification(id);
        return success(EnterpriseQualificationConvert.INSTANCE.convert(qualification));
    }

    @GetMapping("/list-by-enterprise")
    @Operation(summary = "获得企业资质列表")
    @Parameter(name = "enterpriseId", description = "企业编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('enterprise:cert:query')")
    public CommonResult<List<EnterpriseQualificationRespVO>> getEnterpriseQualificationListByEnterpriseId(@RequestParam("enterpriseId") Long enterpriseId) {
        List<EnterpriseQualificationDO> list = enterpriseQualificationService.getEnterpriseQualificationListByEnterpriseId(enterpriseId);
        return success(EnterpriseQualificationConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/page")
    @Operation(summary = "获得企业资质分页")
    @PreAuthorize("@ss.hasPermission('enterprise:cert:query')")
    public CommonResult<PageResult<EnterpriseQualificationRespVO>> pageEnterpriseQualification(@Valid EnterpriseQualificationPageReqVO pageVO) {
        PageResult<EnterpriseQualificationDO> pageResult = enterpriseQualificationService.pageEnterpriseQualification(pageVO);
        return success(EnterpriseQualificationConvert.INSTANCE.convertPage(pageResult));
    }

    @PutMapping("/audit")
    @Operation(summary = "审核企业资质")
    @PreAuthorize("@ss.hasPermission('enterprise:cert:audit')")
    public CommonResult<Boolean> auditEnterpriseQualification(@Valid @RequestBody EnterpriseQualificationAuditReqVO auditReqVO) {
        enterpriseQualificationService.auditEnterpriseQualification(auditReqVO);
        return success(true);
    }

} 