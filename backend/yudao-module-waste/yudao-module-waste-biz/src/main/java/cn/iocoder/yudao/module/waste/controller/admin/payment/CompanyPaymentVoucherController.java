package cn.iocoder.yudao.module.waste.controller.admin.payment;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.CompanyPaymentVoucherDO;
import cn.iocoder.yudao.module.waste.service.payment.CompanyPaymentVoucherService;
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

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 对公付款凭证")
@RestController
@RequestMapping("/waste/company-payment-voucher")
@Validated
public class CompanyPaymentVoucherController {

    @Resource
    private CompanyPaymentVoucherService companyPaymentVoucherService;

    @PostMapping("/create")
    @Operation(summary = "创建对公付款凭证")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:create')")
    public CommonResult<Long> createCompanyPaymentVoucher(@Valid @RequestBody CompanyPaymentVoucherCreateReqVO createReqVO) {
        return success(companyPaymentVoucherService.createCompanyPaymentVoucher(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新对公付款凭证")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:update')")
    public CommonResult<Boolean> updateCompanyPaymentVoucher(@Valid @RequestBody CompanyPaymentVoucherUpdateReqVO updateReqVO) {
        companyPaymentVoucherService.updateCompanyPaymentVoucher(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除对公付款凭证")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:delete')")
    public CommonResult<Boolean> deleteCompanyPaymentVoucher(@RequestParam("id") Long id) {
        companyPaymentVoucherService.deleteCompanyPaymentVoucher(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得对公付款凭证")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:query')")
    public CommonResult<CompanyPaymentVoucherRespVO> getCompanyPaymentVoucher(@RequestParam("id") Long id) {
        CompanyPaymentVoucherDO companyPaymentVoucher = companyPaymentVoucherService.getCompanyPaymentVoucher(id);
        return success(BeanUtils.toBean(companyPaymentVoucher, CompanyPaymentVoucherRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得对公付款凭证分页")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:query')")
    public CommonResult<PageResult<CompanyPaymentVoucherRespVO>> getCompanyPaymentVoucherPage(@Valid CompanyPaymentVoucherPageReqVO pageReqVO) {
        PageResult<CompanyPaymentVoucherDO> pageResult = companyPaymentVoucherService.getCompanyPaymentVoucherPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CompanyPaymentVoucherRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出对公付款凭证 Excel")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCompanyPaymentVoucherExcel(@Valid CompanyPaymentVoucherPageReqVO pageReqVO,
                                                  HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CompanyPaymentVoucherDO> list = companyPaymentVoucherService.getCompanyPaymentVoucherPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "对公付款凭证.xls", "数据", CompanyPaymentVoucherRespVO.class,
                BeanUtils.toBean(list, CompanyPaymentVoucherRespVO.class));
    }

    // ==================== 业务方法 ====================

    @PostMapping("/producer-confirm")
    @Operation(summary = "产废企业确认凭证")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:confirm')")
    public CommonResult<Boolean> producerConfirm(@RequestParam("id") Long id,
                                                 @RequestParam("confirmed") Boolean confirmed,
                                                 @RequestParam("confirmBy") String confirmBy) {
        companyPaymentVoucherService.producerConfirm(id, confirmed, confirmBy);
        return success(true);
    }

    @PostMapping("/recycler-confirm")
    @Operation(summary = "回收企业确认凭证")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:confirm')")
    public CommonResult<Boolean> recyclerConfirm(@RequestParam("id") Long id,
                                                 @RequestParam("confirmed") Boolean confirmed,
                                                 @RequestParam("confirmBy") String confirmBy) {
        companyPaymentVoucherService.recyclerConfirm(id, confirmed, confirmBy);
        return success(true);
    }

    @PostMapping("/handle-dispute")
    @Operation(summary = "处理争议凭证")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:dispute')")
    public CommonResult<Boolean> handleDispute(@RequestParam("id") Long id,
                                               @RequestParam("resolution") String resolution) {
        companyPaymentVoucherService.handleDispute(id, resolution);
        return success(true);
    }

    @GetMapping("/by-order")
    @Operation(summary = "根据订单ID获取付款凭证")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:query')")
    public CommonResult<List<CompanyPaymentVoucherRespVO>> getVouchersByOrderId(@RequestParam("orderId") Long orderId) {
        List<CompanyPaymentVoucherDO> list = companyPaymentVoucherService.getVouchersByOrderId(orderId);
        return success(BeanUtils.toBean(list, CompanyPaymentVoucherRespVO.class));
    }

    @GetMapping("/pending-producer-confirm")
    @Operation(summary = "获取待产废企业确认的凭证")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:query')")
    public CommonResult<List<CompanyPaymentVoucherRespVO>> getPendingProducerConfirmVouchers() {
        List<CompanyPaymentVoucherDO> list = companyPaymentVoucherService.getPendingProducerConfirmVouchers();
        return success(BeanUtils.toBean(list, CompanyPaymentVoucherRespVO.class));
    }

    @GetMapping("/pending-recycler-confirm")
    @Operation(summary = "获取待回收企业确认的凭证")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:query')")
    public CommonResult<List<CompanyPaymentVoucherRespVO>> getPendingRecyclerConfirmVouchers() {
        List<CompanyPaymentVoucherDO> list = companyPaymentVoucherService.getPendingRecyclerConfirmVouchers();
        return success(BeanUtils.toBean(list, CompanyPaymentVoucherRespVO.class));
    }

    @GetMapping("/dispute-vouchers")
    @Operation(summary = "获取争议凭证列表")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:query')")
    public CommonResult<List<CompanyPaymentVoucherRespVO>> getDisputeVouchers() {
        List<CompanyPaymentVoucherDO> list = companyPaymentVoucherService.getDisputeVouchers();
        return success(BeanUtils.toBean(list, CompanyPaymentVoucherRespVO.class));
    }

    @PutMapping("/batch-confirm")
    @Operation(summary = "批量确认凭证")
    @PreAuthorize("@ss.hasPermission('waste:company-payment-voucher:confirm')")
    public CommonResult<Boolean> batchConfirm(@RequestParam("ids") List<Long> ids,
                                              @RequestParam("confirmType") String confirmType,
                                              @RequestParam("confirmBy") String confirmBy) {
        companyPaymentVoucherService.batchConfirm(ids, confirmType, confirmBy);
        return success(true);
    }

} 