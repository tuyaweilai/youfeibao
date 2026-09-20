package cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceLinkReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceLinkRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoicePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceSaveReqVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.inputinvoice.InputInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 进项收票登记与勾稽（#49 T11，ADR 0029）。
 *
 * <p>登记单位供货方开给回收企业的进项发票，并勾稽到采购单据（收购单 / 采购订单 / 入库单）：
 * 票、货、款三者对得上。自然人出售者不在本链路，他们走反向开票。
 */
@Tag(name = "管理后台 - 进项收票")
@RestController
@RequestMapping("/icbc/input-invoice")
@Validated
public class InputInvoiceController {

    @Resource
    private InputInvoiceService inputInvoiceService;

    @PostMapping("/create")
    @Operation(summary = "登记进项发票（同一销方 + 发票号码唯一）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.INPUT_INVOICE_MANAGE + "')")
    public CommonResult<Long> create(@Valid @RequestBody InputInvoiceSaveReqVO createReqVO) {
        return success(inputInvoiceService.createInvoice(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改进项发票（仅未勾稽的票可改）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.INPUT_INVOICE_MANAGE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody InputInvoiceSaveReqVO updateReqVO) {
        inputInvoiceService.updateInvoice(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除进项发票（仅未勾稽的票可删）")
    @Parameter(name = "id", description = "进项发票编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.INPUT_INVOICE_MANAGE + "')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        inputInvoiceService.deleteInvoice(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得进项发票详情（含勾稽记录）")
    @Parameter(name = "id", description = "进项发票编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.INPUT_INVOICE_QUERY + "')")
    public CommonResult<InputInvoiceRespVO> get(@RequestParam("id") Long id) {
        return success(inputInvoiceService.getDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得进项发票分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.INPUT_INVOICE_QUERY + "')")
    public CommonResult<PageResult<InputInvoiceRespVO>> page(@Valid InputInvoicePageReqVO pageReqVO) {
        return success(inputInvoiceService.getInvoicePage(pageReqVO));
    }

    @PostMapping("/link")
    @Operation(summary = "勾稽到采购单据（金额上限按调用方给出的单据金额校验）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.INPUT_INVOICE_MANAGE + "')")
    public CommonResult<InputInvoiceLinkRespVO> link(@Valid @RequestBody InputInvoiceLinkReqVO linkReqVO) {
        return success(inputInvoiceService.linkToBiz(linkReqVO));
    }

    @PostMapping("/unlink")
    @Operation(summary = "取消勾稽")
    @Parameter(name = "linkId", description = "勾稽记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.INPUT_INVOICE_MANAGE + "')")
    public CommonResult<Boolean> unlink(@RequestParam("linkId") Long linkId) {
        inputInvoiceService.unlink(linkId);
        return success(true);
    }

}
