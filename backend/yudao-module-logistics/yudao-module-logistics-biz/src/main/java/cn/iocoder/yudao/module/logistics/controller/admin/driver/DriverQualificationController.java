package cn.iocoder.yudao.module.logistics.controller.admin.driver;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.*;
import cn.iocoder.yudao.module.logistics.convert.driver.DriverQualificationConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.DriverQualificationDO;
import cn.iocoder.yudao.module.logistics.service.driver.DriverQualificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

@Tag(name = "管理后台 - 司机资质信息")
@RestController
@RequestMapping("/logistics/driver-qualification")
@Validated
public class DriverQualificationController {

    @Resource
    private DriverQualificationService driverQualificationService;

    @PostMapping("/create")
    @Operation(summary = "创建司机资质信息")
    @PreAuthorize("@ss.hasPermission('logistics:driver:create')")
    public CommonResult<Long> createDriverQualification(@Valid @RequestBody DriverQualificationCreateReqVO createReqVO) {
        return success(driverQualificationService.createDriverQualification(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新司机资质信息")
    @PreAuthorize("@ss.hasPermission('logistics:driver:update')")
    public CommonResult<Boolean> updateDriverQualification(@Valid @RequestBody DriverQualificationUpdateReqVO updateReqVO) {
        driverQualificationService.updateDriverQualification(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除司机资质信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('logistics:driver:delete')")
    public CommonResult<Boolean> deleteDriverQualification(@RequestParam("id") Long id) {
        driverQualificationService.deleteDriverQualification(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得司机资质信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:driver:query')")
    public CommonResult<DriverQualificationRespVO> getDriverQualification(@RequestParam("id") Long id) {
        return success(driverQualificationService.getDriverQualificationDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得司机资质信息分页")
    @PreAuthorize("@ss.hasPermission('logistics:driver:query')")
    public CommonResult<PageResult<DriverQualificationRespVO>> getDriverQualificationPage(@Valid DriverQualificationPageReqVO pageReqVO) {
        return success(driverQualificationService.getDriverQualificationPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出司机资质信息 Excel")
    @PreAuthorize("@ss.hasPermission('logistics:driver:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDriverQualificationExcel(@Valid DriverQualificationPageReqVO pageReqVO,
                                               HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DriverQualificationDO> list = driverQualificationService.getDriverQualificationList(pageReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "司机资质信息.xls", "数据", DriverQualificationExcelVO.class,
                DriverQualificationConvert.INSTANCE.convertExcelList(list));
    }

    @GetMapping("/list-by-enterprise")
    @Operation(summary = "根据企业ID获得司机资质信息列表")
    @Parameter(name = "enterpriseId", description = "企业ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:driver:query')")
    public CommonResult<List<DriverQualificationRespVO>> getDriverQualificationListByEnterpriseId(@RequestParam("enterpriseId") Long enterpriseId) {
        List<DriverQualificationDO> list = driverQualificationService.getDriverQualificationListByEnterpriseId(enterpriseId);
        return success(DriverQualificationConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-status")
    @Operation(summary = "根据状态获得司机资质信息列表")
    @Parameter(name = "status", description = "司机状态", required = true, example = "0")
    @PreAuthorize("@ss.hasPermission('logistics:driver:query')")
    public CommonResult<List<DriverQualificationRespVO>> getDriverQualificationListByStatus(@RequestParam("status") Integer status) {
        List<DriverQualificationDO> list = driverQualificationService.getDriverQualificationListByStatus(status);
        return success(DriverQualificationConvert.INSTANCE.convertList(list));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新司机状态")
    @PreAuthorize("@ss.hasPermission('logistics:driver:update')")
    public CommonResult<Boolean> updateDriverQualificationStatus(@RequestParam("id") Long id,
                                                                  @RequestParam("status") Integer status) {
        driverQualificationService.updateDriverQualificationStatus(id, status);
        return success(true);
    }

    @GetMapping("/get-by-user-id")
    @Operation(summary = "根据用户ID获得司机资质信息")
    @Parameter(name = "userId", description = "用户ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:driver:query')")
    public CommonResult<DriverQualificationRespVO> getDriverQualificationByUserId(@RequestParam("userId") Long userId) {
        DriverQualificationDO driverQualification = driverQualificationService.getDriverQualificationByUserId(userId);
        return success(driverQualification != null ? DriverQualificationConvert.INSTANCE.convert(driverQualification) : null);
    }

    @GetMapping("/get-by-driver-code")
    @Operation(summary = "根据司机编号获得司机资质信息")
    @Parameter(name = "driverCode", description = "司机编号", required = true, example = "D001")
    @PreAuthorize("@ss.hasPermission('logistics:driver:query')")
    public CommonResult<DriverQualificationRespVO> getDriverQualificationByDriverCode(@RequestParam("driverCode") String driverCode) {
        DriverQualificationDO driverQualification = driverQualificationService.getDriverQualificationByDriverCode(driverCode);
        return success(driverQualification != null ? DriverQualificationConvert.INSTANCE.convert(driverQualification) : null);
    }

    @GetMapping("/get-by-driving-license-no")
    @Operation(summary = "根据驾驶证号码获得司机资质信息")
    @Parameter(name = "drivingLicenseNo", description = "驾驶证号码", required = true, example = "310101199001010001")
    @PreAuthorize("@ss.hasPermission('logistics:driver:query')")
    public CommonResult<DriverQualificationRespVO> getDriverQualificationByDrivingLicenseNo(@RequestParam("drivingLicenseNo") String drivingLicenseNo) {
        DriverQualificationDO driverQualification = driverQualificationService.getDriverQualificationByDrivingLicenseNo(drivingLicenseNo);
        return success(driverQualification != null ? DriverQualificationConvert.INSTANCE.convert(driverQualification) : null);
    }

} 