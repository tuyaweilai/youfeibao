package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionReviewReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionSaveReqVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.PurchaseOrderExceptionService;
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
 * 管理后台 - 采购订单履约异常授权（#47 T09）。
 *
 * <p>超量 / 过期 / 跨场站交货被企业配置成「提交授权审核」时，由现场或采购经办提交、由管理员审核。
 * 授权只放宽被授权的那一件事，不改订单状态、不改已发生的业务。
 */
@Tag(name = "管理后台 - 采购订单履约异常授权")
@RestController
@RequestMapping("/icbc/purchase-order-exception")
@Validated
public class PurchaseOrderExceptionController {

    @Resource
    private PurchaseOrderExceptionService purchaseOrderExceptionService;

    @PostMapping("/request")
    @Operation(summary = "提交履约异常授权审核（超量 / 过期 / 跨场站交货）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_EXCEPTION_REQUEST + "')")
    public CommonResult<Long> request(@Valid @RequestBody PurchaseOrderExceptionSaveReqVO reqVO) {
        return success(purchaseOrderExceptionService.requestException(reqVO));
    }

    @PostMapping("/review")
    @Operation(summary = "审核履约异常授权单（通过 / 拒绝）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_EXCEPTION_AUDIT + "')")
    public CommonResult<Boolean> review(@Valid @RequestBody PurchaseOrderExceptionReviewReqVO reqVO) {
        purchaseOrderExceptionService.reviewException(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得履约异常授权单")
    @Parameter(name = "id", description = "授权单编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_EXCEPTION_QUERY + "')")
    public CommonResult<PurchaseOrderExceptionRespVO> get(@RequestParam("id") Long id) {
        return success(purchaseOrderExceptionService.getException(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得履约异常授权单分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_EXCEPTION_QUERY + "')")
    public CommonResult<PageResult<PurchaseOrderExceptionRespVO>> page(
            @Valid PurchaseOrderExceptionPageReqVO reqVO) {
        return success(purchaseOrderExceptionService.getExceptionPage(reqVO));
    }

}
