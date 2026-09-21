package cn.iocoder.yudao.module.icbc.controller.admin.platform;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignConfigRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.PlatformEsignQuotaSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.PlatformEsignTenantRespVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.esign.EsignConfigService;
import cn.iocoder.yudao.module.icbc.service.esign.EsignTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 平台运营：电子签章平台级参数与租户开通总览（#92，ADR 0036）。
 *
 * <p>接入第三方电子签章是一次**配置动作而不是改代码**：环境、两套 endpoint、应用标识、密钥、
 * 回调地址与验签、签署链接渠道、平台模板都在这里维护。密钥**只落后端、界面不回显明文**。
 *
 * <p>平台运营还能看到每个租户的激活状态与合同额度，但**不代盖企业章**：章是租户级的（ADR 0001）。
 */
@Tag(name = "管理后台 - 平台运营：电子签章配置与租户开通")
@RestController
@RequestMapping("/icbc/platform/esign")
@Validated
public class PlatformEsignController {

    @Resource
    private EsignConfigService esignConfigService;
    @Resource
    private EsignTenantService esignTenantService;

    @GetMapping("/config")
    @Operation(summary = "查看电子签章平台参数", description = "密钥只回「已配置」与否，不回明文")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_ESIGN_QUERY + "')")
    public CommonResult<EsignConfigRespVO> getConfig() {
        return success(esignConfigService.getConfig());
    }

    @PutMapping("/config")
    @Operation(summary = "保存电子签章平台参数", description = "密钥字段留空表示不改动既有值")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_ESIGN_MANAGE + "')")
    public CommonResult<Long> saveConfig(@Valid @RequestBody EsignConfigSaveReqVO reqVO) {
        return success(esignConfigService.saveConfig(reqVO));
    }

    @GetMapping("/tenant/list")
    @Operation(summary = "查看各租户的电子签章激活状态与合同额度（跨租户）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_ESIGN_QUERY + "')")
    public CommonResult<List<PlatformEsignTenantRespVO>> listTenants() {
        return success(esignTenantService.listPlatformTenants());
    }

    @PutMapping("/quota")
    @Operation(summary = "调整某租户的电子签章合同额度")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_ESIGN_MANAGE + "')")
    public CommonResult<Boolean> updateQuota(@Valid @RequestBody PlatformEsignQuotaSaveReqVO reqVO) {
        esignTenantService.updateQuota(reqVO);
        return success(true);
    }

}
