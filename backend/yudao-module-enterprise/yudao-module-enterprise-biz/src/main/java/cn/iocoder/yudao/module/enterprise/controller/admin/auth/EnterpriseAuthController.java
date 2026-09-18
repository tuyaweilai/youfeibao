package cn.iocoder.yudao.module.enterprise.controller.admin.auth;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoCreateReqVO;
import cn.iocoder.yudao.module.enterprise.convert.EnterpriseInfoConvert;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseUserRelationDO;
import cn.iocoder.yudao.module.enterprise.enums.EnterpriseUserRelationTypeEnum;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseUserRelationService;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseCertificationService;
import cn.iocoder.yudao.module.enterprise.service.PersonalCertificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 企业入驻认证
 * 
 * 提供用户注册后进行企业入驻的功能，无需权限控制
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 企业入驻认证")
@RestController
@RequestMapping("/enterprise/auth")
@Validated
public class EnterpriseAuthController {

    @Resource
    private EnterpriseInfoService enterpriseInfoService;
    
    @Resource
    private EnterpriseUserRelationService enterpriseUserRelationService;

    @Resource
    private EnterpriseCertificationService enterpriseCertificationService;

    @Resource
    private PersonalCertificationService personalCertificationService;

    @PostMapping("/register")
    @Operation(summary = "企业入驻", description = "创建企业信息并自动与当前用户关联，注册用户可以直接调用此接口进行企业入驻")
    @PermitAll // 无需权限控制，注册用户即可访问
    public CommonResult<Long> registerEnterprise(@Valid @RequestBody EnterpriseInfoCreateReqVO createReqVO) {
        // 1. 获取当前登录用户ID并校验登录状态
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return CommonResult.error(401, "用户未登录，请先登录");
        }
        
        // 2. 校验用户是否已关联企业
        List<EnterpriseUserRelationDO> existingRelations = enterpriseUserRelationService.getUserRelationListByUserId(userId);
        if (!existingRelations.isEmpty()) {
            return CommonResult.error(400, "您已关联企业，不能重复入驻");
        }

        // 3. 创建企业信息
        Long enterpriseId = enterpriseInfoService.createEnterpriseInfo(createReqVO);
        
        // 4. 创建用户与企业的关联关系
        EnterpriseUserRelationDO relation = new EnterpriseUserRelationDO();
        relation.setUserId(userId);
        relation.setEnterpriseId(enterpriseId);
        relation.setRelationType(EnterpriseUserRelationTypeEnum.ADMIN.getType()); // 设置为企业管理员
        relation.setIsPrimaryContact(true); // 设置为主要联系人
        relation.setIsDefaultEnterprise(true); // 设置为默认企业
        
        // 5. 保存关联关系
        Long relationId = enterpriseUserRelationService.createUserRelation(
                EnterpriseInfoConvert.INSTANCE.convertToRelationCreateReq(relation));
        
        return success(enterpriseId);
    }

    @PostMapping("/enterprise/initiate-auth")
    @Operation(summary = "发起企业认证", description = "调用e签宝接口，返回认证URL")
    @PreAuthorize("@ss.hasPermission('enterprise:info:auth')")
    public CommonResult<String> initiateEnterpriseAuth(@RequestParam("enterpriseId") Long enterpriseId) {
        String url = enterpriseCertificationService.initiateEnterpriseCertification(enterpriseId);
        return success(url);
    }

    @PostMapping("/personal/initiate-auth")
    @Operation(summary = "发起个人认证", description = "调用e签宝接口，返回认证URL")
    @PreAuthorize("@ss.hasPermission('enterprise:info:auth')")
    public CommonResult<String> initiatePersonalAuth(@RequestParam("realName") String realName,
                                                   @RequestParam("idCardNo") String idCardNo) {
        String url = personalCertificationService.initiatePersonalCertification(realName, idCardNo);
        return success(url);
    }
} 