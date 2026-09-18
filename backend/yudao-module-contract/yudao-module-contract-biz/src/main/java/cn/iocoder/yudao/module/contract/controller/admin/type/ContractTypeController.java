package cn.iocoder.yudao.module.contract.controller.admin.type;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.type.vo.*;
import cn.iocoder.yudao.module.contract.convert.type.ContractTypeConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.type.ContractTypeDO;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 合同类型")
@RestController
@RequestMapping("/contract/type")
@Validated
public class ContractTypeController {

    @Resource
    private ContractTypeService contractTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建合同类型")
    @PreAuthorize("@ss.hasPermission('contract:type:create')")
    public CommonResult<Long> createContractType(@Valid @RequestBody ContractTypeCreateReqVO createReqVO) {
        return success(contractTypeService.createContractType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新合同类型")
    @PreAuthorize("@ss.hasPermission('contract:type:update')")
    public CommonResult<Boolean> updateContractType(@Valid @RequestBody ContractTypeUpdateReqVO updateReqVO) {
        contractTypeService.updateContractType(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合同类型")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:type:delete')")
    public CommonResult<Boolean> deleteContractType(@RequestParam("id") Long id) {
        contractTypeService.deleteContractType(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得合同类型")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('contract:type:query')")
    public CommonResult<ContractTypeRespVO> getContractType(@RequestParam("id") Long id) {
        ContractTypeDO contractType = contractTypeService.getContractType(id);
        return success(ContractTypeConvert.INSTANCE.convert(contractType));
    }

    @GetMapping("/page")
    @Operation(summary = "获得合同类型分页")
    @PreAuthorize("@ss.hasPermission('contract:type:query')")
    public CommonResult<PageResult<ContractTypeRespVO>> getContractTypePage(@Valid ContractTypePageReqVO pageVO) {
        PageResult<ContractTypeDO> pageResult = contractTypeService.getContractTypePage(pageVO);
        return success(ContractTypeConvert.INSTANCE.convertPage(pageResult));
    }

    @GetMapping("/list-all")
    @Operation(summary = "获得所有合同类型列表")
    @PreAuthorize("@ss.hasPermission('contract:type:query')")
    public CommonResult<List<ContractTypeRespVO>> getContractTypeList() {
        List<ContractTypeDO> list = contractTypeService.getContractTypeList();
        return success(ContractTypeConvert.INSTANCE.convertList(list));
    }

} 