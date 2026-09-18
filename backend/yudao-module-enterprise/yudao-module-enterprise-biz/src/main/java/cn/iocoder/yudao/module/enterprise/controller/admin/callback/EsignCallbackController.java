package cn.iocoder.yudao.module.enterprise.controller.admin.callback;

import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.config.SecurityProperties;
import cn.iocoder.yudao.module.enterprise.framework.esign.EsignProperties;
import cn.iocoder.yudao.module.enterprise.framework.esign.utils.EsignSignatureUtils;
import cn.iocoder.yudao.module.enterprise.service.EsignAuthFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

/**
 * e签宝回调控制器
 * 处理认证结果回调通知
 * 
 * 回调文档参考：
 * - 实名认证回调: https://open.esign.cn/doc/opendoc/notify3/naksvv
 * - 授权完成回调: https://open.esign.cn/doc/opendoc/notify3/tme3qi
 *
 * @author 芋道源码
 */
@RestController
@RequestMapping("/enterprise/esign/callback")
@RequiredArgsConstructor
@Slf4j
public class EsignCallbackController {

    private final EsignAuthFlowService authFlowService;
    private final EsignProperties esignProperties;

    /**
     * 实名认证回调接口
     * 当用户完成实名认证后，e签宝会调用此接口通知认证结果
     */
    @PostMapping("/identity-verify")
    public CommonResult<String> identityVerifyCallback(@RequestBody String requestBody, 
                                                      HttpServletRequest request) {
        log.info("收到e签宝实名认证回调: {}", requestBody);
        
        try {
            // 验证签名（如果启用了RSA签名）
            if (esignProperties.useRsaSignature() && !verifyCallback(request, requestBody)) {
                log.error("e签宝实名认证回调签名验证失败");
                return CommonResult.error(400, "签名验证失败");
            }
            
            // 解析回调数据
            Map<String, Object> callbackData = JSONUtil.toBean(requestBody, Map.class);
            String action = (String) callbackData.get("action");
            Map<String, Object> data = (Map<String, Object>) callbackData.get("data");
            
            if (!"IDENTITY_VERIFY".equals(action)) {
                log.warn("未知的回调动作: {}", action);
                return CommonResult.error(400, "未知的回调动作");
            }
            
            // 处理认证回调
            handleIdentityVerifyCallback(data, requestBody);
            
            return CommonResult.success("处理成功");
        } catch (Exception e) {
            log.error("处理e签宝实名认证回调失败", e);
            return CommonResult.error(500, "处理回调失败: " + e.getMessage());
        }
    }

    /**
     * 授权完成回调接口
     * 当用户完成授权后，e签宝会调用此接口通知授权结果
     */
    @PostMapping("/authorize-finish")
    public CommonResult<String> authorizeFinishCallback(@RequestBody String requestBody,
                                                       HttpServletRequest request) {
        log.info("收到e签宝授权完成回调: {}", requestBody);
        
        try {
            // 验证签名（如果启用了RSA签名）
            if (esignProperties.useRsaSignature() && !verifyCallback(request, requestBody)) {
                log.error("e签宝授权完成回调签名验证失败");
                return CommonResult.error(400, "签名验证失败");
            }
            
            // 解析回调数据
            Map<String, Object> callbackData = JSONUtil.toBean(requestBody, Map.class);
            String action = (String) callbackData.get("action");
            Map<String, Object> data = (Map<String, Object>) callbackData.get("data");
            
            if (!"AUTHORIZE_FINISH".equals(action)) {
                log.warn("未知的回调动作: {}", action);
                return CommonResult.error(400, "未知的回调动作");
            }
            
            // 处理授权回调
            handleAuthorizeFinishCallback(data, requestBody);
            
            return CommonResult.success("处理成功");
        } catch (Exception e) {
            log.error("处理e签宝授权完成回调失败", e);
            return CommonResult.error(500, "处理回调失败: " + e.getMessage());
        }
    }

    /**
     * 处理实名认证回调
     */
    private void handleIdentityVerifyCallback(Map<String, Object> data, String originalData) {
        String authFlowId = (String) data.get("authFlowId");
        String verifyStatus = (String) data.get("verifyStatus");
        
        if (authFlowId == null) {
            log.warn("回调数据中缺少authFlowId");
            return;
        }
        
        log.info("处理实名认证回调: authFlowId={}, verifyStatus={}", authFlowId, verifyStatus);
        
        // 根据认证状态更新数据库
        if ("VERIFIED".equals(verifyStatus)) {
            // 认证成功
            String orgId = (String) data.get("orgId");
            String personId = (String) data.get("psnId");
            authFlowService.updateNotifyStatus(authFlowId, originalData, orgId, personId);
        } else {
            // 认证失败
            String errorMessage = data.get("errorMessage") != null ? (String) data.get("errorMessage") : "认证失败";
            authFlowService.updateAuthStatus(authFlowId, 3, errorMessage); // 3=认证失败
        }
    }

    /**
     * 处理授权完成回调
     */
    private void handleAuthorizeFinishCallback(Map<String, Object> data, String originalData) {
        String authFlowId = (String) data.get("authFlowId");
        String authorizeStatus = (String) data.get("authorizeStatus");
        
        if (authFlowId == null) {
            log.warn("回调数据中缺少authFlowId");
            return;
        }
        
        log.info("处理授权完成回调: authFlowId={}, authorizeStatus={}", authFlowId, authorizeStatus);
        
        // 根据授权状态更新数据库
        if ("AUTHORIZED".equals(authorizeStatus)) {
            // 授权成功
            String orgId = (String) data.get("orgId");
            String personId = (String) data.get("psnId");
            authFlowService.updateNotifyStatus(authFlowId, originalData, orgId, personId);
        } else {
            // 授权失败
            String errorMessage = data.get("errorMessage") != null ? (String) data.get("errorMessage") : "授权失败";
            authFlowService.updateAuthStatus(authFlowId, 3, errorMessage); // 3=认证失败
        }
    }

    /**
     * 验证回调签名
     */
    private boolean verifyCallback(HttpServletRequest request, String requestBody) {
        try {
            String method = request.getMethod();
            String accept = request.getHeader("Accept");
            String contentMd5 = request.getHeader("Content-MD5");
            String contentType = request.getHeader("Content-Type");
            String date = request.getHeader("Date");
            String authorization = request.getHeader("Authorization");
            String url = request.getRequestURI();
            
            if (authorization == null || !authorization.startsWith("Algorithm=RSA256, Signature=")) {
                log.warn("回调请求缺少有效的Authorization头");
                return false;
            }
            
            String signature = authorization.substring("Algorithm=RSA256, Signature=".length());
            
            // 构建自定义请求头Map
            Map<String, String> customHeaders = Collections.emptyMap();
            
            // 验证签名
            return EsignSignatureUtils.verifySignature(
                    method, accept, contentMd5, contentType, date, 
                    customHeaders, url, signature, esignProperties.getRsaPublicKey()
            );
        } catch (Exception e) {
            log.error("验证回调签名失败", e);
            return false;
        }
    }
} 