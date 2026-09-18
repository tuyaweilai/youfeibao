package cn.iocoder.yudao.module.enterprise.service.impl;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.enterprise.framework.esign.EsignClient;
import cn.iocoder.yudao.module.enterprise.service.PersonalCertificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalCertificationServiceImpl implements PersonalCertificationService {

    private final EsignClient esignClient;

    @Override
    public String initiatePersonalCertification(String realName, String idCardNo) {
        // 获取当前登录用户ID
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        
        // 构建重定向和回调URL
        String redirectUrl = "https://your-domain.com/personal/auth/result"; // TODO: 配置化
        String notifyUrl = "https://your-domain.com/admin-api/enterprise/esign/callback/identity-verify"; // TODO: 配置化
        
        Map<String, Object> resp = esignClient.getPersonalAuthUrl(realName, idCardNo, redirectUrl, notifyUrl);
        if (resp == null || !"0".equals(String.valueOf(resp.get("code")))) {
            String errorMsg = resp != null ? (String) resp.get("message") : "调用失败";
            throw new RuntimeException("获取个人认证链接失败: " + errorMsg);
        }
        
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        String authUrl = (String) data.get("authUrl");
        String authShortUrl = (String) data.get("authShortUrl");
        
        return authShortUrl != null ? authShortUrl : authUrl;
    }
} 