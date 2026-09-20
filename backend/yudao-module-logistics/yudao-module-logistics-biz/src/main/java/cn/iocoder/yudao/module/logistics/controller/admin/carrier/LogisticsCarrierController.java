package cn.iocoder.yudao.module.logistics.controller.admin.carrier;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo.LogisticsCarrierSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierService;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
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

/**
 * 管理后台 - 承运商档案（V3 #70）。
 *
 * <p>运力吃紧时找的第三方公司。停用而不是删除：历史任务上的司机与运费要留着。
 */
@Tag(name = "管理后台 - 承运商档案")
@RestController
@RequestMapping("/logistics/carrier")
@Validated
public class LogisticsCarrierController {

    @Resource
    private LogisticsCarrierService logisticsCarrierService;

    @PostMapping("/create")
    @Operation(summary = "创建承运商")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_CREATE + "')")
    public CommonResult<Long> create(@Valid @RequestBody LogisticsCarrierSaveReqVO createReqVO) {
        return success(logisticsCarrierService.createCarrier(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新承运商")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_UPDATE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody LogisticsCarrierSaveReqVO updateReqVO) {
        logisticsCarrierService.updateCarrier(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除承运商")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_DELETE + "')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        logisticsCarrierService.deleteCarrier(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得承运商")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_QUERY + "')")
    public CommonResult<LogisticsCarrierRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(logisticsCarrierService.getCarrier(id), LogisticsCarrierRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得承运商分页")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_QUERY + "')")
    public CommonResult<PageResult<LogisticsCarrierRespVO>> page(@Valid LogisticsCarrierPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(logisticsCarrierService.getCarrierPage(pageReqVO),
                LogisticsCarrierRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出承运商 Excel")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_EXPORT + "')")
    public void exportExcel(@Valid LogisticsCarrierPageReqVO exportReqVO,
                            HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LogisticsCarrierDO> list = logisticsCarrierService.getCarrierList(exportReqVO);
        List<LogisticsCarrierRespVO> rows = BeanUtils.toBean(list, LogisticsCarrierRespVO.class);
        rows.forEach(row -> row.setStatusName(row.getStatus() != null && row.getStatus() == 0 ? "合作中" : "已停用"));
        ExcelUtils.write(response, "承运商.xls", "数据", LogisticsCarrierRespVO.class, rows);
    }

}
