package cn.iocoder.yudao.module.icbc.controller.app.settlement;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo.SettlementRespVO;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerInvoiceConfirmItemVO;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerSettlementConfirmReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerSettlementDisputeReqVO;
import cn.iocoder.yudao.module.icbc.service.invoice.AutoInvoiceApplicationService;
import cn.iocoder.yudao.module.icbc.service.settlement.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getUserAgent;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 自然人出售者 - 结算确认（#33，ADR 0018 / 0022 / 0024）。
 *
 * <p>勾选即确认，留痕时间 / IP / 设备 / 该版快照哈希；有异议则选固定原因枚举。不做聊天、不做工单。
 * 必须显式带 {@code naturalPersonId}：同一登录可代多个主体操作，不做静默推断。
 */
@Tag(name = "自然人出售者 - 结算确认")
@RestController
@RequestMapping("/icbc/seller/settlement")
@Validated
@Slf4j
public class SellerSettlementController {

    @Resource
    private SettlementService settlementService;
    @Resource
    private AutoInvoiceApplicationService autoInvoiceApplicationService;

    @GetMapping("/list")
    @Operation(summary = "待我确认 / 我的结算单", description = "按自然人主体返回，含待确认、已确认、有异议与历史")
    public CommonResult<List<SettlementRespVO>> list(@RequestParam("naturalPersonId") Long naturalPersonId) {
        return success(settlementService.getListForSeller(naturalPersonId));
    }

    @GetMapping("/get")
    @Operation(summary = "结算单明细（含当前版本逐条收购单）")
    public CommonResult<SettlementRespVO> get(@RequestParam("naturalPersonId") Long naturalPersonId,
                                              @RequestParam("id") Long id) {
        return success(settlementService.getDetailForSeller(naturalPersonId, id));
    }

    @PostMapping("/confirm")
    @Operation(summary = "确认结算", description = "勾选即确认；留痕时间 / IP / 设备 / 该版快照哈希；确认后自动逐张推去开票预下单")
    public CommonResult<Boolean> confirm(@Valid @RequestBody SellerSettlementConfirmReqVO reqVO) {
        settlementService.confirm(reqVO, getClientIP(), getUserAgent());
        // 确认之后自动把这一批逐张推去预下单（#106 / ADR 0039）：失败不回滚已经落库的确认，
        // 具体原因与补齐方式由 /invoice-status 逐张给他（ADR 0038 的「谁的事谁看得到」）
        try {
            autoInvoiceApplicationService.applyForSettlement(reqVO.getSettlementId());
        } catch (RuntimeException e) {
            log.warn("结算确认后自动预下单失败 - settlementId: {}", reqVO.getSettlementId(), e);
        }
        return success(true);
    }

    @GetMapping("/invoice-status")
    @Operation(summary = "这一批里每一张开票信息确认的进度",
            description = "结算确认后逐张给出「待你在工行页面确认 / 已确认 / 还不能发起（附原因与补齐方式）」，含可重开的确认页地址")
    public CommonResult<List<SellerInvoiceConfirmItemVO>> invoiceStatus(
            @RequestParam("naturalPersonId") Long naturalPersonId,
            @RequestParam("id") Long id) {
        // 归属校验不静默推断：不是他的结算单就直接拒（与 /get 同一条规矩）
        settlementService.getDetailForSeller(naturalPersonId, id);
        return success(autoInvoiceApplicationService.statusForSettlement(id));
    }

    @PostMapping("/dispute")
    @Operation(summary = "有异议", description = "固定原因枚举 + 说明；选「其他」必须附说明")
    public CommonResult<Boolean> dispute(@Valid @RequestBody SellerSettlementDisputeReqVO reqVO) {
        settlementService.raiseDispute(reqVO);
        return success(true);
    }

}
