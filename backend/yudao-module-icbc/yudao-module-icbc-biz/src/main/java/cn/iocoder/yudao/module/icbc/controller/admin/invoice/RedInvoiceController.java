package cn.iocoder.yudao.module.icbc.controller.admin.invoice;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceApplyResultVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceRevokeReqVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.invoice.RedInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.CREATE;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.GET;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 红冲与发票取消（#14）
 */
@Tag(name = "管理后台 - 红冲与发票取消")
@RestController
@RequestMapping("/icbc/red-invoice")
@Validated
@Slf4j
public class RedInvoiceController {

    @Resource
    private RedInvoiceService redInvoiceService;

    @PostMapping("/apply")
    @Operation(summary = "发起红字冲销，取得红字确认单页面")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.RED_INVOICE_APPLY + "')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<RedInvoiceApplyResultVO> apply(@Valid @RequestBody RedInvoiceApplyReqVO reqVO) {
        return success(redInvoiceService.apply(reqVO));
    }

    @PostMapping("/revoke")
    @Operation(summary = "撤销尚未生效的红字确认单")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.RED_INVOICE_REVOKE + "')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<RedInvoiceQueryRespVO> revoke(@Valid @RequestBody RedInvoiceRevokeReqVO reqVO) {
        return success(redInvoiceService.revoke(reqVO));
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消预开票成功但尚未支付的发票")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.INVOICE_CANCEL + "')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<Boolean> cancel(@Valid @RequestBody InvoiceCancelReqVO reqVO) {
        redInvoiceService.cancelPreInvoice(reqVO.getPartnerOrderId());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "按红冲流水号查询红字发票")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.RED_INVOICE_QUERY + "')")
    @ApiAccessLog(operateType = GET)
    public CommonResult<RedInvoiceQueryRespVO> get(@RequestParam("redOffsetNo") String redOffsetNo) {
        return success(redInvoiceService.getByRedOffsetNo(redOffsetNo));
    }

    @GetMapping("/get-by-partner")
    @Operation(summary = "按蓝票合作方订单号查询最近一次红冲记录")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.RED_INVOICE_QUERY + "')")
    @ApiAccessLog(operateType = GET)
    public CommonResult<RedInvoiceQueryRespVO> getByPartner(
            @RequestParam("partnerOrderId") @Parameter(description = "蓝票合作方订单号") String partnerOrderId) {
        return success(redInvoiceService.getByPartnerOrderId(partnerOrderId));
    }

    @GetMapping("/query")
    @Operation(summary = "主动向工行查询红冲最新状态并收敛")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.RED_INVOICE_QUERY + "')")
    @ApiAccessLog(operateType = GET)
    public CommonResult<RedInvoiceQueryRespVO> query(@RequestParam("redOffsetNo") String redOffsetNo) {
        return success(redInvoiceService.refresh(redOffsetNo));
    }
}
