package cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractAuditReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractCloseReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractSubmitReqVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.purchasecontract.PurchaseContractService;
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
 * 管理后台 - 采购合同（#45 / T07）。
 *
 * <p>合同是采购条款（「一个合同 → 多个采购订单 → 多次收货」），与自然人出售者的
 * 「框架收购协议」（开票前置）是两件事。审核通过前不得作为采购依据。
 */
@Tag(name = "管理后台 - 采购合同")
@RestController
@RequestMapping("/icbc/purchase-contract")
@Validated
public class PurchaseContractController {

    @Resource
    private PurchaseContractService purchaseContractService;

    @PostMapping("/create")
    @Operation(summary = "创建采购合同（草稿）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_CONTRACT_MANAGE + "')")
    public CommonResult<Long> create(@Valid @RequestBody PurchaseContractSaveReqVO createReqVO) {
        return success(purchaseContractService.createContract(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改采购合同（已生效合同的修改会新版本 + 回到待审核）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_CONTRACT_MANAGE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody PurchaseContractSaveReqVO updateReqVO) {
        purchaseContractService.updateContract(updateReqVO);
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "送审采购合同（落一版快照）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_CONTRACT_MANAGE + "')")
    public CommonResult<Boolean> submit(@Valid @RequestBody PurchaseContractSubmitReqVO submitReqVO) {
        purchaseContractService.submitForAudit(submitReqVO);
        return success(true);
    }

    @PostMapping("/audit")
    @Operation(summary = "审核采购合同（通过即生效，可作为采购依据）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_CONTRACT_AUDIT + "')")
    public CommonResult<Boolean> audit(@Valid @RequestBody PurchaseContractAuditReqVO auditReqVO) {
        purchaseContractService.audit(auditReqVO);
        return success(true);
    }

    @PostMapping("/close")
    @Operation(summary = "关闭采购合同")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_CONTRACT_MANAGE + "')")
    public CommonResult<Boolean> close(@Valid @RequestBody PurchaseContractCloseReqVO closeReqVO) {
        purchaseContractService.closeContract(closeReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购合同（只允许删除草稿）")
    @Parameter(name = "id", description = "合同编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_CONTRACT_MANAGE + "')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        purchaseContractService.deleteContract(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购合同详情（含适用品类与历史版本）")
    @Parameter(name = "id", description = "合同编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_CONTRACT_QUERY + "')")
    public CommonResult<PurchaseContractRespVO> get(@RequestParam("id") Long id) {
        return success(purchaseContractService.getDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购合同分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_CONTRACT_QUERY + "')")
    public CommonResult<PageResult<PurchaseContractRespVO>> page(@Valid PurchaseContractPageReqVO pageReqVO) {
        return success(purchaseContractService.getContractPage(pageReqVO));
    }

}
