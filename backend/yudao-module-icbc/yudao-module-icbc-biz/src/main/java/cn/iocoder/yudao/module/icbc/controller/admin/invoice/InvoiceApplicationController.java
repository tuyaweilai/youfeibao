package cn.iocoder.yudao.module.icbc.controller.admin.invoice;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationBatchReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationResultVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreCheckRespVO;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.CREATE;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.GET;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 开票申请（预下单与自然人确认）
 *
 * <p>对应 issue #8：对已登记的收购发起开票申请。这一步不产生发票，只取得自然人确认页面；
 * 出售者确认后预开票状态变为「预开票成功」，真正的票要等付款之后。
 */
@Tag(name = "管理后台 - 开票申请")
@RestController
@RequestMapping("/icbc/invoice-application")
@Validated
@Slf4j
public class InvoiceApplicationController {

    @Resource
    private InvoiceApplicationService invoiceApplicationService;

    @GetMapping("/pre-check")
    @Operation(summary = "开票申请前置校验", description = "逐项列出五类校验结果与补齐方式，不产生业务")
    @Parameter(name = "acquisitionId", description = "收购单编号", required = true, example = "1")
    @Parameter(name = "invoiceType", description = "发票类型：01-专票，02-普票", example = "02")
    @PreAuthorize("@icbc.hasPermission('icbc:invoice-application:query')")
    @ApiAccessLog(operateType = GET)
    public CommonResult<InvoicePreCheckRespVO> preCheck(@RequestParam("acquisitionId") Long acquisitionId,
                                                        @RequestParam(value = "invoiceType", required = false,
                                                                defaultValue = "02") String invoiceType) {
        return success(invoiceApplicationService.preCheck(acquisitionId, invoiceType));
    }

    @PostMapping("/apply")
    @Operation(summary = "单笔发起开票申请", description = "校验通过后返回自然人确认页面，不产生发票")
    @PreAuthorize("@icbc.hasPermission('icbc:invoice-application:apply')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<InvoiceApplicationResultVO> apply(@Valid @RequestBody InvoiceApplicationApplyReqVO reqVO) {
        return success(invoiceApplicationService.apply(reqVO));
    }

    @PostMapping("/apply-batch")
    @Operation(summary = "批量发起开票申请", description = "逐笔独立校验与提交，某一笔失败不影响其他笔")
    @PreAuthorize("@icbc.hasPermission('icbc:invoice-application:apply')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<List<InvoiceApplicationResultVO>> applyBatch(
            @Valid @RequestBody InvoiceApplicationBatchReqVO reqVO) {
        return success(invoiceApplicationService.applyBatch(reqVO));
    }
}
