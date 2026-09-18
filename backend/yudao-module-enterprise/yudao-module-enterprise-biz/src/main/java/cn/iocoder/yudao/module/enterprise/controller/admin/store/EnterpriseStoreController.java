package cn.iocoder.yudao.module.enterprise.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.*;
import cn.iocoder.yudao.module.enterprise.convert.EnterpriseStoreConvert;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseStoreDO;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseStoreService;
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

/**
 * 管理后台 - 企业门店
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 企业门店")
@RestController
@RequestMapping("/enterprise/store")
@Validated
public class EnterpriseStoreController {

    @Resource
    private EnterpriseStoreService enterpriseStoreService;

    @PostMapping("/create")
    @Operation(summary = "创建企业门店")
    @PreAuthorize("@ss.hasPermission('enterprise:store:create')")
    public CommonResult<Long> createEnterpriseStore(@Valid @RequestBody EnterpriseStoreCreateReqVO createReqVO) {
        return success(enterpriseStoreService.createEnterpriseStore(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新企业门店")
    @PreAuthorize("@ss.hasPermission('enterprise:store:update')")
    public CommonResult<Boolean> updateEnterpriseStore(@Valid @RequestBody EnterpriseStoreUpdateReqVO updateReqVO) {
        enterpriseStoreService.updateEnterpriseStore(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新企业门店状态")
    @PreAuthorize("@ss.hasPermission('enterprise:store:update')")
    public CommonResult<Boolean> updateEnterpriseStoreStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        enterpriseStoreService.updateEnterpriseStoreStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除企业门店")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('enterprise:store:delete')")
    public CommonResult<Boolean> deleteEnterpriseStore(@RequestParam("id") Long id) {
        enterpriseStoreService.deleteEnterpriseStore(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得企业门店")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('enterprise:store:query')")
    public CommonResult<EnterpriseStoreRespVO> getEnterpriseStore(@RequestParam("id") Long id) {
        EnterpriseStoreDO store = enterpriseStoreService.getEnterpriseStore(id);
        return success(EnterpriseStoreConvert.INSTANCE.convert(store));
    }

    @GetMapping("/list-by-enterprise")
    @Operation(summary = "获得企业门店列表")
    @Parameter(name = "enterpriseId", description = "企业编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('enterprise:store:query')")
    public CommonResult<List<EnterpriseStoreRespVO>> getEnterpriseStoreListByEnterpriseId(@RequestParam("enterpriseId") Long enterpriseId) {
        List<EnterpriseStoreDO> list = enterpriseStoreService.getEnterpriseStoreListByEnterpriseId(enterpriseId);
        return success(EnterpriseStoreConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-parent")
    @Operation(summary = "获得子门店列表")
    @Parameter(name = "parentId", description = "父门店编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('enterprise:store:query')")
    public CommonResult<List<EnterpriseStoreRespVO>> getEnterpriseStoreListByParentId(@RequestParam("parentId") Long parentId) {
        List<EnterpriseStoreDO> list = enterpriseStoreService.getEnterpriseStoreListByParentId(parentId);
        return success(EnterpriseStoreConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/page")
    @Operation(summary = "获得企业门店分页")
    @PreAuthorize("@ss.hasPermission('enterprise:store:query')")
    public CommonResult<PageResult<EnterpriseStoreRespVO>> pageEnterpriseStore(@Valid EnterpriseStorePageReqVO pageVO) {
        PageResult<EnterpriseStoreDO> pageResult = enterpriseStoreService.pageEnterpriseStore(pageVO);
        return success(EnterpriseStoreConvert.INSTANCE.convertPage(pageResult));
    }

    @GetMapping("/list-tree")
    @Operation(summary = "获得企业门店树形列表")
    @Parameter(name = "enterpriseId", description = "企业编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('enterprise:store:query')")
    public CommonResult<List<EnterpriseStoreTreeRespVO>> getEnterpriseStoreTree(@RequestParam("enterpriseId") Long enterpriseId) {
        List<EnterpriseStoreDO> list = enterpriseStoreService.getEnterpriseStoreTree(enterpriseId);
        return success(EnterpriseStoreConvert.INSTANCE.convertTreeList(list));
    }
} 