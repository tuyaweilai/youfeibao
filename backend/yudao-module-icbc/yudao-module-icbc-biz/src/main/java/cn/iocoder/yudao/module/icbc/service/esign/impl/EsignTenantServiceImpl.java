package cn.iocoder.yudao.module.icbc.service.esign.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignActivateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignConfigRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignOpenConsoleRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignTenantStatusRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.PlatformEsignQuotaSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.PlatformEsignTenantRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.esign.IcbcEsignConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.esign.IcbcEsignTenantDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.esign.IcbcEsignTenantMapper;
import cn.iocoder.yudao.module.icbc.enums.EsignActivationStatusEnum;
import cn.iocoder.yudao.module.icbc.service.esign.EsignConfigService;
import cn.iocoder.yudao.module.icbc.service.esign.EsignTenantService;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 租户级电子签章配置与开通 Service 实现（#92，ADR 0036）。
 *
 * <p>子客编号是**我们生成的确定性编号**（{@code ES + 租户编号左补零到 10 位}）：由租户编号唯一决定，
 * 因此天然「不可重复」；生成后落库、后续只读不写，因此「不可变」。用确定性编号而不是随机串，
 * 是为了让唯一性与幂等都不依赖重试。
 */
@Service
@Validated
public class EsignTenantServiceImpl implements EsignTenantService {

    /**
     * 控制台链接有效期（分钟）：够管理员当场走完认证，又不会留一枚长期有效的链接在聊天记录里。
     */
    private static final int CONSOLE_LINK_EXPIRE_MINUTES = 30;

    @Resource
    private IcbcEsignTenantMapper esignTenantMapper;
    @Resource
    private EsignConfigService esignConfigService;
    @Resource
    private TenantApi tenantApi;

    @Override
    public IcbcEsignTenantDO getOrCreateCurrent() {
        IcbcEsignTenantDO config = esignTenantMapper.selectCurrent();
        return config != null ? config : createCurrent();
    }

    @Override
    public EsignTenantStatusRespVO getStatus() {
        return toStatusRespVO(getOrCreateCurrent());
    }

    @Override
    public EsignOpenConsoleRespVO openConsole() {
        // 平台参数没配齐就明确失败：链接是拼在平台配置上的，缺一项都拼不出来
        EsignConfigRespVO platformConfig = esignConfigService.getConfig();
        if (!Boolean.TRUE.equals(platformConfig.getConfigured())) {
            throw exception(ESIGN_CONFIG_INCOMPLETE, String.join("、", platformConfig.getMissingFields()));
        }
        IcbcEsignConfigDO platform = esignConfigService.getRawConfig();
        IcbcEsignTenantDO config = getOrCreateCurrent();
        String token = IdUtil.fastSimpleUUID();
        IcbcEsignTenantDO update = new IcbcEsignTenantDO();
        update.setId(config.getId());
        // 一次性：再次开通就换新令牌，旧链接自然作废
        update.setConsoleToken(token);
        update.setConsoleTokenExpireTime(LocalDateTime.now().plusMinutes(CONSOLE_LINK_EXPIRE_MINUTES));
        if (!EsignActivationStatusEnum.ACTIVATED.getStatus().equals(config.getActivationStatus())) {
            update.setActivationStatus(EsignActivationStatusEnum.AUTHENTICATING.getStatus());
        }
        esignTenantMapper.updateById(update);
        config = esignTenantMapper.selectById(config.getId());
        return toOpenConsoleRespVO(config, platform, token);
    }

