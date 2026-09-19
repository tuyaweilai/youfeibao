package cn.iocoder.yudao.module.icbc.controller.admin.settlement;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo.*;
import cn.iocoder.yudao.module.icbc.service.settlement.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 结算单（#33，ADR 0018 / 0022）。
 *
 * <p>收货员在这里「结束本次收货」生成结算单，企业对异议做「改」或「不改但附说明」两个动作，
 * 或走线下签字逃生门。确认与异议的表达在自然人端。
 */
@Tag(name = "管理后台 - 结算单")
@RestController
@RequestMapping("/icbc/settlement")
@Validated
@Slf4j
public class SettlementConfirmController {

    @Resource
    private SettlementService settlementService;

    @GetMapping("/page")
    @Operation(summary = "分页查询结算单")
    @PreAuthorize("@icbc.hasPermission('icbc:settlement-confirm:query')")
    public CommonResult<PageResult<SettlementRespVO>> getPage(@Valid SettlementPageReqVO pageReqVO) {
        return success(settlementService.getPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得结算单明细（含当前版本与逐条收购单）")
    @PreAuthorize("@icbc.hasPermission('icbc:settlement-confirm:query')")
    public CommonResult<SettlementRespVO> get(@RequestParam("id") Long id) {
        return success(settlementService.getDetail(id));
    }

    @GetMapping("/list-by-payee")
    @Operation(summary = "某个出售者的全部结算单")
    @PreAuthorize("@icbc.hasPermission('icbc:settlement-confirm:query')")
    public CommonResult<List<SettlementRespVO>> listByPayee(@RequestParam("payeeId") Long payeeId) {
        return success(settlementService.getListByPayeeId(payeeId));
    }

    @PostMapping("/generate")
    @Operation(summary = "结束本次收货，生成结算单",
            description = "聚合同一出售者尚未归组的收购单；生成后不得再往里加收购单，要加只能新建")
    @PreAuthorize("@icbc.hasPermission('icbc:settlement-confirm:manage')")
    public CommonResult<Long> generate(@Valid @RequestBody SettlementGenerateReqVO reqVO) {
        return success(settlementService.generate(reqVO));
    }

    @PostMapping("/change")
    @Operation(summary = "对异议的答复：改（新版本 + 原因，回到待确认）")
    @PreAuthorize("@icbc.hasPermission('icbc:settlement-confirm:manage')")
    public CommonResult<Boolean> change(@Valid @RequestBody SettlementChangeReqVO reqVO) {
        settlementService.changeByEnterprise(reqVO);
        return success(true);
    }

    @PostMapping("/reply")
    @Operation(summary = "对异议的答复：不改但附说明（回到待确认）")
    @PreAuthorize("@icbc.hasPermission('icbc:settlement-confirm:manage')")
    public CommonResult<Boolean> reply(@Valid @RequestBody SettlementReplyReqVO reqVO) {
        settlementService.replyNoChange(reqVO);
        return success(true);
    }

    @PostMapping("/offline-sign")
    @Operation(summary = "线下签字确认", description = "上传带签字的纸质确认书 + 办理人，等价于确认")
    @PreAuthorize("@icbc.hasPermission('icbc:settlement-confirm:manage')")
    public CommonResult<Boolean> offlineSign(@Valid @RequestBody SettlementOfflineSignReqVO reqVO) {
        settlementService.offlineSign(reqVO);
        return success(true);
    }

    @PostMapping("/cancel-acquisition")
    @Operation(summary = "作废未开票的收购单", description = "必须留原因，对自然人可见，作废后不可再开票付款")
    @PreAuthorize("@icbc.hasPermission('icbc:settlement-confirm:manage')")
    public CommonResult<Boolean> cancelAcquisition(@Valid @RequestBody SettlementAcquisitionCancelReqVO reqVO) {
        settlementService.cancelAcquisition(reqVO);
        return success(true);
    }

}
