package cn.iocoder.yudao.module.icbc.controller.admin.handover;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchIntakeReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchUpdateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingAddReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingEffectiveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverIntakeCandidateRespVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.handover.HandoverBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 / 收货员现场端 - 交接批次与有效磅次（#50 T12）。
 *
 * <p>一个交易对方的一次物理交接记为一个交接批次；过磅保留每一次原始读数，只有被选定的那一次
 * 参与计量，其余留档不参与。**同一车同一天两次送货是两个批次**，不做去重。
 * 现场端（uni-app）与 PC 共用本组端点：现场端为主，PC 可查改补录。
 */
@Tag(name = "管理后台 - 交接批次与有效磅次")
@RestController
@RequestMapping("/icbc/handover-batch")
@Validated
public class IcbcHandoverBatchController {

    @Resource
    private HandoverBatchService handoverBatchService;

    @PostMapping("/create")
    @Operation(summary = "登记交接批次", description = "登记交易对方、场站或上门地址、时间、来源方式、司机与车牌；预约与采购订单都不是必要条件")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.HANDOVER_BATCH_MANAGE + "')")
    public CommonResult<Long> createBatch(@Valid @RequestBody HandoverBatchCreateReqVO reqVO) {
        return success(handoverBatchService.createBatch(reqVO));
    }

    @PostMapping("/intake")
    @Operation(summary = "按现场交接登记回场复磅（上门提货）",
            description = "磅房按司机现场登记的交接建批次并录第一次过磅；把现场参考量 / 参考单价、要件状态、司机与车辆搬进批次；**场站必填且是派单场站**；同一现场交接登记幂等")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.HANDOVER_BATCH_MANAGE + "')")
    public CommonResult<Long> intakeFromHandover(@Valid @RequestBody HandoverBatchIntakeReqVO reqVO) {
        return success(handoverBatchService.intakeFromHandover(reqVO));
    }

    @GetMapping("/pending-intake-list")
    @Operation(summary = "待回场复磅的现场交接登记",
            description = "物流侧最近登记的交接里还没建过批次的那些；带现场参考量与照片凭证，供磅房对得上现场谈的事")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.HANDOVER_BATCH_QUERY + "')")
    public CommonResult<List<HandoverIntakeCandidateRespVO>> getPendingIntakeList() {
        return success(handoverBatchService.getPendingIntakeList());
    }

    @PutMapping("/update")
    @Operation(summary = "补录交接批次", description = "补录司机、车牌、地点、时间、来源方式；交易对方不在这里改")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.HANDOVER_BATCH_MANAGE + "')")
    public CommonResult<Boolean> updateBatch(@Valid @RequestBody HandoverBatchUpdateReqVO reqVO) {
        handoverBatchService.updateBatch(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得交接批次详情（含全部磅次与有效磅次）")
    @Parameter(name = "id", description = "批次编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.HANDOVER_BATCH_QUERY + "')")
    public CommonResult<HandoverBatchRespVO> getBatch(@RequestParam("id") Long id) {
        return success(handoverBatchService.getBatch(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页获得交接批次")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.HANDOVER_BATCH_QUERY + "')")
    public CommonResult<PageResult<HandoverBatchRespVO>> getPage(@Valid HandoverBatchPageReqVO reqVO) {
        return success(handoverBatchService.getPage(reqVO));
    }

    @GetMapping("/weighings")
    @Operation(summary = "获得某批次的全部磅次", description = "含留档不参与的那些；有效磅次至多一条")
    @Parameter(name = "batchId", description = "批次编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.HANDOVER_BATCH_QUERY + "')")
    public CommonResult<List<HandoverWeighingRespVO>> listWeighings(@RequestParam("batchId") Long batchId) {
        return success(handoverBatchService.listWeighings(batchId));
    }

    @PostMapping("/weighing/add")
    @Operation(summary = "新增一次磅次", description = "复磅 / 拆装 / 重称都走这里；第一次磅次自动成为有效磅次")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.HANDOVER_BATCH_MANAGE + "')")
    public CommonResult<Long> addWeighing(@Valid @RequestBody HandoverWeighingAddReqVO reqVO) {
        return success(handoverBatchService.addWeighing(reqVO));
    }

    @PostMapping("/weighing/effective")
    @Operation(summary = "指定有效磅次", description = "只有被选定的那一次参与计量；该批次已产生收购单则不能再改")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.HANDOVER_BATCH_MANAGE + "')")
    public CommonResult<Boolean> selectEffectiveWeighing(
            @Valid @RequestBody HandoverWeighingEffectiveReqVO reqVO) {
        handoverBatchService.selectEffectiveWeighing(reqVO);
        return success(true);
    }

}
