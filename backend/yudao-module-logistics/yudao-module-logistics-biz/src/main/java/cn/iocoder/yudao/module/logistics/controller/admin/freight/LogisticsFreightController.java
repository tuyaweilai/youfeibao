package cn.iocoder.yudao.module.logistics.controller.admin.freight;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightConfirmReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightPayReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightReconciliationReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightReconciliationRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightUpdateReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.freight.LogisticsFreightOrderDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.service.freight.LogisticsFreightService;
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
 * 管理后台 - 承运商运费与对账（V8 #75）。
 *
 * <p>运费是**付给承运商的应付**，与收购单里的调整项不是同一个「运费」（CONTEXT.md「运费」）：
 * 本控制器不读也不写任何收购单 / 发票金额。
 */
@Tag(name = "管理后台 - 承运商运费与对账")
@RestController
@RequestMapping("/logistics/freight")
@Validated
public class LogisticsFreightController {

    @Resource
    private LogisticsFreightService logisticsFreightService;

    @PostMapping("/create")
    @Operation(summary = "按趟次汇集运费")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.FREIGHT_CREATE + "')")
    public CommonResult<Long> create(@Valid @RequestBody LogisticsFreightCreateReqVO createReqVO) {
        return success(logisticsFreightService.createFreight(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改运费单（仅待确认应付）")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.FREIGHT_UPDATE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody LogisticsFreightUpdateReqVO updateReqVO) {
        logisticsFreightService.updateFreight(updateReqVO);
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认应付（差异必须留原因）")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.FREIGHT_CONFIRM + "')")
    public CommonResult<Boolean> confirm(@Valid @RequestBody LogisticsFreightConfirmReqVO confirmReqVO) {
        logisticsFreightService.confirmPayable(confirmReqVO);
        return success(true);
    }

    @PutMapping("/pay")
    @Operation(summary = "登记外部付款凭证（不接对公付款通道）")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.FREIGHT_PAY + "')")
    public CommonResult<Boolean> pay(@Valid @RequestBody LogisticsFreightPayReqVO payReqVO) {
        logisticsFreightService.registerPaymentVoucher(payReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得运费单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.FREIGHT_QUERY + "')")
    public CommonResult<LogisticsFreightRespVO> get(@RequestParam("id") Long id) {
        return success(logisticsFreightService.toResp(logisticsFreightService.getFreight(id)));
    }

    @GetMapping("/get-by-task")
    @Operation(summary = "按任务获得运费单（没有返回 null）")
    @Parameter(name = "taskId", description = "运输任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.FREIGHT_QUERY + "')")
    public CommonResult<LogisticsFreightRespVO> getByTask(@RequestParam("taskId") Long taskId) {
        LogisticsFreightOrderDO order = logisticsFreightService.getFreightByTaskId(taskId);
        return success(order == null ? null : logisticsFreightService.toResp(order));
    }

    @GetMapping("/page")
    @Operation(summary = "获得运费单分页")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.FREIGHT_QUERY + "')")
    public CommonResult<PageResult<LogisticsFreightRespVO>> page(@Valid LogisticsFreightPageReqVO pageReqVO) {
        PageResult<LogisticsFreightOrderDO> page = logisticsFreightService.getFreightPage(pageReqVO);
        return success(new PageResult<>(toRespList(page.getList()), page.getTotal()));
    }

    @GetMapping("/reconciliation")
    @Operation(summary = "按承运商与合同汇集运费（对账）")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.FREIGHT_QUERY + "')")
    public CommonResult<List<LogisticsFreightReconciliationRespVO>> reconciliation(
            @Valid LogisticsFreightReconciliationReqVO reqVO) {
        return success(logisticsFreightService.getReconciliation(reqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出运费单 Excel")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.FREIGHT_EXPORT + "')")
    public void exportExcel(@Valid LogisticsFreightPageReqVO exportReqVO,
                            HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LogisticsFreightOrderDO> list = logisticsFreightService.getFreightList(exportReqVO);
        ExcelUtils.write(response, "承运商运费.xls", "数据", LogisticsFreightRespVO.class, toRespList(list));
    }

    @GetMapping("/reconciliation/export-excel")
    @Operation(summary = "导出运费对账 Excel")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.FREIGHT_EXPORT + "')")
    public void exportReconciliation(@Valid LogisticsFreightReconciliationReqVO reqVO,
                                     HttpServletResponse response) throws IOException {
        List<LogisticsFreightReconciliationRespVO> list = logisticsFreightService.getReconciliation(reqVO);
        ExcelUtils.write(response, "运费对账.xls", "数据", LogisticsFreightReconciliationRespVO.class, list);
    }

    private List<LogisticsFreightRespVO> toRespList(List<LogisticsFreightOrderDO> orders) {
        return orders.stream().map(logisticsFreightService::toResp).collect(Collectors.toList());
    }

}
