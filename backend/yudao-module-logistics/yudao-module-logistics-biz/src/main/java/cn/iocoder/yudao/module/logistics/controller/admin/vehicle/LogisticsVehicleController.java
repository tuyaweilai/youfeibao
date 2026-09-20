package cn.iocoder.yudao.module.logistics.controller.admin.vehicle;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehiclePageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehicleSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.LogisticsVehicleDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.service.vehicle.LogisticsVehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
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

/**
 * 管理后台 - 车辆档案（V2a #77）。
 *
 * <p>车辆是派车的可选对象，车牌租户内唯一。「运输中」不接受手工设置（由运输任务驱动）。
 */
@Tag(name = "管理后台 - 车辆档案")
@RestController
@RequestMapping("/logistics/vehicle")
@Validated
public class LogisticsVehicleController {

    @Resource
    private LogisticsVehicleService logisticsVehicleService;

    /** 状态名：列表与详情都要给，页面直接显示，不让前端各自维护一份文案 */
    private String statusName(Integer status) {
        return cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum.ofStatus(status)
                .map(cn.iocoder.yudao.module.logistics.enums.LogisticsVehicleStatusEnum::getName).orElse(null);
    }

    @PostMapping("/create")
    @Operation(summary = "创建车辆")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.VEHICLE_CREATE + "')")
    public CommonResult<Long> create(@Valid @RequestBody LogisticsVehicleSaveReqVO createReqVO) {
        return success(logisticsVehicleService.createVehicle(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新车辆")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.VEHICLE_UPDATE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody LogisticsVehicleSaveReqVO updateReqVO) {
        logisticsVehicleService.updateVehicle(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除车辆")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.VEHICLE_DELETE + "')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        logisticsVehicleService.deleteVehicle(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得车辆")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.VEHICLE_QUERY + "')")
    public CommonResult<LogisticsVehicleRespVO> get(@RequestParam("id") Long id) {
        LogisticsVehicleDO vehicle = logisticsVehicleService.getVehicle(id);
        return success(BeanUtils.toBean(vehicle, LogisticsVehicleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出车辆 Excel")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.VEHICLE_EXPORT + "')")
    public void exportExcel(@Valid LogisticsVehiclePageReqVO exportReqVO,
                            HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LogisticsVehicleDO> list = logisticsVehicleService.getVehicleList(exportReqVO);
        List<LogisticsVehicleRespVO> rows = BeanUtils.toBean(list, LogisticsVehicleRespVO.class);
        rows.forEach(row -> row.setStatusName(statusName(row.getStatus())));
        ExcelUtils.write(response, "车辆.xls", "数据", LogisticsVehicleRespVO.class, rows);
    }

    @GetMapping("/page")
    @Operation(summary = "获得车辆分页")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.VEHICLE_QUERY + "')")
    public CommonResult<PageResult<LogisticsVehicleRespVO>> page(@Valid LogisticsVehiclePageReqVO pageReqVO) {
        PageResult<LogisticsVehicleDO> page = logisticsVehicleService.getVehiclePage(pageReqVO);
        PageResult<LogisticsVehicleRespVO> result = BeanUtils.toBean(page, LogisticsVehicleRespVO.class);
        result.getList().forEach(resp -> resp.setStatusName(statusName(resp.getStatus())));
        return success(result);
    }

}
