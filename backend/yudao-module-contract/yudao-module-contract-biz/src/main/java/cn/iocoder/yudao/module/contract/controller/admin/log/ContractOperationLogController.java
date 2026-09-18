package cn.iocoder.yudao.module.contract.controller.admin.log;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.log.vo.ContractOperationLogPageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.log.vo.ContractOperationLogRespVO;
import cn.iocoder.yudao.module.contract.convert.log.ContractOperationLogConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.log.ContractOperationLogDO;
import cn.iocoder.yudao.module.contract.service.log.ContractOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 合同操作日志")
@RestController
@RequestMapping("/contract/operation-log")
@Validated
public class ContractOperationLogController {

    @Resource
    private ContractOperationLogService contractOperationLogService;

    @GetMapping("/page")
    @Operation(summary = "获得合同操作日志分页")
    @PreAuthorize("@ss.hasPermission('contract:log:query')")
    public CommonResult<PageResult<ContractOperationLogRespVO>> getContractOperationLogPage(@Valid ContractOperationLogPageReqVO pageVO) {
        PageResult<ContractOperationLogDO> pageResult = contractOperationLogService.getContractOperationLogPage(pageVO);
        return success(ContractOperationLogConvert.INSTANCE.convertPage(pageResult));
    }

    @GetMapping("/list-by-contract")
    @Operation(summary = "获得合同操作日志列表")
    @Parameter(name = "contractId", description = "合同编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('contract:log:query')")
    public CommonResult<List<ContractOperationLogRespVO>> getContractOperationLogListByContractId(@RequestParam("contractId") Long contractId) {
        List<ContractOperationLogDO> list = contractOperationLogService.getContractOperationLogList(contractId);
        return success(ContractOperationLogConvert.INSTANCE.convertList(list));
    }

} 