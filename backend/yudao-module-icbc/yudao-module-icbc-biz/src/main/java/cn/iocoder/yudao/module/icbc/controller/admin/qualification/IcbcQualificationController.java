package cn.iocoder.yudao.module.icbc.controller.admin.qualification;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo.IcbcQualificationPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo.IcbcQualificationRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo.IcbcQualificationSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.qualification.IcbcQualificationDO;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 租户三层资质
 */
@Tag(name = "管理后台 - 租户三层资质")
@RestController
@RequestMapping("/icbc/qualification")
@Validated
public class IcbcQualificationController {

    @Resource
    private IcbcQualificationService qualificationService;

    @PostMapping("/create")
    @Operation(summary = "创建资质")
    @PreAuthorize("@icbc.hasPermission('icbc:qualification:create')")
    public CommonResult<Long> create(@Valid @RequestBody IcbcQualificationSaveReqVO createReqVO) {
        return success(qualificationService.createQualification(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资质")
    @PreAuthorize("@icbc.hasPermission('icbc:qualification:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody IcbcQualificationSaveReqVO updateReqVO) {
        qualificationService.updateQualification(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资质")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@icbc.hasPermission('icbc:qualification:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        qualificationService.deleteQualification(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资质")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@icbc.hasPermission('icbc:qualification:query')")
    public CommonResult<IcbcQualificationRespVO> get(@RequestParam("id") Long id) {
        return success(toResp(qualificationService.getQualification(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得资质分页")
    @PreAuthorize("@icbc.hasPermission('icbc:qualification:query')")
    public CommonResult<PageResult<IcbcQualificationRespVO>> page(@Valid IcbcQualificationPageReqVO pageReqVO) {
        PageResult<IcbcQualificationDO> page = qualificationService.getQualificationPage(pageReqVO);
        return success(BeanUtils.toBean(page, IcbcQualificationRespVO.class, this::fillExpiringSoon));
    }

    @GetMapping("/list")
    @Operation(summary = "获得资质列表")
    @PreAuthorize("@icbc.hasPermission('icbc:qualification:query')")
    public CommonResult<List<IcbcQualificationRespVO>> list() {
        return success(qualificationService.getQualificationList().stream().map(this::toResp).toList());
    }

    @GetMapping("/expiring")
    @Operation(summary = "获得临近到期的资质（默认 30 天内）")
    @PreAuthorize("@icbc.hasPermission('icbc:qualification:query')")
    public CommonResult<List<IcbcQualificationRespVO>> expiring(
            @RequestParam(value = "days", required = false, defaultValue = "30") Integer days) {
        return success(qualificationService.getExpiringList(days).stream().map(this::toResp).toList());
    }

    @GetMapping("/tenant-ready")
    @Operation(summary = "本租户三层资质是否齐全有效（开票就绪）")
    @PreAuthorize("@icbc.hasPermission('icbc:qualification:query')")
    public CommonResult<Boolean> tenantReady() {
        return success(qualificationService.isTenantReady());
    }

    private IcbcQualificationRespVO toResp(IcbcQualificationDO doObj) {
        IcbcQualificationRespVO vo = BeanUtils.toBean(doObj, IcbcQualificationRespVO.class);
        fillExpiringSoon(vo);
        return vo;
    }

    private void fillExpiringSoon(IcbcQualificationRespVO vo) {
        LocalDate today = LocalDate.now();
        vo.setExpiringSoon(vo.getValidTo() != null
                && !vo.getValidTo().isBefore(today)
                && !vo.getValidTo().isAfter(today.plusDays(30)));
    }

}
