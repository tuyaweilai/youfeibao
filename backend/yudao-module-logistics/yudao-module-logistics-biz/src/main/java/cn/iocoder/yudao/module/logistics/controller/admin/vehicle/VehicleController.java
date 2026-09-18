package cn.iocoder.yudao.module.logistics.controller.admin.vehicle;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.*;
import cn.iocoder.yudao.module.logistics.convert.vehicle.VehicleConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.VehicleDO;
import cn.iocoder.yudao.module.logistics.service.vehicle.VehicleService;
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

@Tag(name = "管理后台 - 车辆信息")
@RestController
@RequestMapping("/logistics/vehicle")
@Validated
public class VehicleController {

    @Resource
    private VehicleService vehicleService;

    @PostMapping("/create")
    @Operation(summary = "创建车辆信息")
    @PreAuthorize("@ss.hasPermission('logistics:vehicle:create')")
    public CommonResult<Long> createVehicle(@Valid @RequestBody VehicleCreateReqVO createReqVO) {
        return success(vehicleService.createVehicle(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新车辆信息")
    @PreAuthorize("@ss.hasPermission('logistics:vehicle:update')")
    public CommonResult<Boolean> updateVehicle(@Valid @RequestBody VehicleUpdateReqVO updateReqVO) {
        vehicleService.updateVehicle(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除车辆信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('logistics:vehicle:delete')")
    public CommonResult<Boolean> deleteVehicle(@RequestParam("id") Long id) {
        vehicleService.deleteVehicle(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得车辆信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:vehicle:query')")
    public CommonResult<VehicleRespVO> getVehicle(@RequestParam("id") Long id) {
        return success(vehicleService.getVehicleDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得车辆信息分页")
    @PreAuthorize("@ss.hasPermission('logistics:vehicle:query')")
    public CommonResult<PageResult<VehicleRespVO>> getVehiclePage(@Valid VehiclePageReqVO pageReqVO) {
        return success(vehicleService.getVehiclePage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出车辆信息 Excel")
    @PreAuthorize("@ss.hasPermission('logistics:vehicle:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportVehicleExcel(@Valid VehiclePageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<VehicleDO> list = vehicleService.getVehicleList(pageReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "车辆信息.xls", "数据", VehicleExcelVO.class,
                VehicleConvert.INSTANCE.convertExcelList(list));
    }

    @GetMapping("/list-by-enterprise")
    @Operation(summary = "根据企业ID获得车辆信息列表")
    @Parameter(name = "enterpriseId", description = "企业ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('logistics:vehicle:query')")
    public CommonResult<List<VehicleRespVO>> getVehicleListByEnterpriseId(@RequestParam("enterpriseId") Long enterpriseId) {
        List<VehicleDO> list = vehicleService.getVehicleListByEnterpriseId(enterpriseId);
        return success(VehicleConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-status")
    @Operation(summary = "根据状态获得车辆信息列表")
    @Parameter(name = "status", description = "车辆状态", required = true, example = "0")
    @PreAuthorize("@ss.hasPermission('logistics:vehicle:query')")
    public CommonResult<List<VehicleRespVO>> getVehicleListByStatus(@RequestParam("status") Integer status) {
        List<VehicleDO> list = vehicleService.getVehicleListByStatus(status);
        return success(VehicleConvert.INSTANCE.convertList(list));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新车辆状态")
    @PreAuthorize("@ss.hasPermission('logistics:vehicle:update')")
    public CommonResult<Boolean> updateVehicleStatus(@RequestParam("id") Long id,
                                                      @RequestParam("status") Integer status) {
        vehicleService.updateVehicleStatus(id, status);
        return success(true);
    }

    @GetMapping("/get-by-plate-number")
    @Operation(summary = "根据车牌号获得车辆信息")
    @Parameter(name = "plateNumber", description = "车牌号", required = true, example = "京A12345")
    @PreAuthorize("@ss.hasPermission('logistics:vehicle:query')")
    public CommonResult<VehicleRespVO> getVehicleByPlateNumber(@RequestParam("plateNumber") String plateNumber) {
        VehicleDO vehicle = vehicleService.getVehicleByPlateNumber(plateNumber);
        return success(vehicle != null ? VehicleConvert.INSTANCE.convert(vehicle) : null);
    }

} 