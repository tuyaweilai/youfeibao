package cn.iocoder.yudao.module.icbc.controller.admin.platform;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo.IcbcQualificationPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo.IcbcQualificationRespVO;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 平台运营：跨租户资质核实
 *
 * <p>只有平台运营（或超管）能进。资质是租户内的表，这里用跨租户读 + 回写，
 * 对应 #5「平台运营可核实资质」。
 */
@Tag(name = "管理后台 - 平台运营跨租户资质")
@RestController
@RequestMapping("/icbc/platform/qualification")
@Validated
public class PlatformQualificationController {

    @Resource
    private IcbcQualificationService qualificationService;

    @GetMapping("/page")
    @Operation(summary = "获得全平台资质分页（跨租户，可按 tenantId 过滤）")
    @PreAuthorize("@icbc.hasPermission('icbc:platform:qualification:query')")
    public CommonResult<PageResult<IcbcQualificationRespVO>> page(@Valid IcbcQualificationPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(qualificationService.getQualificationPageIgnoreTenant(pageReqVO),
                IcbcQualificationRespVO.class));
    }

    @PutMapping("/audit")
    @Operation(summary = "核实资质：回写状态与核实意见")
    @Parameter(name = "id", description = "编号", required = true)
    @Parameter(name = "status", description = "0-待核实，1-有效，2-失效，3-吊销", required = true)
    @Parameter(name = "auditRemark", description = "核实意见")
    @PreAuthorize("@icbc.hasPermission('icbc:platform:qualification:audit')")
    public CommonResult<Boolean> audit(@RequestParam("id") Long id,
                                       @RequestParam("status") Integer status,
                                       @RequestParam(value = "auditRemark", required = false) String auditRemark) {
        qualificationService.auditQualification(id, status, auditRemark);
        return success(true);
    }

}
