package cn.iocoder.yudao.module.icbc.controller.app.settlement;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo.SettlementRespVO;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerSettlementConfirmReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerSettlementDisputeReqVO;
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
    @Operation(summary = "确认结算", description = "勾选即确认；留痕时间 / IP / 设备 / 该版快照哈希")
    public CommonResult<Boolean> confirm(@Valid @RequestBody SellerSettlementConfirmReqVO reqVO) {
        settlementService.confirm(reqVO, getClientIP(), getUserAgent());
        return success(true);
    }

    @PostMapping("/dispute")
    @Operation(summary = "有异议", description = "固定原因枚举 + 说明；选「其他」必须附说明")
    public CommonResult<Boolean> dispute(@Valid @RequestBody SellerSettlementDisputeReqVO reqVO) {
        settlementService.raiseDispute(reqVO);
        return success(true);
    }

}
