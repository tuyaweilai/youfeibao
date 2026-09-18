package cn.iocoder.yudao.module.contract.controller.admin.party;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.party.vo.*;
import cn.iocoder.yudao.module.contract.convert.party.ContractPartyConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.party.ContractPartyDO;
import cn.iocoder.yudao.module.contract.service.party.ContractPartyService;
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

@Tag(name = "管理后台 - 合同参与方")
@RestController
@RequestMapping("/contract/party")
@Validated
public class ContractPartyController {

    @Resource
    private ContractPartyService contractPartyService;

    @PostMapping("/create")
    @Operation(summary = "创建合同参与方")
    @PreAuthorize("@ss.hasPermission('contract:party:create')")
    public CommonResult<Long> createContractParty(@Valid @RequestBody ContractPartyCreateReqVO createReqVO) {
        return success(contractPartyService.createContractParty(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新合同参与方")
    @PreAuthorize("@ss.hasPermission('contract:party:update')")
    public CommonResult<Boolean> updateContractParty(@Valid @RequestBody ContractPartyUpdateReqVO updateReqVO) {
        contractPartyService.updateContractParty(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合同参与方")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:party:delete')")
    public CommonResult<Boolean> deleteContractParty(@RequestParam("id") Long id) {
        contractPartyService.deleteContractParty(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得合同参与方")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('contract:party:query')")
    public CommonResult<ContractPartyRespVO> getContractParty(@RequestParam("id") Long id) {
        ContractPartyDO contractParty = contractPartyService.getContractParty(id);
        return success(ContractPartyConvert.INSTANCE.convert(contractParty));
    }

    @GetMapping("/page")
    @Operation(summary = "获得合同参与方分页")
    @PreAuthorize("@ss.hasPermission('contract:party:query')")
    public CommonResult<PageResult<ContractPartyRespVO>> getContractPartyPage(@Valid ContractPartyPageReqVO pageVO) {
        PageResult<ContractPartyDO> pageResult = contractPartyService.getContractPartyPage(pageVO);
        return success(ContractPartyConvert.INSTANCE.convertPage(pageResult));
    }

    @GetMapping("/list-by-contract")
    @Operation(summary = "根据合同ID获取参与方列表")
    @Parameter(name = "contractId", description = "合同ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('contract:party:query')")
    public CommonResult<List<ContractPartyRespVO>> getContractPartyListByContractId(@RequestParam("contractId") Long contractId) {
        List<ContractPartyDO> list = contractPartyService.getContractPartyListByContractId(contractId);
        return success(ContractPartyConvert.INSTANCE.convertList(list));
    }

    @PostMapping("/update-sign-status")
    @Operation(summary = "更新签署状态")
    @PreAuthorize("@ss.hasPermission('contract:party:update')")
    public CommonResult<Boolean> updateSignStatus(@RequestParam("id") Long id,
                                                @RequestParam("signStatus") Integer signStatus,
                                                @RequestParam(value = "signIp", required = false) String signIp) {
        contractPartyService.updateSignStatus(id, signStatus, signIp);
        return success(true);
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建合同参与方")
    @PreAuthorize("@ss.hasPermission('contract:party:create')")
    public CommonResult<List<Long>> batchCreateContractParties(
            @RequestParam("contractId") Long contractId,
            @Valid @RequestBody List<ContractPartyCreateReqVO> parties) {
        return success(contractPartyService.batchCreateContractParties(contractId, parties));
    }
} 