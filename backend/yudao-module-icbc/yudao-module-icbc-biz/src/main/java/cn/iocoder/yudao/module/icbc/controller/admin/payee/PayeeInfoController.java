package cn.iocoder.yudao.module.icbc.controller.admin.payee;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.*;
import cn.iocoder.yudao.module.icbc.convert.payee.PayeeInfoConvert;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.service.payee.PayeeInfoService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工行收方信息")
@RestController
@RequestMapping("/icbc/payee-info")
@Validated
public class PayeeInfoController {

    @Resource
    private PayeeInfoService payeeInfoService;

    @PostMapping("/create")
    @Operation(summary = "创建收方信息")
    @PreAuthorize("@ss.hasPermission('icbc:payee-info:create')")
    public CommonResult<Long> createPayeeInfo(@Valid @RequestBody PayeeInfoSaveReqVO createReqVO) {
        return success(payeeInfoService.createPayeeInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新收方信息")
    @PreAuthorize("@ss.hasPermission('icbc:payee-info:update')")
    public CommonResult<Boolean> updatePayeeInfo(@Valid @RequestBody PayeeInfoSaveReqVO updateReqVO) {
        payeeInfoService.updatePayeeInfo(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除收方信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('icbc:payee-info:delete')")
    public CommonResult<Boolean> deletePayeeInfo(@RequestParam("id") Long id) {
        payeeInfoService.deletePayeeInfo(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得收方信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('icbc:payee-info:query')")
    public CommonResult<PayeeInfoRespVO> getPayeeInfo(@RequestParam("id") Long id) {
        PayeeInfoDO payeeInfo = payeeInfoService.getPayeeInfo(id);
        return success(BeanUtils.toBean(payeeInfo, PayeeInfoRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得收方信息分页")
    @PreAuthorize("@ss.hasPermission('icbc:payee-info:query')")
    public CommonResult<PageResult<PayeeInfoRespVO>> getPayeeInfoPage(@Valid PayeeInfoPageReqVO pageReqVO) {
        PageResult<PayeeInfoDO> pageResult = payeeInfoService.getPayeeInfoPage(pageReqVO);
        return success(PayeeInfoConvert.INSTANCE.convertPage(pageResult));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出收方信息 Excel")
    @PreAuthorize("@ss.hasPermission('icbc:payee-info:export')")
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
    @PreAuthorize("@ss.hasPermission('icbc:payee-info:create')")
    public CommonResult<Long> addPayeeToIcbc(@Valid @RequestBody PayeeAddReqVO reqVO) {
        return success(payeeInfoService.addPayeeToIcbc(reqVO));
    }

    @PostMapping("/reverse/receiver/query")
    @Operation(summary = "工行收方查询接口")
    @PreAuthorize("@ss.hasPermission('icbc:payee-info:query')")
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

} 