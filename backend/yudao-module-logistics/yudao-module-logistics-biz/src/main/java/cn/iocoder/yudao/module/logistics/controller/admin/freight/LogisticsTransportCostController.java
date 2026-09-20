package cn.iocoder.yudao.module.logistics.controller.admin.freight;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsTransportCostSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.freight.LogisticsTransportCostDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.service.freight.LogisticsTransportCostService;
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
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 运输费用（内部成本，V8 #75）。
 *
 * <p>自有车的路桥 / 燃油等按**实际承担方**记，与承运商运费分开记账，也不影响收购单金额。
 */
@Tag(name = "管理后台 - 运输费用（内部成本）")
@RestController
@RequestMapping("/logistics/transport-cost")
@Validated
public class LogisticsTransportCostController {

    @Resource
    private LogisticsTransportCostService logisticsTransportCostService;

    @PostMapping("/create")
    @Operation(summary = "登记运输费用")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_COST_CREATE + "')")
    public CommonResult<Long> create(@Valid @RequestBody LogisticsTransportCostSaveReqVO createReqVO) {
        return success(logisticsTransportCostService.createTransportCost(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改运输费用")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_COST_UPDATE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody LogisticsTransportCostSaveReqVO updateReqVO) {
        logisticsTransportCostService.updateTransportCost(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除运输费用")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_COST_DELETE + "')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        logisticsTransportCostService.deleteTransportCost(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得运输费用")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_COST_QUERY + "')")
    public CommonResult<LogisticsTransportCostRespVO> get(@RequestParam("id") Long id) {
        return success(logisticsTransportCostService.toResp(
                logisticsTransportCostService.getTransportCost(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得运输费用分页")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_COST_QUERY + "')")
    public CommonResult<PageResult<LogisticsTransportCostRespVO>> page(
            @Valid LogisticsTransportCostPageReqVO pageReqVO) {
        PageResult<LogisticsTransportCostDO> page = logisticsTransportCostService.getTransportCostPage(pageReqVO);
        return success(new PageResult<>(toRespList(page.getList()), page.getTotal()));
    }

    @GetMapping("/list-by-task")
    @Operation(summary = "按任务取运输费用")
    @Parameter(name = "taskId", description = "运输任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_COST_QUERY + "')")
    public CommonResult<List<LogisticsTransportCostRespVO>> listByTask(@RequestParam("taskId") Long taskId) {
        return success(toRespList(logisticsTransportCostService.getTransportCostListByTaskId(taskId)));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出运输费用 Excel")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_COST_QUERY + "')")
    public void exportExcel(@Valid LogisticsTransportCostPageReqVO exportReqVO,
                            HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LogisticsTransportCostDO> list = logisticsTransportCostService.getTransportCostList(exportReqVO);
        ExcelUtils.write(response, "运输费用.xls", "数据", LogisticsTransportCostRespVO.class, toRespList(list));
    }

    private List<LogisticsTransportCostRespVO> toRespList(List<LogisticsTransportCostDO> costs) {
        return costs.stream().map(logisticsTransportCostService::toResp).collect(Collectors.toList());
    }

}
