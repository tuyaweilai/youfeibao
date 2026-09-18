package cn.iocoder.yudao.module.contract.controller.admin.attachment;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.attachment.vo.*;
import cn.iocoder.yudao.module.contract.convert.attachment.ContractAttachmentConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.attachment.ContractAttachmentDO;
import cn.iocoder.yudao.module.contract.service.attachment.ContractAttachmentService;
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

@Tag(name = "管理后台 - 合同附件")
@RestController
@RequestMapping("/contract/attachment")
@Validated
public class ContractAttachmentController {

    @Resource
    private ContractAttachmentService contractAttachmentService;

    @PostMapping("/create")
    @Operation(summary = "创建合同附件")
    @PreAuthorize("@ss.hasPermission('contract:attachment:create')")
    public CommonResult<Long> createContractAttachment(@Valid @RequestBody ContractAttachmentCreateReqVO createReqVO) {
        return success(contractAttachmentService.createContractAttachment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新合同附件")
    @PreAuthorize("@ss.hasPermission('contract:attachment:update')")
    public CommonResult<Boolean> updateContractAttachment(@Valid @RequestBody ContractAttachmentUpdateReqVO updateReqVO) {
        contractAttachmentService.updateContractAttachment(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合同附件")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:attachment:delete')")
    public CommonResult<Boolean> deleteContractAttachment(@RequestParam("id") Long id) {
        contractAttachmentService.deleteContractAttachment(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得合同附件")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('contract:attachment:query')")
    public CommonResult<ContractAttachmentRespVO> getContractAttachment(@RequestParam("id") Long id) {
        ContractAttachmentDO contractAttachment = contractAttachmentService.getContractAttachment(id);
        return success(ContractAttachmentConvert.INSTANCE.convert(contractAttachment));
    }

    @GetMapping("/page")
    @Operation(summary = "获得合同附件分页")
    @PreAuthorize("@ss.hasPermission('contract:attachment:query')")
    public CommonResult<PageResult<ContractAttachmentRespVO>> getContractAttachmentPage(@Valid ContractAttachmentPageReqVO pageVO) {
        PageResult<ContractAttachmentDO> pageResult = contractAttachmentService.getContractAttachmentPage(pageVO);
        return success(ContractAttachmentConvert.INSTANCE.convertPage(pageResult));
    }

    @GetMapping("/list-by-version")
    @Operation(summary = "根据合同版本获取附件列表")
    @Parameter(name = "versionId", description = "合同版本编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:attachment:query')")
    public CommonResult<List<ContractAttachmentRespVO>> getContractAttachmentListByVersionId(@RequestParam("versionId") Long versionId) {
        List<ContractAttachmentDO> list = contractAttachmentService.getContractAttachmentListByVersionId(versionId);
        return success(ContractAttachmentConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-version-and-type")
    @Operation(summary = "根据合同版本和附件类型获取附件列表")
    @PreAuthorize("@ss.hasPermission('contract:attachment:query')")
    public CommonResult<List<ContractAttachmentRespVO>> getContractAttachmentListByVersionIdAndType(
            @RequestParam("versionId") Long versionId,
            @RequestParam("attachmentType") Integer attachmentType) {
        List<ContractAttachmentDO> list = contractAttachmentService.getContractAttachmentListByVersionIdAndType(versionId, attachmentType);
        return success(ContractAttachmentConvert.INSTANCE.convertList(list));
    }

    @PostMapping("/download")
    @Operation(summary = "下载合同附件")
    @Parameter(name = "id", description = "附件编号", required = true)
    @PreAuthorize("@ss.hasPermission('contract:attachment:query')")
    public CommonResult<Boolean> downloadContractAttachment(@RequestParam("id") Long id) {
        contractAttachmentService.downloadContractAttachment(id);
        return success(true);
    }

} 