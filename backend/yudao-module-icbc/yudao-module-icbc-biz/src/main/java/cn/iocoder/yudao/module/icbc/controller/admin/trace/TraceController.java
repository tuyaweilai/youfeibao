package cn.iocoder.yudao.module.icbc.controller.admin.trace;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceRowRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceSearchReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceSearchRespVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.trace.TraceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 关联单据查询（#55 T17）。
 *
 * <p>只读聚合：四栏 + 付款 / 发票的单链路追溯，按单号 / 车牌 / 主体反查。
 * 默认脱敏；未脱敏查看与导出处两处独立权限（{@code icbc:trace:sensitive:view} / {@code icbc:trace:export}），
 * 导出另留操作记录。
 */
@Tag(name = "管理后台 - 关联单据查询")
@RestController
@RequestMapping("/icbc/trace")
@Validated
public class TraceController {

    @Resource
    private TraceQueryService traceQueryService;

    @GetMapping("/search")
    @Operation(summary = "反查一批货的链路", description = "按单号 / 车牌 / 主体反查；含汇总、筛选范围与未展示明细数，敏感字段默认脱敏")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.TRACE_QUERY + "')")
    public CommonResult<TraceSearchRespVO> search(@Valid TraceSearchReqVO reqVO) {
        return success(traceQueryService.search(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "单张收购单的完整链路（脱敏）", description = "四栏 + 付款 / 发票，一对多环节展开全部明细；税号 / 身份证 / 手机号 / 银行卡按岗位权限脱敏")
    @Parameter(name = "acquisitionId", description = "收购单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.TRACE_QUERY + "')")
    public CommonResult<TraceRowRespVO> get(@RequestParam("acquisitionId") Long acquisitionId) {
        return success(traceQueryService.getTrace(acquisitionId, false));
    }

    @GetMapping("/sensitive")
    @Operation(summary = "单张收购单的完整链路（未脱敏）",
            description = "返回税号 / 身份证 / 手机号 / 银行卡原值；需 icbc:trace:sensitive:view 岗位权限")
    @Parameter(name = "acquisitionId", description = "收购单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.TRACE_SENSITIVE_VIEW + "')")
    public CommonResult<TraceRowRespVO> getSensitive(@RequestParam("acquisitionId") Long acquisitionId) {
        return success(traceQueryService.getTrace(acquisitionId, true));
    }

    @GetMapping("/export")
    @Operation(summary = "导出关联单据查询结果", description = "与在线查看同一套脱敏判断；导出动作留操作记录")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.TRACE_EXPORT + "')")
    public void export(@Valid TraceSearchReqVO reqVO, HttpServletResponse response) {
        traceQueryService.export(reqVO, response);
    }

}
