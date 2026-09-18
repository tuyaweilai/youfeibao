package cn.iocoder.yudao.module.enterprise.service.impl;

import cn.iocoder.yudao.module.enterprise.dal.dataobject.EsignAuthFlowDO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import cn.iocoder.yudao.module.enterprise.dal.mysql.EnterpriseInfoMapper;
import cn.iocoder.yudao.module.enterprise.enums.EsignAuthStatusEnum;
import cn.iocoder.yudao.module.enterprise.enums.EsignAuthTypeEnum;
import cn.iocoder.yudao.module.enterprise.framework.esign.EsignClient;
import cn.iocoder.yudao.module.enterprise.framework.esign.EsignProperties;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseCertificationService;
import cn.iocoder.yudao.module.enterprise.service.EsignAuthFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnterpriseCertificationServiceImpl implements EnterpriseCertificationService {

    private final EsignClient esignClient;
    private final EsignAuthFlowService authFlowService;
    private final EsignProperties esignProperties;
    private final EnterpriseInfoMapper enterpriseInfoMapper;

    @Override
    public String initiateEnterpriseCertification(Long enterpriseId) {
        log.info("发起企业认证: enterpriseId={}", enterpriseId);
        
        // 获取企业信息
        EnterpriseInfoDO enterprise = enterpriseInfoMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new RuntimeException("企业不存在");
        }
        
        // 发起企业认证
        return initiateEnterpriseAuth(
                enterpriseId,
                enterprise.getName(),
                enterprise.getUnifiedSocialCreditCode(),
                enterprise.getLegalRepresentativeName(),
                enterprise.getLegalRepresentativeIdCard()
        );
    }

    @Override
    public String initiatePersonalAuth(Long userId, String realName, String idCardNo) {
        log.info("发起个人认证: userId={}, realName={}", userId, realName);
        
        try {
            // 构建重定向和回调URL
            String redirectUrl = buildRedirectUrl("personal");
            String notifyUrl = buildNotifyUrl("identity-verify");
            
            // 调用e签宝接口获取认证链接
            Map<String, Object> response = esignClient.getPersonalAuthUrl(realName, idCardNo, redirectUrl, notifyUrl);
            
            if (response == null || !"0".equals(String.valueOf(response.get("code")))) {
                String errorMsg = response != null ? (String) response.get("message") : "调用失败";
                throw new RuntimeException("获取个人认证链接失败: " + errorMsg);
            }
            
            // 解析响应数据
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String authFlowId = (String) data.get("authFlowId");
            String authUrl = (String) data.get("authUrl");
            String authShortUrl = (String) data.get("authShortUrl");
            
            // 保存认证流程记录
            EsignAuthFlowDO authFlow = EsignAuthFlowDO.builder()
                    .authFlowId(authFlowId)
                    .userId(userId)
                    .authType(EsignAuthTypeEnum.PERSONAL.getType())
                    .authStatus(EsignAuthStatusEnum.IN_PROGRESS.getStatus())
                    .authUrl(authUrl)
                    .authShortUrl(authShortUrl)
                    .notifyStatus(0)
                    .build();
            
            authFlowService.createAuthFlow(authFlow);
            
            log.info("个人认证流程创建成功: authFlowId={}, authUrl={}", authFlowId, authUrl);
            return authUrl;
            
        } catch (Exception e) {
            log.error("发起个人认证失败: userId={}", userId, e);
            throw new RuntimeException("发起个人认证失败: " + e.getMessage());
        }
    }

    @Override
    public String initiateEnterpriseAuth(Long enterpriseId, String orgName, String orgIDCardNum, 
                                       String legalRepName, String legalRepIDCardNum) {
        log.info("发起企业认证: enterpriseId={}, orgName={}", enterpriseId, orgName);
        
        try {
            // 构建重定向和回调URL
            String redirectUrl = buildRedirectUrl("enterprise");
            String notifyUrl = buildNotifyUrl("identity-verify");
            
            // 调用e签宝接口获取认证链接
            Map<String, Object> response = esignClient.getOrgAuthUrl(
                    orgName, orgIDCardNum, "CRED_ORG_USCC",
                    legalRepName, legalRepIDCardNum, "CRED_PSN_CH_IDCARD",
                    redirectUrl, notifyUrl
            );
            
            if (response == null || !"0".equals(String.valueOf(response.get("code")))) {
                String errorMsg = response != null ? (String) response.get("message") : "调用失败";
                throw new RuntimeException("获取企业认证链接失败: " + errorMsg);
            }
            
            // 解析响应数据
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String authFlowId = (String) data.get("authFlowId");
            String authUrl = (String) data.get("authUrl");
            String authShortUrl = (String) data.get("authShortUrl");
            
            // 保存认证流程记录
            EsignAuthFlowDO authFlow = EsignAuthFlowDO.builder()
                    .authFlowId(authFlowId)
                    .enterpriseId(enterpriseId)
                    .authType(EsignAuthTypeEnum.ENTERPRISE.getType())
                    .authStatus(EsignAuthStatusEnum.IN_PROGRESS.getStatus())
                    .authUrl(authUrl)
                    .authShortUrl(authShortUrl)
                    .notifyStatus(0)
                    .build();
            
            authFlowService.createAuthFlow(authFlow);
            
            log.info("企业认证流程创建成功: authFlowId={}, authUrl={}", authFlowId, authUrl);
            return authUrl;
            
        } catch (Exception e) {
            log.error("发起企业认证失败: enterpriseId={}", enterpriseId, e);
            throw new RuntimeException("发起企业认证失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getAuthStatus(String authFlowId) {
        EsignAuthFlowDO authFlow = authFlowService.getByAuthFlowId(authFlowId);
        if (authFlow == null) {
            throw new RuntimeException("认证流程不存在");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("authFlowId", authFlow.getAuthFlowId());
        result.put("authType", authFlow.getAuthType());
        result.put("authStatus", authFlow.getAuthStatus());
        result.put("notifyStatus", authFlow.getNotifyStatus());
        result.put("notifyTime", authFlow.getNotifyTime());
        result.put("errorMessage", authFlow.getErrorMessage() != null ? authFlow.getErrorMessage() : "");
        result.put("orgId", authFlow.getOrgId() != null ? authFlow.getOrgId() : "");
        result.put("personId", authFlow.getPersonId() != null ? authFlow.getPersonId() : "");
        return result;
    }

    /**
     * 构建重定向URL
     */
    private String buildRedirectUrl(String type) {
        if (esignProperties.getRedirectBaseUrl() != null) {
            return esignProperties.getRedirectBaseUrl() + "/enterprise/auth/" + type + "/result";
        }
        // 默认重定向URL
        return "https://your-domain.com/enterprise/auth/" + type + "/result";
    }

    /**
     * 构建回调通知URL
     */
    private String buildNotifyUrl(String type) {
        if (esignProperties.getNotifyBaseUrl() != null) {
            return esignProperties.getNotifyBaseUrl() + "/admin-api/enterprise/esign/callback/" + type;
        }
        // 默认回调URL
        return "https://your-domain.com/admin-api/enterprise/esign/callback/" + type;
    }
} 