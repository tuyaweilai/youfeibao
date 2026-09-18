package cn.iocoder.yudao.module.enterprise.controller.admin.relation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.*;
import cn.iocoder.yudao.module.enterprise.convert.EnterpriseUserRelationConvert;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseStoreDO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseUserRelationDO;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseStoreService;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseUserRelationService;
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
 * 管理后台 - 用户企业关系
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 用户企业关系")
@RestController
@RequestMapping("/enterprise/user-relation")
@Validated
public class EnterpriseUserRelationController {

    @Resource
    private EnterpriseUserRelationService enterpriseUserRelationService;
    
    @Resource
    private EnterpriseInfoService enterpriseInfoService;
    
    @Resource
    private EnterpriseStoreService enterpriseStoreService;

    @PostMapping("/create")
    @Operation(summary = "创建用户企业关系")
    @PreAuthorize("@ss.hasPermission('enterprise:user:manage')")
    public CommonResult<Long> createUserRelation(@Valid @RequestBody EnterpriseUserRelationCreateReqVO createReqVO) {
        return success(enterpriseUserRelationService.createUserRelation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户企业关系")
    @PreAuthorize("@ss.hasPermission('enterprise:user:manage')")
    public CommonResult<Boolean> updateUserRelation(@Valid @RequestBody EnterpriseUserRelationUpdateReqVO updateReqVO) {
        enterpriseUserRelationService.updateUserRelation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户企业关系")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('enterprise:user:manage')")
    public CommonResult<Boolean> deleteUserRelation(@RequestParam("id") Long id) {
        enterpriseUserRelationService.deleteUserRelation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得用户企业关系")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('enterprise:user:query')")
    public CommonResult<EnterpriseUserRelationRespVO> getUserRelation(@RequestParam("id") Long id) {
        EnterpriseUserRelationDO relation = enterpriseUserRelationService.getUserRelation(id);
        EnterpriseUserRelationRespVO respVO = EnterpriseUserRelationConvert.INSTANCE.convert(relation);
        fillRelationInfo(respVO);
        return success(respVO);
    }

    @GetMapping("/list-by-user")
    @Operation(summary = "获得用户的企业关系列表")
    @Parameter(name = "userId", description = "用户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('enterprise:user:query')")
    public CommonResult<List<EnterpriseUserRelationRespVO>> getUserRelationListByUserId(@RequestParam("userId") Long userId) {
        List<EnterpriseUserRelationDO> list = enterpriseUserRelationService.getUserRelationListByUserId(userId);
        List<EnterpriseUserRelationRespVO> respList = EnterpriseUserRelationConvert.INSTANCE.convertList(list);
        respList.forEach(this::fillRelationInfo);
        return success(respList);
    }

    @GetMapping("/list-by-enterprise")
    @Operation(summary = "获得企业下的用户关系列表")
    @Parameter(name = "enterpriseId", description = "企业编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('enterprise:user:query')")
    public CommonResult<List<EnterpriseUserRelationRespVO>> getUserRelationListByEnterpriseId(@RequestParam("enterpriseId") Long enterpriseId) {
        List<EnterpriseUserRelationDO> list = enterpriseUserRelationService.getUserRelationListByEnterpriseId(enterpriseId);
        List<EnterpriseUserRelationRespVO> respList = EnterpriseUserRelationConvert.INSTANCE.convertList(list);
        respList.forEach(this::fillRelationInfo);
        return success(respList);
    }

    @GetMapping("/list-by-store")
    @Operation(summary = "获得门店下的用户关系列表")
    @Parameter(name = "storeId", description = "门店编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('enterprise:user:query')")
    public CommonResult<List<EnterpriseUserRelationRespVO>> getUserRelationListByStoreId(@RequestParam("storeId") Long storeId) {
        List<EnterpriseUserRelationDO> list = enterpriseUserRelationService.getUserRelationListByStoreId(storeId);
        List<EnterpriseUserRelationRespVO> respList = EnterpriseUserRelationConvert.INSTANCE.convertList(list);
        respList.forEach(this::fillRelationInfo);
        return success(respList);
    }

    @GetMapping("/page")
    @Operation(summary = "获得用户企业关系分页")
    @PreAuthorize("@ss.hasPermission('enterprise:user:query')")
    public CommonResult<PageResult<EnterpriseUserRelationRespVO>> pageUserRelation(@Valid EnterpriseUserRelationPageReqVO pageVO) {
        PageResult<EnterpriseUserRelationDO> pageResult = enterpriseUserRelationService.pageUserRelation(pageVO);
        PageResult<EnterpriseUserRelationRespVO> respPage = EnterpriseUserRelationConvert.INSTANCE.convertPage(pageResult);
        respPage.getList().forEach(this::fillRelationInfo);
        return success(respPage);
    }

    @PutMapping("/set-default")
    @Operation(summary = "设置默认企业")
    @Parameter(name = "userId", description = "用户编号", required = true)
    @Parameter(name = "relationId", description = "关系编号", required = true)
    @PreAuthorize("@ss.hasPermission('enterprise:user:manage')")
    public CommonResult<Boolean> setDefaultEnterprise(@RequestParam("userId") Long userId, @RequestParam("relationId") Long relationId) {
        enterpriseUserRelationService.setDefaultEnterprise(userId, relationId);
        return success(true);
    }
    
    /**
     * 填充关系的扩展信息（企业名称、门店名称等）
     * 
     * @param respVO 响应VO
     */
    private void fillRelationInfo(EnterpriseUserRelationRespVO respVO) {
        if (respVO == null) {
            return;
        }
        
        // 填充企业名称
        if (respVO.getEnterpriseId() != null) {
            EnterpriseInfoDO enterprise = enterpriseInfoService.getEnterpriseInfo(respVO.getEnterpriseId());
            if (enterprise != null) {
                respVO = EnterpriseUserRelationConvert.INSTANCE.setEnterpriseInfo(respVO, enterprise);
            }
        }
        
        // 填充门店名称
        if (respVO.getStoreId() != null) {
            EnterpriseStoreDO store = enterpriseStoreService.getEnterpriseStore(respVO.getStoreId());
            if (store != null) {
                respVO = EnterpriseUserRelationConvert.INSTANCE.setStoreInfo(respVO, store);
            }
        }
        
        // TODO 用户名称可以从用户模块获取，这里暂时未实现
    }
} 