    @Override
    public void activate(EsignActivateReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getSealNo())) {
            throw exception(ESIGN_SEAL_NO_REQUIRED);
        }
        IcbcEsignTenantDO config = getOrCreateCurrent();
        IcbcEsignTenantDO update = new IcbcEsignTenantDO();
        update.setId(config.getId());
        update.setActivationStatus(EsignActivationStatusEnum.ACTIVATED.getStatus());
        update.setOperatorNo(reqVO.getOperatorNo());
        update.setSealNo(reqVO.getSealNo());
        update.setActivatedTime(LocalDateTime.now());
        // 激活即用掉这枚控制台令牌
        update.setConsoleToken("");
        if (StrUtil.isNotBlank(reqVO.getRemark())) {
            update.setRemark(reqVO.getRemark());
        }
        esignTenantMapper.updateById(update);
    }

    @Override
    public boolean isTenantActivated() {
        IcbcEsignTenantDO config = esignTenantMapper.selectCurrent();
        return config != null
                && EsignActivationStatusEnum.ACTIVATED.getStatus().equals(config.getActivationStatus())
                && StrUtil.isNotBlank(config.getSealNo());
    }

    @Override
    public Long resolveTenantIdBySubCustomerNo(String subCustomerNo) {
        if (StrUtil.isBlank(subCustomerNo)) {
            return null;
        }
        // 子客编号是全局唯一的，回调没有租户上下文，必须跨租户找一次
        IcbcEsignTenantDO config = TenantUtils.executeIgnore(
                () -> esignTenantMapper.selectBySubCustomerNo(subCustomerNo));
        return config != null ? config.getTenantId() : null;
    }

    @Override
    public List<PlatformEsignTenantRespVO> listPlatformTenants() {
        Map<Long, IcbcEsignTenantDO> configByTenant = new LinkedHashMap<>();
        for (IcbcEsignTenantDO config : listAllConfigs()) {
            configByTenant.put(config.getTenantId(), config);
        }
        // 以租户清单为准，并上配置行里出现过的租户（租户被删但配置还在时不至于漏掉）
        Set<Long> tenantIds = new LinkedHashSet<>();
        List<Long> allTenantIds = tenantApi.getTenantIdList();
        if (allTenantIds != null) {
            tenantIds.addAll(allTenantIds);
        }
        tenantIds.addAll(configByTenant.keySet());
        List<PlatformEsignTenantRespVO> result = new ArrayList<>();
        for (Long tenantId : tenantIds) {
            result.add(toPlatformRespVO(tenantId, configByTenant.get(tenantId)));
        }
        return result;
    }

    @Override
    public void updateQuota(PlatformEsignQuotaSaveReqVO reqVO) {
        Long tenantId = reqVO.getTenantId();
        TenantUtils.execute(tenantId, () -> {
            IcbcEsignTenantDO config = esignTenantMapper.selectCurrent();
            if (config == null) {
                IcbcEsignTenantDO created = buildNewConfig(tenantId);
                created.setContractQuota(reqVO.getContractQuota());
                created.setRemark(reqVO.getRemark());
                esignTenantMapper.insert(created);
                return;
            }
            IcbcEsignTenantDO update = new IcbcEsignTenantDO();
            update.setId(config.getId());
            update.setContractQuota(reqVO.getContractQuota());
            if (StrUtil.isNotBlank(reqVO.getRemark())) {
                update.setRemark(reqVO.getRemark());
            }
            esignTenantMapper.updateById(update);
        });
    }

    // ==================== 内部方法 ====================

    private IcbcEsignTenantDO createCurrent() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        IcbcEsignTenantDO created = buildNewConfig(tenantId);
        esignTenantMapper.insert(created);
        return esignTenantMapper.selectById(created.getId());
    }

    /**
     * 子客编号由租户编号确定性生成：唯一且不可变，不依赖随机串与重试。
     */
    private IcbcEsignTenantDO buildNewConfig(Long tenantId) {
        return IcbcEsignTenantDO.builder()
                .subCustomerNo(buildSubCustomerNo(tenantId))
                .activationStatus(EsignActivationStatusEnum.NOT_OPENED.getStatus())
                .contractQuota(0)
                .contractUsed(0)
                .build();
    }

    static String buildSubCustomerNo(Long tenantId) {
        return "ES" + String.format("%010d", tenantId);
    }

    private EsignOpenConsoleRespVO toOpenConsoleRespVO(IcbcEsignTenantDO config, IcbcEsignConfigDO platform,
                                                       String token) {
        EsignActivationStatusEnum status = EsignActivationStatusEnum.ofStatus(config.getActivationStatus());
        EsignOpenConsoleRespVO resp = new EsignOpenConsoleRespVO();
        resp.setSubCustomerNo(config.getSubCustomerNo());
        resp.setLink(buildConsoleLink(platform, config.getSubCustomerNo(), token));
        resp.setExpiresTime(config.getConsoleTokenExpireTime());
        resp.setActivationStatus(config.getActivationStatus());
        resp.setActivationStatusName(status != null ? status.getName() : null);
        resp.setNextStep(status != null ? status.getNextStep() : null);
        return resp;
    }

    /**
     * 一次性控制台链接：带上平台应用标识与本租户子客编号，第三方控制台据此把这次认证挂到正确的企业上。
     */
    private String buildConsoleLink(IcbcEsignConfigDO platform, String subCustomerNo, String token) {
        String base = StrUtil.removeSuffix(platform.getConsoleEndpoint(), "/");
        return base + "/enterprise/open?appId=" + encode(platform.getAppId())
                + "&subCustomerNo=" + encode(subCustomerNo)
                + "&token=" + encode(token);
    }

    private String encode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private List<IcbcEsignTenantDO> listAllConfigs() {
        return TenantUtils.executeIgnore(() -> esignTenantMapper.selectAll());
    }

    private PlatformEsignTenantRespVO toPlatformRespVO(Long tenantId, IcbcEsignTenantDO config) {
        Integer activationStatus = config != null ? config.getActivationStatus()
                : EsignActivationStatusEnum.NOT_OPENED.getStatus();
        EsignActivationStatusEnum status = EsignActivationStatusEnum.ofStatus(activationStatus);
        int quota = config == null || config.getContractQuota() == null ? 0 : config.getContractQuota();
        int used = config == null || config.getContractUsed() == null ? 0 : config.getContractUsed();
        PlatformEsignTenantRespVO resp = new PlatformEsignTenantRespVO();
        resp.setTenantId(tenantId);
        resp.setTenantName(tenantApi.getTenantName(tenantId));
        resp.setSubCustomerNo(config != null ? config.getSubCustomerNo() : null);
        resp.setActivationStatus(activationStatus);
        resp.setActivationStatusName(status != null ? status.getName() : null);
        resp.setSealNo(config != null ? config.getSealNo() : null);
        resp.setSealReady(config != null && StrUtil.isNotBlank(config.getSealNo()));
        resp.setContractQuota(quota);
        resp.setContractUsed(used);
        resp.setRemainingQuota(Math.max(quota - used, 0));
        resp.setActivatedTime(config != null ? config.getActivatedTime() : null);
        return resp;
    }

    private EsignTenantStatusRespVO toStatusRespVO(IcbcEsignTenantDO config) {
        EsignActivationStatusEnum status = EsignActivationStatusEnum.ofStatus(config.getActivationStatus());
        EsignTenantStatusRespVO resp = new EsignTenantStatusRespVO();
        resp.setSubCustomerNo(config.getSubCustomerNo());
        resp.setActivationStatus(config.getActivationStatus());
        resp.setActivationStatusName(status != null ? status.getName() : null);
        resp.setNextStep(status != null ? status.getNextStep() : null);
        resp.setOperatorNo(config.getOperatorNo());
        resp.setSealNo(config.getSealNo());
        resp.setSealReady(StrUtil.isNotBlank(config.getSealNo()));
        int quota = config.getContractQuota() == null ? 0 : config.getContractQuota();
        int used = config.getContractUsed() == null ? 0 : config.getContractUsed();
        resp.setContractQuota(quota);
        resp.setContractUsed(used);
        resp.setRemainingQuota(Math.max(quota - used, 0));
        resp.setActivatedTime(config.getActivatedTime());
        resp.setPlatformConfigured(esignConfigService.isPlatformConfigured());
        return resp;
    }

}
