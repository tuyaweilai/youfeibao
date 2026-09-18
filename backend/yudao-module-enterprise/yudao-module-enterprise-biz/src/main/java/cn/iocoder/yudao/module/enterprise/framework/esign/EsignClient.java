package cn.iocoder.yudao.module.enterprise.framework.esign;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.enterprise.framework.esign.utils.EsignSignatureUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * e签宝开放平台客户端封装
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EsignClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final EsignProperties properties;

    /**
     * 构建请求头（支持RSA签名）
     */
    private HttpHeaders buildHeaders(String method, String url, String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        // 设置基础头部
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'"));
        headers.add("Date", date);
        headers.add("X-Tsign-Open-App-Id", properties.getAppId());
        
        if (properties.useRsaSignature()) {
            // 使用RSA签名
            buildRsaSignatureHeaders(headers, method, url, requestBody, date);
        } else {
            // 使用AppSecret（仅沙箱环境）
            headers.add("X-Tsign-Open-App-Secret", properties.getAppSecret());
        }
        
        return headers;
    }

    /**
     * 构建RSA签名请求头
     */
    private void buildRsaSignatureHeaders(HttpHeaders headers, String method, String url, String requestBody, String date) {
        try {
            // 计算Content-MD5
            String contentMd5 = EsignSignatureUtils.calculateContentMd5(requestBody);
            headers.add("Content-MD5", contentMd5);
            
            // 构建自定义请求头Map
            Map<String, String> customHeaders = new HashMap<>();
            customHeaders.put("X-Tsign-Open-App-Id", properties.getAppId());
            
            // 从URL中提取路径
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (uri.getQuery() != null) {
                path += "?" + uri.getQuery();
            }
            
            // 生成签名
            String signature = EsignSignatureUtils.generateSignature(
                    method,
                    MediaType.APPLICATION_JSON_VALUE,
                    contentMd5,
                    MediaType.APPLICATION_JSON_VALUE,
                    date,
                    customHeaders,
                    path,
                    properties.getRsaPrivateKey()
            );
            
            headers.add("Authorization", "Algorithm=RSA256, Signature=" + signature);
            
            log.debug("RSA签名生成成功: {}", signature);
        } catch (Exception e) {
            log.error("构建RSA签名头部失败", e);
            throw new RuntimeException("构建RSA签名头部失败", e);
        }
    }

    /**
     * 获取企业认证&授权页面链接
     * 对接接口：/v3/org-auth-url（符合官方v3文档）
     */
    public Map<String, Object> getOrgAuthUrl(String orgName, String orgIDCardNum, String orgIDCardType, 
                                           String legalRepName, String legalRepIDCardNum, String legalRepIDCardType,
                                           String redirectUrl, String notifyUrl) {
        String url = properties.getBaseUrl() + "/v3/org-auth-url";
        
        // 构建符合v3文档的请求参数
        Map<String, Object> requestBody = new HashMap<>();
        
        // 组织机构认证配置项
        Map<String, Object> orgAuthConfig = new HashMap<>();
        orgAuthConfig.put("orgName", orgName);
        
        // 组织机构身份附加信息
        Map<String, Object> orgInfo = new HashMap<>();
        orgInfo.put("orgIDCardNum", orgIDCardNum);
        orgInfo.put("orgIDCardType", orgIDCardType);
        orgInfo.put("legalRepName", legalRepName);
        orgInfo.put("legalRepIDCardNum", legalRepIDCardNum);
        orgInfo.put("legalRepIDCardType", legalRepIDCardType);
        orgAuthConfig.put("orgInfo", orgInfo);
        
        // 机构实名认证页面配置项
        Map<String, Object> orgAuthPageConfig = new HashMap<>();
        orgAuthPageConfig.put("orgDefaultAuthMode", "ORG_BANK_TRANSFER");
        List<String> availableAuthModes = Arrays.asList("ORG_BANK_TRANSFER", "ORG_LEGALREP_INVOLVED");
        orgAuthPageConfig.put("orgAvailableAuthModes", availableAuthModes);
        orgAuthConfig.put("orgAuthPageConfig", orgAuthPageConfig);
        
        requestBody.put("orgAuthConfig", orgAuthConfig);
        
        // 授权配置（根据业务需要调整）
        Map<String, Object> authorizeConfig = new HashMap<>();
        List<String> authorizedScopes = Arrays.asList("get_org_identity_info", "get_psn_identity_info");
        authorizeConfig.put("authorizedScopes", authorizedScopes);
        requestBody.put("authorizeConfig", authorizeConfig);
        
        // 认证完成重定向配置项
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            Map<String, Object> redirectConfig = new HashMap<>();
            redirectConfig.put("redirectUrl", redirectUrl);
            redirectConfig.put("redirectDelayTime", "3"); // 3秒后跳转
            requestBody.put("redirectConfig", redirectConfig);
        }
        
        // 其他配置
        requestBody.put("clientType", "ALL"); // 自动适配移动端或PC端
        if (notifyUrl != null && !notifyUrl.isEmpty()) {
            requestBody.put("notifyUrl", notifyUrl);
        }
        
        // 转换为JSON字符串
        String requestJson = JSONUtil.toJsonStr(requestBody);
        log.info("调用e签宝v3企业认证接口，请求参数: {}", requestJson);
        
        // 构建请求头（包含签名）
        HttpHeaders headers = buildHeaders("POST", url, requestJson);
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
        
        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            log.info("e签宝v3企业认证接口响应: {}", response);
            return response;
        } catch (Exception e) {
            log.error("调用e签宝v3企业认证接口失败", e);
            throw new RuntimeException("调用e签宝企业认证接口失败: " + e.getMessage());
        }
    }

    /**
     * 获取个人认证&授权页面链接
     * 对接接口：/v3/psn-auth-url
     */
    public Map<String, Object> getPersonalAuthUrl(String realName, String idCardNo, String redirectUrl, String notifyUrl) {
        String url = properties.getBaseUrl() + "/v3/psn-auth-url";
        
        // 构建符合v3文档的请求参数
        Map<String, Object> requestBody = new HashMap<>();
        
        // 个人认证配置项
        Map<String, Object> psnAuthConfig = new HashMap<>();
        
        Map<String, Object> psnInfo = new HashMap<>();
        psnInfo.put("psnName", realName);
        psnInfo.put("psnIDCardNum", idCardNo);
        psnInfo.put("psnIDCardType", "CRED_PSN_CH_IDCARD");
        psnAuthConfig.put("psnInfo", psnInfo);
        
        Map<String, Object> psnAuthPageConfig = new HashMap<>();
        psnAuthPageConfig.put("psnDefaultAuthMode", "PSN_FACE");
        psnAuthConfig.put("psnAuthPageConfig", psnAuthPageConfig);
        
        requestBody.put("psnAuthConfig", psnAuthConfig);
        
        // 授权配置
        Map<String, Object> authorizeConfig = new HashMap<>();
        List<String> authorizedScopes = Arrays.asList("get_psn_identity_info");
        authorizeConfig.put("authorizedScopes", authorizedScopes);
        requestBody.put("authorizeConfig", authorizeConfig);
        
        // 重定向配置
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            Map<String, Object> redirectConfig = new HashMap<>();
            redirectConfig.put("redirectUrl", redirectUrl);
            redirectConfig.put("redirectDelayTime", "3");
            requestBody.put("redirectConfig", redirectConfig);
        }
        
        requestBody.put("clientType", "ALL");
        if (notifyUrl != null && !notifyUrl.isEmpty()) {
            requestBody.put("notifyUrl", notifyUrl);
        }
        
        // 转换为JSON字符串
        String requestJson = JSONUtil.toJsonStr(requestBody);
        log.info("调用e签宝v3个人认证接口，请求参数: {}", requestJson);
        
        // 构建请求头（包含签名）
        HttpHeaders headers = buildHeaders("POST", url, requestJson);
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
        
        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            log.info("e签宝v3个人认证接口响应: {}", response);
            return response;
        } catch (Exception e) {
            log.error("调用e签宝v3个人认证接口失败", e);
            throw new RuntimeException("调用e签宝个人认证接口失败: " + e.getMessage());
        }
    }
} 