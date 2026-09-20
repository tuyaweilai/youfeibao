package cn.iocoder.yudao.module.icbc.controller.admin.report;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAcquisitionLedgerPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAcquisitionLedgerRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAnomalyPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAnomalyRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportPurchasePerformancePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportPurchasePerformanceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportSettlementPaymentPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportSettlementPaymentRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockBalancePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockBalanceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockRecordPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockRecordRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportTableRespVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 经营报表（#57 T19）。
 *
 * <p>四张表（采购履约 / 收购台账 / 库存 / 结算付款）+ 一张异常表，全部只读；每张表的口径随响应返回。
 * 所有指标都可下钻到来源明细（响应里带来源单据编号 / 单号），异常表另给建议下钻入口。
 */
@Tag(name = "管理后台 - 经营报表")
@RestController
@RequestMapping("/icbc/report")
@Validated
public class ReportController {

    @Resource
    private ReportService reportService;

    @GetMapping("/tables")
    @Operation(summary = "经营报表表清单与口径说明")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.REPORT_QUERY + "')")
    public CommonResult<List<ReportTableRespVO>> tables() {
        return success(reportService.getTables());
    }

    @GetMapping("/purchase-performance/page")
    @Operation(summary = "采购履约表", description = "计划量 / 实际履约量 / 余额 / 到期日 / 进度 / 超量与过期异常")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.REPORT_QUERY + "')")
    public CommonResult<PageResult<ReportPurchasePerformanceRespVO>> purchasePerformancePage(
            @Valid ReportPurchasePerformancePageReqVO reqVO) {
        return success(reportService.getPurchasePerformancePage(reqVO));
    }

    @GetMapping("/acquisition-ledger/page")
    @Operation(summary = "收购台账", description = "交易对方 / 场站 / 回收方式 / 品类与等级 / 各重量口径 / 成交金额与对应单据")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.REPORT_QUERY + "')")
    public CommonResult<PageResult<ReportAcquisitionLedgerRespVO>> acquisitionLedgerPage(
            @Valid ReportAcquisitionLedgerPageReqVO reqVO) {
        return success(reportService.getAcquisitionLedgerPage(reqVO));
    }

    @GetMapping("/stock/balance/page")
    @Operation(summary = "库存表 - 在库量", description = "品类 + 仓库 + 库位 + 批次 + 库龄；只给数量口径")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.REPORT_QUERY + "')")
    public CommonResult<PageResult<ReportStockBalanceRespVO>> stockBalancePage(
            @Valid ReportStockBalancePageReqVO reqVO) {
        return success(reportService.getStockBalancePage(reqVO));
    }

    @GetMapping("/stock/record/page")
    @Operation(summary = "库存表 - 入出流水", description = "一行一条库存变更（正入负出）；只给数量口径")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.REPORT_QUERY + "')")
    public CommonResult<PageResult<ReportStockRecordRespVO>> stockRecordPage(
            @Valid ReportStockRecordPageReqVO reqVO) {
        return success(reportService.getStockRecordPage(reqVO));
    }

    @GetMapping("/settlement-payment/page")
    @Operation(summary = "结算付款表", description = "结算金额 / 办理进度 / 回单状态 / 失败原因与未办理时长")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.REPORT_QUERY + "')")
    public CommonResult<PageResult<ReportSettlementPaymentRespVO>> settlementPaymentPage(
            @Valid ReportSettlementPaymentPageReqVO reqVO) {
        return success(reportService.getSettlementPaymentPage(reqVO));
    }

    @GetMapping("/anomaly/page")
    @Operation(summary = "异常表", description = "磅差 / 超采购量 / 超入库量 / 重复关联 / 长期未确认 / 资料缺失（派生清单）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.REPORT_QUERY + "')")
    public CommonResult<PageResult<ReportAnomalyRespVO>> anomalyPage(
            @Valid ReportAnomalyPageReqVO reqVO) {
        return success(reportService.getAnomalyPage(reqVO));
    }

}
