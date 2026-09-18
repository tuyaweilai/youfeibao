package cn.iocoder.yudao.module.contract.controller.admin.contract;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.contract.vo.*;
import cn.iocoder.yudao.module.contract.convert.contract.ContractConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.contract.ContractDO;
import cn.iocoder.yudao.module.contract.dal.dataobject.type.ContractTypeDO;
import cn.iocoder.yudao.module.contract.service.contract.ContractService;
import cn.iocoder.yudao.module.contract.service.type.ContractTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 合同")
@RestController
@RequestMapping("/contract/contract")
@Validated
public class ContractController {

    @Resource
    private ContractService contractService;
    
    @Resource
    private ContractTypeService contractTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建合同")
    @PreAuthorize("@ss.hasPermission('contract:contract:create')")
    public CommonResult<Long> createContract(@Valid @RequestBody ContractCreateReqVO createReqVO) {
        return success(contractService.createContract(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新合同")
    @PreAuthorize("@ss.hasPermission('contract:contract:update')")
    public CommonResult<Boolean> updateContract(@Valid @RequestBody ContractUpdateReqVO updateReqVO) {
        contractService.updateContract(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合同")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:contract:delete')")
    public CommonResult<Boolean> deleteContract(@RequestParam("id") Long id) {
        contractService.deleteContract(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得合同")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('contract:contract:query')")
    public CommonResult<ContractRespVO> getContract(@RequestParam("id") Long id) {
        ContractDO contract = contractService.getContract(id);
        if (contract == null) {
            return success(null);
        }
        // 拼接数据
        ContractRespVO respVO = ContractConvert.INSTANCE.convert(contract);
        // 设置合同类型名称
        ContractTypeDO contractType = contractTypeService.getContractType(contract.getTypeId());
        if (contractType != null) {
            respVO.setTypeName(contractType.getName());
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得合同分页")
    @PreAuthorize("@ss.hasPermission('contract:contract:query')")
    public CommonResult<PageResult<ContractRespVO>> getContractPage(@Valid ContractPageReqVO pageVO) {
        PageResult<ContractDO> pageResult = contractService.getContractPage(pageVO);
        if (pageResult.getList().isEmpty()) {
            return success(PageResult.empty());
        }
        
        // 获取合同类型Map
        List<Long> typeIds = pageResult.getList().stream()
                .map(ContractDO::getTypeId)
                .distinct()
                .collect(Collectors.toList());
        List<ContractTypeDO> types = contractTypeService.getContractTypeList();
        Map<Long, String> typeMap = types.stream()
                .collect(Collectors.toMap(ContractTypeDO::getId, ContractTypeDO::getName));
        
        // 转换并设置类型名称
        List<ContractRespVO> list = ContractConvert.INSTANCE.convertList(pageResult.getList());
        list.forEach(item -> item.setTypeName(typeMap.get(item.getTypeId())));
        
        return success(new PageResult<>(list, pageResult.getTotal()));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交合同")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:contract:update')")
    public CommonResult<Boolean> submitContract(@RequestParam("id") Long id) {
        contractService.submitContract(id);
        return success(true);
    }

    @GetMapping("/get-by-no")
    @Operation(summary = "根据合同编号获取合同")
    @Parameter(name = "contractNo", description = "合同编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:contract:query')")
    public CommonResult<ContractRespVO> getContractByNo(@RequestParam("contractNo") String contractNo) {
        ContractDO contract = contractService.getContractByNo(contractNo);
        return success(ContractConvert.INSTANCE.convert(contract));
    }

    @GetMapping("/expiring")
    @Operation(summary = "获取即将到期的合同")
    @Parameter(name = "days", description = "天数", required = true, example = "30")
    @PreAuthorize("@ss.hasPermission('contract:contract:query')")
    public CommonResult<List<ContractRespVO>> getExpiringContracts(@RequestParam("days") Integer days) {
        List<ContractDO> list = contractService.getExpiringContracts(days);
        return success(ContractConvert.INSTANCE.convertList(list));
    }

} 