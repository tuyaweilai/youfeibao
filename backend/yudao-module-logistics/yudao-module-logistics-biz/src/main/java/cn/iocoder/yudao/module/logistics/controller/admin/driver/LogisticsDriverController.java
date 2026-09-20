package cn.iocoder.yudao.module.logistics.controller.admin.driver;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
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
 * 管理后台 - 司机档案（V2a #77）。
 *
 * <p>司机是回收企业建档的租户内账号（自有与承运商同构），账号由企业创建，不自主注册（ADR 0032）。
 */
@Tag(name = "管理后台 - 司机档案")
@RestController
@RequestMapping("/logistics/driver")
@Validated
public class LogisticsDriverController {

    @Resource
    private LogisticsDriverService logisticsDriverService;

    @PostMapping("/create")
    @Operation(summary = "创建司机")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_CREATE + "')")
    public CommonResult<Long> create(@Valid @RequestBody LogisticsDriverSaveReqVO createReqVO) {
        return success(logisticsDriverService.createDriver(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新司机")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_UPDATE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody LogisticsDriverSaveReqVO updateReqVO) {
        logisticsDriverService.updateDriver(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除司机")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_DELETE + "')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        logisticsDriverService.deleteDriver(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得司机")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_QUERY + "')")
    public CommonResult<LogisticsDriverRespVO> get(@RequestParam("id") Long id) {
        LogisticsDriverDO driver = logisticsDriverService.getDriver(id);
        return success(BeanUtils.toBean(driver, LogisticsDriverRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得司机分页")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DRIVER_QUERY + "')")
    public CommonResult<PageResult<LogisticsDriverRespVO>> page(@Valid LogisticsDriverPageReqVO pageReqVO) {
        PageResult<LogisticsDriverDO> page = logisticsDriverService.getDriverPage(pageReqVO);
        return success(BeanUtils.toBean(page, LogisticsDriverRespVO.class));
    }

}
