package cn.iocoder.yudao.module.contract.controller.admin.template;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.template.vo.*;
import cn.iocoder.yudao.module.contract.convert.template.ContractTemplateConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.template.ContractTemplateDO;
import cn.iocoder.yudao.module.contract.service.template.ContractTemplateService;
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

@Tag(name = "管理后台 - 合同模板")
@RestController
@RequestMapping("/contract/template")
@Validated
public class ContractTemplateController {

    @Resource
    private ContractTemplateService contractTemplateService;

    @PostMapping("/create")
    @Operation(summary = "创建合同模板")
    @PreAuthorize("@ss.hasPermission('contract:template:create')")
    public CommonResult<Long> createContractTemplate(@Valid @RequestBody ContractTemplateCreateReqVO createReqVO) {
        return success(contractTemplateService.createContractTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新合同模板")
    @PreAuthorize("@ss.hasPermission('contract:template:update')")
    public CommonResult<Boolean> updateContractTemplate(@Valid @RequestBody ContractTemplateUpdateReqVO updateReqVO) {
        contractTemplateService.updateContractTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合同模板")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:template:delete')")
    public CommonResult<Boolean> deleteContractTemplate(@RequestParam("id") Long id) {
        contractTemplateService.deleteContractTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得合同模板")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('contract:template:query')")
    public CommonResult<ContractTemplateRespVO> getContractTemplate(@RequestParam("id") Long id) {
        ContractTemplateDO contractTemplate = contractTemplateService.getContractTemplate(id);
        return success(ContractTemplateConvert.INSTANCE.convert(contractTemplate));
    }

    @GetMapping("/page")
    @Operation(summary = "获得合同模板分页")
    @PreAuthorize("@ss.hasPermission('contract:template:query')")
    public CommonResult<PageResult<ContractTemplateRespVO>> getContractTemplatePage(@Valid ContractTemplatePageReqVO pageVO) {
        PageResult<ContractTemplateDO> pageResult = contractTemplateService.getContractTemplatePage(pageVO);
        return success(ContractTemplateConvert.INSTANCE.convertPage(pageResult));
    }

    @GetMapping("/list-by-type")
    @Operation(summary = "根据合同类型获取模板列表")
    @Parameter(name = "contractTypeId", description = "合同类型编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:template:query')")
    public CommonResult<List<ContractTemplateRespVO>> getContractTemplateListByTypeId(@RequestParam("contractTypeId") Long contractTypeId) {
        List<ContractTemplateDO> list = contractTemplateService.getContractTemplateListByTypeId(contractTypeId);
        return success(ContractTemplateConvert.INSTANCE.convertList(list));
    }

} 