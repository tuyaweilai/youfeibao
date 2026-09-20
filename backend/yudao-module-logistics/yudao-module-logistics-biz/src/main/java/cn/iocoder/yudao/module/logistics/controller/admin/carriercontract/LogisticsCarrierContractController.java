package cn.iocoder.yudao.module.logistics.controller.admin.carriercontract;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractRespVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractSaveReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractSurchargeVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierContractDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBillingModeEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.service.carrier.LogisticsCarrierContractService;
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
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 承运合同（V8 #75）。
 *
 * <p>运价与计费方式的约定，是运费对账的依据。停用而不是删除：历史运单上的合同号与运价快照要留着。
 */
@Tag(name = "管理后台 - 承运合同")
@RestController
@RequestMapping("/logistics/carrier-contract")
@Validated
public class LogisticsCarrierContractController {

    @Resource
    private LogisticsCarrierContractService logisticsCarrierContractService;

    @PostMapping("/create")
    @Operation(summary = "创建承运合同")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_CONTRACT_CREATE + "')")
    public CommonResult<Long> create(@Valid @RequestBody LogisticsCarrierContractSaveReqVO createReqVO) {
        return success(logisticsCarrierContractService.createCarrierContract(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新承运合同")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_CONTRACT_UPDATE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody LogisticsCarrierContractSaveReqVO updateReqVO) {
        logisticsCarrierContractService.updateCarrierContract(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除承运合同")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_CONTRACT_DELETE + "')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        logisticsCarrierContractService.deleteCarrierContract(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得承运合同")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_CONTRACT_QUERY + "')")
    public CommonResult<LogisticsCarrierContractRespVO> get(@RequestParam("id") Long id) {
        return success(toResp(logisticsCarrierContractService.getCarrierContract(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得承运合同分页")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_CONTRACT_QUERY + "')")
    public CommonResult<PageResult<LogisticsCarrierContractRespVO>> page(
            @Valid LogisticsCarrierContractPageReqVO pageReqVO) {
        PageResult<LogisticsCarrierContractDO> page = logisticsCarrierContractService
                .getCarrierContractPage(pageReqVO);
        return success(new PageResult<>(toRespList(page.getList()), page.getTotal()));
    }

    @GetMapping("/list-by-carrier")
    @Operation(summary = "按承运商取承运合同（运费汇集选合同用）")
    @Parameter(name = "carrierId", description = "承运商编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_CONTRACT_QUERY + "')")
    public CommonResult<List<LogisticsCarrierContractRespVO>> listByCarrier(@RequestParam("carrierId") Long carrierId) {
        return success(toRespList(logisticsCarrierContractService.getContractListByCarrierId(carrierId)));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出承运合同 Excel")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.CARRIER_CONTRACT_EXPORT + "')")
    public void exportExcel(@Valid LogisticsCarrierContractPageReqVO exportReqVO,
                            HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LogisticsCarrierContractDO> list = logisticsCarrierContractService.getCarrierContractList(exportReqVO);
        ExcelUtils.write(response, "承运合同.xls", "数据", LogisticsCarrierContractRespVO.class, toRespList(list));
    }

    private List<LogisticsCarrierContractRespVO> toRespList(List<LogisticsCarrierContractDO> contracts) {
        return contracts.stream().map(this::toResp).collect(Collectors.toList());
    }

    private LogisticsCarrierContractRespVO toResp(LogisticsCarrierContractDO contract) {
        LogisticsCarrierContractRespVO resp = BeanUtils.toBean(contract, LogisticsCarrierContractRespVO.class);
        resp.setBillingModeName(LogisticsFreightBillingModeEnum.nameOf(contract.getBillingMode()));
        resp.setStatusName(CommonStatusEnum.isEnable(contract.getStatus()) ? "生效" : "已停用");
        if (StrUtil.isNotBlank(contract.getSurcharges())) {
            resp.setSurcharges(JsonUtils.parseArray(contract.getSurcharges(),
                    LogisticsCarrierContractSurchargeVO.class));
        }
        return resp;
    }

}
