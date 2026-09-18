package cn.iocoder.yudao.module.icbc.controller.admin.payer;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.*;
import cn.iocoder.yudao.module.icbc.convert.payer.PayerInfoConvert;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.service.payer.PayerInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 工行付方信息
 */
@Tag(name = "管理后台 - 工行付方信息")
@RestController
@RequestMapping("/icbc/payer-info")
@Validated
public class PayerInfoController {

    @Resource
    private PayerInfoService payerInfoService;

    @PostMapping("/create")
    @Operation(summary = "创建付方信息")
    @PreAuthorize("@ss.hasPermission('icbc:payer-info:create')")
    public CommonResult<Long> createPayerInfo(@Valid @RequestBody PayerInfoSaveReqVO createReqVO) {
        return success(payerInfoService.createPayerInfo(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新付方信息")
    @PreAuthorize("@ss.hasPermission('icbc:payer-info:update')")
    public CommonResult<Boolean> updatePayerInfo(@Valid @RequestBody PayerInfoSaveReqVO updateReqVO) {
        payerInfoService.updatePayerInfo(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除付方信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('icbc:payer-info:delete')")
    public CommonResult<Boolean> deletePayerInfo(@RequestParam("id") Long id) {
        payerInfoService.deletePayerInfo(id);
        return success(true);
    }

    @DeleteMapping("/delete-batch")
    @Operation(summary = "批量删除付方信息")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('icbc:payer-info:delete')")
    public CommonResult<Boolean> deletePayerInfos(@RequestParam("ids") Collection<Long> ids) {
        payerInfoService.deletePayerInfos(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得付方信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('icbc:payer-info:query')")
    public CommonResult<PayerInfoRespVO> getPayerInfo(@RequestParam("id") Long id) {
        PayerInfoDO payerInfo = payerInfoService.getPayerInfo(id);
        return success(PayerInfoConvert.INSTANCE.convert(payerInfo));
    }

    @GetMapping("/get-by-credit-code")
    @Operation(summary = "根据统一社会信用代码获得付方信息")
    @Parameter(name = "creditCode", description = "统一社会信用代码", required = true, example = "91110105MA01R2278M")
    @PreAuthorize("@ss.hasPermission('icbc:payer-info:query')")
    public CommonResult<PayerInfoRespVO> getPayerInfoByCreditCode(@RequestParam("creditCode") String creditCode) {
        PayerInfoDO payerInfo = payerInfoService.getPayerInfoByCreditCode(creditCode);
        return success(PayerInfoConvert.INSTANCE.convert(payerInfo));
    }

    @GetMapping("/list")
    @Operation(summary = "获得付方信息列表")
    @Parameter(name = "ids", description = "编号列表", required = true, example = "1024,2048")
    @PreAuthorize("@ss.hasPermission('icbc:payer-info:query')")
    public CommonResult<List<PayerInfoRespVO>> getPayerInfoList(@RequestParam("ids") Collection<Long> ids) {
        List<PayerInfoDO> list = payerInfoService.getPayerInfoList(ids);
        return success(PayerInfoConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/page")
    @Operation(summary = "获得付方信息分页")
    @PreAuthorize("@ss.hasPermission('icbc:payer-info:query')")
    public CommonResult<PageResult<PayerInfoRespVO>> getPayerInfoPage(@Valid PayerInfoPageReqVO pageVO) {
        PageResult<PayerInfoDO> pageResult = payerInfoService.getPayerInfoPage(pageVO);
        return success(PayerInfoConvert.INSTANCE.convertPage(pageResult));
    }

    @PostMapping("/reverse/payer/add")
    @Operation(summary = "工行付方新增接口")
    @PreAuthorize("@ss.hasPermission('icbc:payer-info:create')")
    public CommonResult<Long> addPayerToIcbc(@Valid @RequestBody PayerAddReqVO reqVO) {
        return success(payerInfoService.addPayerToIcbc(reqVO));
    }

    @PostMapping("/reverse/payer/query")
    @Operation(summary = "工行付方查询接口")
    @PreAuthorize("@ss.hasPermission('icbc:payer-info:query')")
    public CommonResult<PayerInfoRespVO> queryPayerFromIcbc(@Valid @RequestBody PayerQueryReqVO reqVO) {
        PayerInfoDO payerInfo = payerInfoService.queryPayerFromIcbc(reqVO);
        return success(PayerInfoConvert.INSTANCE.convert(payerInfo));
    }

    /**
     * 付方审核结果回调接口
     * 
     * 注意：实际项目中，此接口应该作为回调接口，供工行调用，不需要权限校验
     * 为了简化开发，这里使用普通接口模拟
     */
    @PostMapping("/callback/payer-audit")
    @Operation(summary = "付方审核结果回调")
    public CommonResult<Boolean> handlePayerAuditCallback(@RequestParam("payerNo") String payerNo,
                                                        @RequestParam("status") Integer status,
                                                        @RequestParam("auditMsg") String auditMsg,
                                                        @RequestParam("payerStatus") String payerStatus) {
        payerInfoService.handlePayerAuditCallback(payerNo, status, auditMsg, payerStatus);
        return success(true);
    }
} 