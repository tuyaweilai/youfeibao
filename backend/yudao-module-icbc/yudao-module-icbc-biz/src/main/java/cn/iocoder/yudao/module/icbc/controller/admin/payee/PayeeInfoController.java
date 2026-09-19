package cn.iocoder.yudao.module.icbc.controller.admin.payee;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.*;
import cn.iocoder.yudao.module.icbc.convert.payee.PayeeInfoConvert;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.IcbcPayeeBankCardChangeDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.enums.PayeeBankCardChangeStatusEnum;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeBankCardChangeService;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工行收方信息")
@RestController
@RequestMapping("/icbc/payee-info")
@Validated
public class PayeeInfoController {

    @Resource
    private PayeeInfoService payeeInfoService;

    @Resource
    private PayeeBankCardChangeService payeeBankCardChangeService;

    @PostMapping("/create")
    @Operation(summary = "创建收方信息")
    @PreAuthorize("@icbc.hasPermission('icbc:payee-info:create')")
    public CommonResult<Long> createPayeeInfo(@Valid @RequestBody PayeeInfoSaveReqVO createReqVO) {
        return success(payeeInfoService.createPayeeInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新收方信息")
    @PreAuthorize("@icbc.hasPermission('icbc:payee-info:update')")
    public CommonResult<Boolean> updatePayeeInfo(@Valid @RequestBody PayeeInfoSaveReqVO updateReqVO) {
        payeeInfoService.updatePayeeInfo(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除收方信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@icbc.hasPermission('icbc:payee-info:delete')")
    public CommonResult<Boolean> deletePayeeInfo(@RequestParam("id") Long id) {
        payeeInfoService.deletePayeeInfo(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得收方信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@icbc.hasPermission('icbc:payee-info:query')")
    public CommonResult<PayeeInfoRespVO> getPayeeInfo(@RequestParam("id") Long id) {
        PayeeInfoDO payeeInfo = payeeInfoService.getPayeeInfo(id);
        PayeeInfoRespVO respVO = BeanUtils.toBean(payeeInfo, PayeeInfoRespVO.class);
        enrichBankCardChange(List.of(respVO));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得收方信息分页")
    @PreAuthorize("@icbc.hasPermission('icbc:payee-info:query')")
    public CommonResult<PageResult<PayeeInfoRespVO>> getPayeeInfoPage(@Valid PayeeInfoPageReqVO pageReqVO) {
        PageResult<PayeeInfoDO> pageResult = payeeInfoService.getPayeeInfoPage(pageReqVO);
        PageResult<PayeeInfoRespVO> result = PayeeInfoConvert.INSTANCE.convertPage(pageResult);
        enrichBankCardChange(result.getList());
        return success(result);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出收方信息 Excel")
    @PreAuthorize("@icbc.hasPermission('icbc:payee-info:export')")
    public void exportPayeeInfoExcel(@Valid PayeeInfoPageReqVO exportReqVO,
              HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<PayeeInfoDO> list = payeeInfoService.getPayeeInfoList(exportReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "收方信息.xls", "数据", PayeeInfoRespVO.class,
                        PayeeInfoConvert.INSTANCE.convertList(list));
    }

    // ==================== 工行接口相关 ====================

    @PostMapping("/reverse/receiver/add")
    @Operation(summary = "工行收方新增接口")
    @PreAuthorize("@icbc.hasPermission('icbc:payee-info:create')")
    public CommonResult<Long> addPayeeToIcbc(@Valid @RequestBody PayeeAddReqVO reqVO) {
        return success(payeeInfoService.addPayeeToIcbc(reqVO));
    }

    @PostMapping("/reverse/receiver/query")
    @Operation(summary = "工行收方查询接口")
    @PreAuthorize("@icbc.hasPermission('icbc:payee-info:query')")
    public CommonResult<List<PayeeInfoRespVO>> queryPayeeFromIcbc(@Valid @RequestBody PayeeQueryReqVO reqVO) {
        List<PayeeInfoDO> list = payeeInfoService.queryPayeeFromIcbc(reqVO);
        return success(PayeeInfoConvert.INSTANCE.convertList(list));
    }

    @PostMapping("/callback/audit")
    @Operation(summary = "工行收方审核回调")
    public CommonResult<Boolean> handlePayeeAuditCallback(@RequestParam("outUserId") String outUserId,
                                                          @RequestParam("auditStatus") String auditStatus,
                                                          @RequestParam("auditMsg") String auditMsg,
                                                          @RequestParam("icbcMediumId") String icbcMediumId) {
        payeeInfoService.handlePayeeAuditCallback(outUserId, auditStatus, auditMsg, icbcMediumId);
        return success(true);
    }

    // ==================== 收款账户变更（换银行卡，#37） ====================

    @GetMapping("/bank-card-change/list")
    @Operation(summary = "获得某出售者的收款账户变更记录（倒序）")
    @Parameter(name = "payeeId", description = "收方（出售者）档案编号", required = true)
    @PreAuthorize("@icbc.hasPermission('icbc:payee-info:query')")
    public CommonResult<List<PayeeBankCardChangeRespVO>> getBankCardChangeList(
            @RequestParam("payeeId") Long payeeId) {
        return success(payeeBankCardChangeService.listByPayeeId(payeeId).stream()
                .map(PayeeInfoController::toChangeRespVO).collect(Collectors.toList()));
    }

    @PostMapping("/bank-card-change/cancel")
    @Operation(summary = "取消在途的收款账户变更",
            description = "企业侧人工清障：审核中的变更取消后，原卡继续有效，付款随即恢复")
    @PreAuthorize("@icbc.hasPermission('icbc:payee-info:update')")
    public CommonResult<Boolean> cancelBankCardChange(@RequestParam("changeId") Long changeId,
                                                     @RequestParam(value = "reason", required = false) String reason) {
        payeeBankCardChangeService.cancelChange(changeId, reason);
        return success(true);
    }

    /**
     * 把在途变更的可见信息贴到收方列表 / 详情上：企业侧要一眼看到「收款账户变更中」。
     */
    private void enrichBankCardChange(List<PayeeInfoRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, IcbcPayeeBankCardChangeDO> pending = payeeBankCardChangeService.pendingMap(
                list.stream().map(PayeeInfoRespVO::getId).filter(Objects::nonNull)
                        .collect(Collectors.toList()));
        if (pending.isEmpty()) {
            return;
        }
        for (PayeeInfoRespVO resp : list) {
            IcbcPayeeBankCardChangeDO change = pending.get(resp.getId());
            if (change == null) {
                continue;
            }
            resp.setBankCardChangeStatus(change.getStatus());
            resp.setBankCardChangeStatusName(PayeeBankCardChangeStatusEnum.nameOf(change.getStatus()));
            resp.setBankCardChangeNewCardTail(MaskUtils.cardTail(change.getNewBankCardNo()));
            resp.setBankCardChangeRequestedAt(change.getRequestedAt());
        }
    }

    private static PayeeBankCardChangeRespVO toChangeRespVO(IcbcPayeeBankCardChangeDO change) {
        PayeeBankCardChangeRespVO resp = new PayeeBankCardChangeRespVO();
        resp.setId(change.getId());
        resp.setChangeNo(change.getChangeNo());
        resp.setPayeeId(change.getPayeeId());
        resp.setNaturalPersonId(change.getNaturalPersonId());
        resp.setStatus(change.getStatus());
        resp.setStatusName(PayeeBankCardChangeStatusEnum.nameOf(change.getStatus()));
        resp.setOldCardTail(change.getOldCardTail());
        resp.setNewCardTail(MaskUtils.cardTail(change.getNewBankCardNo()));
        resp.setNewBankName(change.getNewBankName());
        resp.setIcbcOpenacctStatus(change.getIcbcOpenacctStatus());
        resp.setAuditResult(change.getAuditResult());
        resp.setRejectReason(change.getRejectReason());
        resp.setRequestSource(change.getRequestSource());
        resp.setRequestedAt(change.getRequestedAt());
        resp.setResolvedAt(change.getResolvedAt());
        resp.setRemark(change.getRemark());
        resp.setCreateTime(change.getCreateTime());
        return resp;
    }

} 
