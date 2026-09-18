package cn.iocoder.yudao.module.enterprise.controller.app.enterprise;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.enterprise.controller.app.enterprise.vo.*;
import cn.iocoder.yudao.module.enterprise.convert.EnterpriseInfoConvert;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseUserRelationDO;
import cn.iocoder.yudao.module.enterprise.enums.EnterpriseStatusEnum;
import cn.iocoder.yudao.module.enterprise.enums.EnterpriseTypeEnum;
import cn.iocoder.yudao.module.enterprise.enums.EnterpriseUserRelationTypeEnum;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseUserRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * APP - 企业信息 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "APP - 企业信息")
@RestController
@RequestMapping("/enterprise")
@Validated
public class AppEnterpriseController {

    @Resource
    private EnterpriseInfoService enterpriseInfoService;
    
    @Resource
    private EnterpriseUserRelationService enterpriseUserRelationService;

    @GetMapping("/simple-list")
    @Operation(summary = "获得企业信息精简列表", description = "主要用于下拉选择，会自动过滤出当前用户有权限的企业")
    @Parameter(name = "name", description = "企业名称，模糊匹配", example = "芋道")
    @PreAuthorize("@ss.hasPermission('enterprise:list')")
    public CommonResult<List<AppEnterpriseInfoSimpleRespVO>> getEnterpriseSimpleList(
            @RequestParam(value = "name", required = false) String name) {
        // 查询企业列表
        // TODO 待完善：需要根据当前登录的会员用户，过滤出其有权限的企业
        // 仅查询已完成全部认证的企业
        List<EnterpriseInfoDO> list = enterpriseInfoService.getEnterpriseInfoSimpleList(name, 
                EnterpriseStatusEnum.FULLY_CERTIFIED.getStatus(),  
                1); // 废物产生企业类型值为1
        return success(EnterpriseInfoConvert.INSTANCE.convertAppSimpleList(list));
    }
    
    @PostMapping("/waste-producer/bind")
    @Operation(summary = "产废企业用户绑定", description = "会员用户绑定现有产废企业")
    @PermitAll
    public CommonResult<Long> bindWasteProducer(@RequestBody @Valid AppEnterpriseBindReqVO reqVO) {
        // 获取当前登录用户ID
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        
        // 检查企业是否存在且为产废企业类型
        EnterpriseInfoDO enterprise = enterpriseInfoService.getEnterpriseInfo(reqVO.getEnterpriseId());
        if (enterprise == null || !Integer.valueOf(1).equals(enterprise.getEnterpriseType())) { // 废物产生企业类型值为1
            return CommonResult.error(400, "企业不存在或非产废企业类型");
        }
        
        // 创建用户与企业的关联关系
        EnterpriseUserRelationDO relation = new EnterpriseUserRelationDO();
        relation.setUserId(userId);
        relation.setEnterpriseId(reqVO.getEnterpriseId());
        relation.setStoreId(reqVO.getStoreId()); // 可选
        relation.setRelationType(reqVO.getRelationType() != null ? 
                reqVO.getRelationType() : EnterpriseUserRelationTypeEnum.EMPLOYEE.getType());
        relation.setIsPrimaryContact(reqVO.getIsPrimaryContact() != null ? reqVO.getIsPrimaryContact() : false);
        relation.setIsDefaultEnterprise(true); // 默认设置为用户的默认企业
        
        // 保存关联关系
        Long relationId = enterpriseUserRelationService.createUserRelation(
                EnterpriseInfoConvert.INSTANCE.convertToRelationCreateReq(relation));
        
        return success(relationId);
    }

    @GetMapping("/my-enterprise")
    @Operation(summary = "获得我的企业信息", description = "获取当前登录用户关联的企业信息，包括入驻申请状态")
    @PermitAll
    public CommonResult<List<AppEnterpriseInfoRespVO>> getMyEnterprises() {
        // 获取当前登录用户ID
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        
        // 查询该用户关联的所有企业
        List<EnterpriseUserRelationDO> relations = enterpriseUserRelationService.getUserRelationListByUserId(userId);
        if (relations.isEmpty()) {
            return success(Collections.emptyList());
        }
        
        // 获取企业ID列表
        List<Long> enterpriseIds = relations.stream()
                .map(EnterpriseUserRelationDO::getEnterpriseId)
                .collect(Collectors.toList());
        
        // 查询企业详细信息
        List<EnterpriseInfoDO> enterprises = enterpriseInfoService.getEnterpriseInfoList(enterpriseIds);
        
        // 转换为VO对象并返回
        List<AppEnterpriseInfoRespVO> result = EnterpriseInfoConvert.INSTANCE.convertAppList(enterprises);
        return success(result);
    }
} 