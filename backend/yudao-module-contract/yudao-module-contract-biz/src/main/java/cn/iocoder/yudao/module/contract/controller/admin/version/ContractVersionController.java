package cn.iocoder.yudao.module.contract.controller.admin.version;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.version.vo.*;
import cn.iocoder.yudao.module.contract.convert.version.ContractVersionConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.version.ContractVersionDO;
import cn.iocoder.yudao.module.contract.service.version.ContractVersionService;
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

@Tag(name = "管理后台 - 合同版本")
@RestController
@RequestMapping("/contract/version")
@Validated
public class ContractVersionController {

    @Resource
    private ContractVersionService contractVersionService;

    @PostMapping("/create")
    @Operation(summary = "创建合同版本")
    @PreAuthorize("@ss.hasPermission('contract:version:create')")
    public CommonResult<Long> createContractVersion(@Valid @RequestBody ContractVersionCreateReqVO createReqVO) {
        return success(contractVersionService.createContractVersion(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新合同版本")
    @PreAuthorize("@ss.hasPermission('contract:version:update')")
    public CommonResult<Boolean> updateContractVersion(@Valid @RequestBody ContractVersionUpdateReqVO updateReqVO) {
        contractVersionService.updateContractVersion(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合同版本")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:version:delete')")
    public CommonResult<Boolean> deleteContractVersion(@RequestParam("id") Long id) {
        contractVersionService.deleteContractVersion(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得合同版本")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('contract:version:query')")
    public CommonResult<ContractVersionRespVO> getContractVersion(@RequestParam("id") Long id) {
        ContractVersionDO contractVersion = contractVersionService.getContractVersion(id);
        return success(ContractVersionConvert.INSTANCE.convert(contractVersion));
    }

    @GetMapping("/page")
    @Operation(summary = "获得合同版本分页")
    @PreAuthorize("@ss.hasPermission('contract:version:query')")
    public CommonResult<PageResult<ContractVersionRespVO>> getContractVersionPage(@Valid ContractVersionPageReqVO pageVO) {
        PageResult<ContractVersionDO> pageResult = contractVersionService.getContractVersionPage(pageVO);
        return success(ContractVersionConvert.INSTANCE.convertPage(pageResult));
    }

    @GetMapping("/list-by-contract")
    @Operation(summary = "根据合同获取版本列表")
    @Parameter(name = "contractId", description = "合同编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:version:query')")
    public CommonResult<List<ContractVersionRespVO>> getContractVersionListByContractId(@RequestParam("contractId") Long contractId) {
        List<ContractVersionDO> list = contractVersionService.getContractVersionListByContractId(contractId);
        return success(ContractVersionConvert.INSTANCE.convertList(list));
    }

    @PutMapping("/set-current")
    @Operation(summary = "设置当前版本")
    @PreAuthorize("@ss.hasPermission('contract:version:update')")
    public CommonResult<Boolean> setCurrentVersion(@RequestParam("contractId") Long contractId, 
                                                   @RequestParam("versionId") Long versionId) {
        contractVersionService.setCurrentVersion(contractId, versionId);
        return success(true);
    }

} 