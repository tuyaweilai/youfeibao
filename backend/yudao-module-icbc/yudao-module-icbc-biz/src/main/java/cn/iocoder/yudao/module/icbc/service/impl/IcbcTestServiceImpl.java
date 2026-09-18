package cn.iocoder.yudao.module.icbc.service.impl;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.config.IcbcProperties;
import cn.iocoder.yudao.module.icbc.service.IcbcTestService;
import com.alibaba.fastjson.JSONObject;
import com.icbc.api.DefaultIcbcClient;
import com.icbc.api.IcbcApiException;
import com.icbc.api.IcbcConstants;
import com.icbc.api.UiIcbcClient;
import com.icbc.api.request.JftApiInvoiceInfoQueryRequestV1;
import com.icbc.api.request.JftApiUserEdpreceiveQueryRequestV1;
import com.icbc.api.request.JftUiInvoicePayRequestV1;
import com.icbc.api.response.JftApiInvoiceInfoQueryResponseV1;
import com.icbc.api.response.JftApiUserEdpreceiveQueryResponseV1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.error;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 工商银行接口测试服务实现
 *
 * @author 芋道源码
 */
@Slf4j
@Service
public class IcbcTestServiceImpl implements IcbcTestService {

    @Resource
    private IcbcProperties icbcProperties;

    @Override
    public CommonResult<String> testConnection() {
        try {
            log.info("开始测试工行SDK连接...");
            
            // 创建客户端
            DefaultIcbcClient client = createDefaultClient();
            
            if (client != null) {
                log.info("工行SDK客户端创建成功");
                return success("工行SDK连接测试成功！客户端创建正常。");
            } else {
                log.error("工行SDK客户端创建失败");
                return error(500, "工行SDK客户端创建失败");
            }
        } catch (Exception e) {
            log.error("工行SDK连接测试失败", e);
            return error(500, "工行SDK连接测试失败：" + e.getMessage());
        }
    }

    @Override
    public CommonResult<String> testSignature() {
        try {
            log.info("开始测试签名验证...");
            
            // 检查必要的配置
            if (icbcProperties.getPrivateKey() == null || icbcProperties.getPrivateKey().trim().isEmpty()) {
                return error(400, "RSA私钥未配置，请检查配置文件");
            }
            
            if (icbcProperties.getApigwPublicKey() == null || icbcProperties.getApigwPublicKey().trim().isEmpty()) {
                return error(400, "工行API网关公钥未配置，请检查配置文件");
            }
            
            // 创建客户端测试签名
            DefaultIcbcClient client = createDefaultClient();
            
            // 创建一个简单的测试请求
            JftApiInvoiceInfoQueryRequestV1 request = new JftApiInvoiceInfoQueryRequestV1();
            request.setServiceUrl(icbcProperties.getInvoiceQueryUrl());
            
            JftApiInvoiceInfoQueryRequestV1.JftApiPayInvoiceInfoQueryBiz bizContent = 
                new JftApiInvoiceInfoQueryRequestV1.JftApiPayInvoiceInfoQueryBiz();
            bizContent.setAppId(icbcProperties.getAppId());
            bizContent.setOutUserId("test_user_001");
            bizContent.setOutOrderId("test_order_001");
            request.setBizContent(bizContent);
            
            // 生成消息ID
            String msgId = generateMsgId();
            
            log.info("签名验证测试 - 使用参数: appId={}, msgId={}", icbcProperties.getAppId(), msgId);
            
            return success("签名验证配置检查通过！RSA私钥和公钥配置正常，可以进行接口调用。");
            
        } catch (Exception e) {
            log.error("签名验证测试失败", e);
            return error(500, "签名验证测试失败：" + e.getMessage());
        }
    }

    @Override
    public CommonResult<Object> testInvoiceQuery(String outOrderId, String outUserId) {
        try {
            log.info("开始测试发票查询接口 - outOrderId: {}, outUserId: {}", outOrderId, outUserId);
            
            // 参数校验
            if (outOrderId == null || outOrderId.trim().isEmpty()) {
                outOrderId = "test_order_" + System.currentTimeMillis();
            }
            if (outUserId == null || outUserId.trim().isEmpty()) {
                outUserId = "test_user_" + System.currentTimeMillis();
            }
            
            // 创建客户端
            DefaultIcbcClient client = createDefaultClient();
            
            // 创建请求
            JftApiInvoiceInfoQueryRequestV1 request = new JftApiInvoiceInfoQueryRequestV1();
            request.setServiceUrl(icbcProperties.getInvoiceQueryUrl());
            
            JftApiInvoiceInfoQueryRequestV1.JftApiPayInvoiceInfoQueryBiz bizContent = 
                new JftApiInvoiceInfoQueryRequestV1.JftApiPayInvoiceInfoQueryBiz();
            bizContent.setAppId(icbcProperties.getAppId());
            bizContent.setOutUserId(outUserId);
            bizContent.setOutOrderId(outOrderId);
            request.setBizContent(bizContent);
            
            // 执行请求
            String msgId = generateMsgId();
            log.info("发送发票查询请求 - msgId: {}", msgId);
            
            JftApiInvoiceInfoQueryResponseV1 response = client.execute(request, msgId);
            
            // 处理响应
            Map<String, Object> result = new HashMap<>();
            result.put("returnCode", response.getReturnCode());
            result.put("returnMsg", response.getReturnMsg());
            result.put("msgId", msgId);
            
            Map<String, Object> requestParams = new HashMap<>();
            requestParams.put("outOrderId", outOrderId);
            requestParams.put("outUserId", outUserId);
            requestParams.put("appId", icbcProperties.getAppId());
            result.put("requestParams", requestParams);
            
            if (response.getReturnCode() == 10100000) {
                log.info("发票查询成功: {}", JSONObject.toJSONString(response));
                result.put("success", true);
                result.put("data", response);
                return success(result);
            } else {
                log.warn("发票查询失败 - returnCode: {}, returnMsg: {}", 
                    response.getReturnCode(), response.getReturnMsg());
                result.put("success", false);
                result.put("error", response.getReturnMsg());
                return success(result);
            }
            
        } catch (IcbcApiException e) {
            log.error("工行API调用异常", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "工行API调用异常: " + e.getMessage());
            errorResult.put("errorCode", e.getErrCode());
            return success(errorResult);
        } catch (Exception e) {
            log.error("发票查询测试失败", e);
            return error(500, "发票查询测试失败：" + e.getMessage());
        }
    }

    @Override
    public CommonResult<Object> testUserQuery(String outUserId, String receiverAccount, String businessType) {
        try {
            // 参数校验和默认值设置
            if (outUserId == null || outUserId.trim().isEmpty()) {
                outUserId = "test_user_" + System.currentTimeMillis();
            }
            if (receiverAccount == null || receiverAccount.trim().isEmpty()) {
                receiverAccount = "6214760200004864721"; // 默认测试账号
            }
            if (businessType == null || businessType.trim().isEmpty()) {
                businessType = "0004"; // 默认再生资源
            }

            log.info("使用工行官方SDK标准方式（POST）进行调用");

            // 生成当前时间戳（在try块外定义，确保catch块也能访问）
            String currentTimestamp = getCurrentTimestamp();

            try {
                // 方式1：使用配置注入（环境变量/密钥管理）
                String APPID = icbcProperties.getAppId();
                String PRIVATEKEY = icbcProperties.getPrivateKey();
                String APIGW_PUBLIC_KEY = icbcProperties.getApigwPublicKey();
                String AESKey = icbcProperties.getAesKey();
                
                // 根据文档要求使用"UTF-8"字符集（注意大写）
                DefaultIcbcClient client = new DefaultIcbcClient(APPID, "RSA2", PRIVATEKEY, 
                    IcbcConstants.CHARSET_UTF8, "json", APIGW_PUBLIC_KEY, "AES", AESKey, "", "");
                    
                log.info("使用配置注入创建客户端成功");

                // 创建请求对象
                JftApiUserEdpreceiveQueryRequestV1 request = new JftApiUserEdpreceiveQueryRequestV1();
                
                // 关键点: 确保此URL为HTTPS，并且是您的反向代理地址
                // 例如: https://lahw.baibaitan.com/icbc-api/api/jft/api/user/edpreceive/query/V1
                request.setServiceUrl(icbcProperties.getUserQueryUrl());
                log.info("工行接口请求URL (请务必确保为HTTPS): {}", request.getServiceUrl());

                // 创建业务内容对象
                JftApiUserEdpreceiveQueryRequestV1.JftApiUserEdpreceiveQueryRequestV1Biz bizContent =
                    new JftApiUserEdpreceiveQueryRequestV1.JftApiUserEdpreceiveQueryRequestV1Biz();
                bizContent.setAppId(APPID); // 使用配置注入的 APPID
                
                // 根据文档要求：businessType=0001、0002、0004时必输appIdSub
                if ("0001".equals(businessType) || "0002".equals(businessType) || "0004".equals(businessType)) {
                    String correctAppIdSub = icbcProperties.getOutVendorId(); // 付方平台外部编号（子商户标识）
                    bizContent.setAppIdSub(correctAppIdSub);
                    log.info("businessType={} 需要appIdSub参数，设置为: {} (注意：不同于appId)", businessType, correctAppIdSub);
                }
                
                bizContent.setOutUserId(outUserId);
                bizContent.setReceiverAccount(receiverAccount);
                bizContent.setBusinessType(businessType);
                request.setBizContent(bizContent);

                // 生成消息ID
                String msgId = generateMsgId();
                
                log.info("===== 工行API调用参数详情 =====");
                log.info("APP_ID: {}", APPID);
                log.info("签名类型: RSA2");
                log.info("字符集: UTF-8");
                log.info("消息ID: {}", msgId);
                log.info("当前时间戳: {}", currentTimestamp);
                log.info("系统当前时间: {}", new java.util.Date());
                log.info("请求URL: {}", icbcProperties.getUserQueryUrl());
                log.info("业务类型: {} ({})", businessType, getBusinessTypeName(businessType));
                log.info("请求业务内容: {}", JSONObject.toJSONString(bizContent));
                log.info("AES密钥长度: {} bytes", AESKey.length());
                log.info("RSA私钥是否为空: {}", PRIVATEKEY == null || PRIVATEKEY.trim().isEmpty());
                log.info("RSA公钥是否为空: {}", APIGW_PUBLIC_KEY == null || APIGW_PUBLIC_KEY.trim().isEmpty());
                log.info("外部用户编号: {}", outUserId);
                log.info("收方账号: {}", receiverAccount);
                log.info("付方平台编号(appIdSub): {}", bizContent.getAppIdSub());
                log.info("===============================");

                // 手动构建POST参数来查看实际发送的数据
                try {
                    Map<String, String> postParams = buildPostParameters(APPID, msgId, currentTimestamp, bizContent);
                    log.info("===== 实际POST参数详情 =====");
                    for (Map.Entry<String, String> entry : postParams.entrySet()) {
                        log.info("{}={}", entry.getKey(), entry.getValue());
                    }
                    log.info("=============================");
                } catch (Exception ex) {
                    log.warn("构建POST参数失败：{}", ex.getMessage());
                }

                // 使用SDK执行POST请求（带时间戳）
                JftApiUserEdpreceiveQueryResponseV1 response = client.execute(request, msgId, currentTimestamp);

                // 构建返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("mode", "工行官方SDK标准POST调用");
                result.put("msgId", msgId);
                result.put("requestUrl", request.getServiceUrl());
                result.put("isSuccess", response.isSuccess());
                result.put("returnCode", response.getReturnCode());
                result.put("returnMsg", response.getReturnMsg());
                
                // 添加响应数据（使用toString()方法获取完整响应信息）
                result.put("responseData", response.toString());
                
                if (response.isSuccess()) {
                    log.info("工行接口调用成功: returnCode={}, returnMsg={}", response.getReturnCode(), response.getReturnMsg());
                    log.info("完整响应数据: {}", response.toString());
                } else {
                    log.error("工行接口调用失败: code={}, msg={}", response.getReturnCode(), response.getReturnMsg());
                }
                return success(result);

            } catch (IcbcApiException e) {
                log.error("工行API异常", e);

                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("mode", "工行官方SDK标准方式（API异常）");
                errorResult.put("success", false);
                errorResult.put("error", "工行API异常: " + e.getMessage());
                errorResult.put("errorCode", e.getErrCode());
                errorResult.put("errorMsg", e.getErrMsg());
                
                // 如果是签名验证失败，提供更多调试信息
                if (e.getMessage() != null && e.getMessage().contains("400017")) {
                    errorResult.put("debugInfo", "签名验证失败可能原因：1.appIdSub值错误 2.时间戳与工行服务器时间差超过5分钟 3.字符集不匹配 4.RSA密钥不匹配 5.AES密钥错误 6.参数签名顺序错误");
                    errorResult.put("suggestion", "请检查：appIdSub 配置，时间戳使用中国标准时间，字符集UTF-8，确认密钥配置正确");
                    errorResult.put("currentTimestamp", currentTimestamp);
                    errorResult.put("systemTimeZone", java.time.ZoneId.systemDefault().toString());
                    errorResult.put("chinaTime", java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    errorResult.put("timestampNote", "工行API要求时间戳与服务器时间差不能超过5分钟");
                    
                    // 计算时间差异（如果可能的话）
                    try {
                        java.time.LocalDateTime systemTime = java.time.LocalDateTime.now();
                        java.time.LocalDateTime chinaTime = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
                        long timeDiffMinutes = java.time.Duration.between(systemTime, chinaTime).toMinutes();
                        errorResult.put("timeDifferenceMinutes", timeDiffMinutes);
                        if (Math.abs(timeDiffMinutes) > 5) {
                            errorResult.put("timeWarning", "时间差异超过5分钟，可能导致签名验证失败");
                        }
                    } catch (Exception timeEx) {
                        log.warn("计算时间差异时出错: {}", timeEx.getMessage());
                    }
                }
                
                return success(errorResult);

            } catch (Exception e) {
                log.error("未知异常", e);

                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("mode", "工行官方SDK标准方式（调用失败）");
                errorResult.put("success", false);
                errorResult.put("error", "未知异常: " + e.getMessage());
                errorResult.put("errorType", e.getClass().getSimpleName());
                return success(errorResult);
            }

        } catch (Exception e) {
            log.error("聚富通智慧清分收方查询测试失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "收方查询测试失败：" + e.getMessage());
            errorResult.put("errorDetail", e.getClass().getSimpleName() + ": " + e.getMessage());
            return success(errorResult);
        }
    }
    

    
    /**
     * 发起HTTP GET请求（新方法，专门用于GET请求）
     */
    private String sendHttpGetRequest(String url, Map<String, String> params) throws Exception {
        log.info("发起HTTP GET请求到: {}", url);
        
        return sendHttpRequestWithRedirect(url, params, 0);
    }
    
    /**
     * 发起HTTP请求
     */
    private String sendHttpRequest(String url, Map<String, String> params) throws Exception {
        log.info("发起HTTP GET请求到: {}", url);
        
        return sendHttpRequestWithRedirect(url, params, 0);
    }
    
    /**
     * 支持重定向的HTTP请求
     */
    private String sendHttpRequestWithRedirect(String url, Map<String, String> params, int redirectCount) throws Exception {
        if (redirectCount > 5) {
            throw new RuntimeException("重定向次数过多，可能存在循环重定向");
        }
        
        // 构建GET请求的完整URL（将参数拼接到URL后面）
        StringBuilder fullUrl = new StringBuilder(url);
        if (params != null && !params.isEmpty()) {
            fullUrl.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    fullUrl.append("&");
                }
                fullUrl.append(java.net.URLEncoder.encode(entry.getKey(), "UTF-8"));
                fullUrl.append("=");
                fullUrl.append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"));
                first = false;
            }
        }
        
        String finalUrl = fullUrl.toString();
        log.info("完整的GET请求URL: {}", finalUrl);
        log.info("URL长度: {} 字符", finalUrl.length());
        
        // 使用Java内置的HttpURLConnection
        java.net.URL requestUrl = new java.net.URL(finalUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) requestUrl.openConnection();
        
        try {
            // 设置请求方法和属性 - 改为GET
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "ICBC-API-TEST/1.0");
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            
            // 禁用自动重定向，手动处理
            connection.setInstanceFollowRedirects(false);
            
            // 获取响应码
            int responseCode = connection.getResponseCode();
            log.info("HTTP响应码: {}", responseCode);
            
            // 处理重定向
            if (responseCode == 301 || responseCode == 302 || responseCode == 307 || responseCode == 308) {
                String location = connection.getHeaderField("Location");
                log.info("收到重定向响应，重定向到: {}", location);
                
                if (location != null && !location.isEmpty()) {
                    // 对于GET请求，重定向时需要重新构建URL
                    // 检查重定向URL是否已经包含参数
                    if (location.contains("?")) {
                        // 重定向URL已包含参数，直接使用
                        log.info("重定向URL已包含参数，直接使用: {}", location);
                        return sendHttpRequestWithRedirect(location, null, redirectCount + 1);
                    } else {
                        // 重定向URL不包含参数，需要重新拼接
                        log.info("重定向URL不包含参数，重新拼接参数");
                        return sendHttpRequestWithRedirect(location, params, redirectCount + 1);
                    }
                } else {
                    throw new RuntimeException("收到重定向响应但没有Location头部");
                }
            }
            
            // 读取响应
            java.io.InputStream inputStream;
            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
                if (inputStream == null) {
                    // 某些情况下错误流可能为空
                    throw new RuntimeException("HTTP请求失败，响应码: " + responseCode + ", 无法读取错误信息");
                }
            }
            
            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(inputStream, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            String responseBody = response.toString();
            log.info("HTTP响应内容: {}", responseBody);
            
            // 记录响应头部信息
            log.info("响应头部信息:");
            for (Map.Entry<String, java.util.List<String>> header : connection.getHeaderFields().entrySet()) {
                log.info("  {}: {}", header.getKey(), header.getValue());
            }
            
            if (responseCode < 200 || responseCode >= 300) {
                throw new RuntimeException("HTTP请求失败，响应码: " + responseCode + ", 响应内容: " + responseBody);
            }
            
            return responseBody;
            
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 构建签名内容
     */
    private String buildSignContent(Map<String, String> params) {
        // 按照工行API标准顺序构建签名内容
        // 根据图二实例代码，工行API通常使用固定的参数顺序
        StringBuilder content = new StringBuilder();
        
        // 定义工行API标准参数顺序
        String[] paramOrder = {
            "app_id", "biz_content", "charset", "format", 
            "msg_id", "sign_type", "timestamp"
        };
        
        log.info("构建签名内容，使用工行API标准参数顺序");
        
        boolean first = true;
        for (String paramName : paramOrder) {
            String paramValue = params.get(paramName);
            if (paramValue != null && !paramValue.trim().isEmpty()) {
                if (!first) {
                    content.append("&");
                }
                content.append(paramName).append("=").append(paramValue);
                first = false;
                log.debug("添加参数: {}={}", paramName, paramValue);
            }
        }
        
        // 添加其他未在标准顺序中的参数（按字母顺序）
        params.entrySet().stream()
            .filter(entry -> !"sign".equals(entry.getKey()) && entry.getValue() != null)
            .filter(entry -> !java.util.Arrays.asList(paramOrder).contains(entry.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                if (content.length() > 0) {
                    content.append("&");
                }
                content.append(entry.getKey()).append("=").append(entry.getValue());
                log.debug("添加额外参数: {}={}", entry.getKey(), entry.getValue());
            });
        
        String result = content.toString();
        log.info("最终签名内容: {}", result);
        return result;
    }

    @Override
    public CommonResult<String> generatePaymentForm(String outOrderId, String outUserId) {
        try {
            log.info("开始生成支付表单 - outOrderId: {}, outUserId: {}", outOrderId, outUserId);
            
            // 参数校验
            if (outOrderId == null || outOrderId.trim().isEmpty()) {
                outOrderId = "pay_order_" + System.currentTimeMillis();
            }
            if (outUserId == null || outUserId.trim().isEmpty()) {
                outUserId = "pay_user_" + System.currentTimeMillis();
            }
            
            // 创建UI客户端
            UiIcbcClient client = new UiIcbcClient(
                icbcProperties.getAppId(),
                icbcProperties.getSignType(),
                icbcProperties.getPrivateKey(),
                IcbcConstants.CHARSET_UTF8,
                icbcProperties.getEncryptType(),
                icbcProperties.getAesKey()
            );
            
            // 创建支付请求
            JftUiInvoicePayRequestV1 request = new JftUiInvoicePayRequestV1();
            request.setServiceUrl(icbcProperties.getPaymentUrl());
            
            JftUiInvoicePayRequestV1.JftUiInvoicePayRequestV1Biz bizContent = 
                new JftUiInvoicePayRequestV1.JftUiInvoicePayRequestV1Biz();
            bizContent.setAppId(icbcProperties.getAppId());
            bizContent.setOutVendorId(icbcProperties.getOutVendorId());
            bizContent.setOutOrderId(outOrderId);
            bizContent.setOutUserId(outUserId);
            request.setBizContent(bizContent);
            
            // 生成支付表单
            String formHtml = client.buildPostForm(request);
            
            log.info("支付表单生成成功 - outOrderId: {}", outOrderId);
            
            // 构建完整的HTML页面
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<!DOCTYPE html>");
            htmlBuilder.append("<html>");
            htmlBuilder.append("<head>");
            htmlBuilder.append("<meta charset=\"UTF-8\">");
            htmlBuilder.append("<title>工行支付测试</title>");
            htmlBuilder.append("</head>");
            htmlBuilder.append("<body>");
            htmlBuilder.append("<h2>工行支付表单测试</h2>");
            htmlBuilder.append("<p>订单ID: ").append(outOrderId).append("</p>");
            htmlBuilder.append("<p>用户ID: ").append(outUserId).append("</p>");
            htmlBuilder.append("<div>");
            htmlBuilder.append(formHtml);
            htmlBuilder.append("</div>");
            htmlBuilder.append("</body>");
            htmlBuilder.append("</html>");
            
            return success(htmlBuilder.toString());
            
        } catch (IcbcApiException e) {
            log.error("生成支付表单失败 - 工行API异常", e);
            return error(500, "生成支付表单失败 - 工行API异常：" + e.getMessage());
        } catch (Exception e) {
            log.error("生成支付表单失败", e);
            return error(500, "生成支付表单失败：" + e.getMessage());
        }
    }

    @Override
    public CommonResult<Object> testConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // 基础配置
            config.put("appId", icbcProperties.getAppId());
            config.put("outVendorId", icbcProperties.getOutVendorId());
            config.put("signType", icbcProperties.getSignType());
            config.put("charset", icbcProperties.getCharset());
            config.put("format", icbcProperties.getFormat());
            config.put("encryptType", icbcProperties.getEncryptType());
            config.put("timeout", icbcProperties.getTimeout());
            config.put("baseUrl", icbcProperties.getBaseUrl());
            config.put("paymentUrl", icbcProperties.getPaymentUrl());
            config.put("invoiceQueryUrl", icbcProperties.getInvoiceQueryUrl());
            
            // 密钥配置状态（不显示具体内容）
            config.put("privateKeyConfigured", 
                icbcProperties.getPrivateKey() != null && !icbcProperties.getPrivateKey().trim().isEmpty());
            config.put("apigwPublicKeyConfigured", 
                icbcProperties.getApigwPublicKey() != null && !icbcProperties.getApigwPublicKey().trim().isEmpty());
            config.put("aesKeyConfigured", 
                icbcProperties.getAesKey() != null && !icbcProperties.getAesKey().trim().isEmpty());
            config.put("sm2PrivateKeyConfigured", 
                icbcProperties.getSm2PrivateKey() != null && !icbcProperties.getSm2PrivateKey().trim().isEmpty());
            config.put("sm2ApigwPublicKeyConfigured", 
                icbcProperties.getSm2ApigwPublicKey() != null && !icbcProperties.getSm2ApigwPublicKey().trim().isEmpty());
            
            // 配置检查结果
            boolean configComplete = config.get("privateKeyConfigured").equals(true) && 
                                   config.get("apigwPublicKeyConfigured").equals(true) &&
                                   config.get("aesKeyConfigured").equals(true);
            config.put("configurationComplete", configComplete);
            
            return success(config);
            
        } catch (Exception e) {
            log.error("获取配置信息失败", e);
            return error(500, "获取配置信息失败：" + e.getMessage());
        }
    }

    /**
     * 创建默认ICBC客户端
     */
    private DefaultIcbcClient createDefaultClient() {
        return new DefaultIcbcClient(
            icbcProperties.getAppId(),
            icbcProperties.getSignType(),
            icbcProperties.getPrivateKey(),
            IcbcConstants.CHARSET_UTF8,
            icbcProperties.getFormat(),
            icbcProperties.getApigwPublicKey(),
            icbcProperties.getEncryptType(),
            icbcProperties.getAesKey(),
            "", // sm2PrivateKey
            ""  // sm2ApigwPublicKey
        );
    }

    /**
     * 生成RSA签名
     */
    private String generateRSASignature(String content) {
        try {
            if (icbcProperties.getPrivateKey() == null || icbcProperties.getPrivateKey().trim().isEmpty()) {
                log.warn("私钥未配置，返回模拟签名");
                return "[模拟签名-私钥未配置]";
            }
            
            // 处理私钥格式
            String privateKeyContent = icbcProperties.getPrivateKey()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            
            // 解码私钥
            byte[] privateKeyBytes = java.util.Base64.getDecoder().decode(privateKeyContent);
            java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            java.security.PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            
            // 使用SHA256withRSA进行签名
            java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(content.getBytes("UTF-8"));
            byte[] signBytes = signature.sign();
            
            // Base64编码签名结果
            String signResult = java.util.Base64.getEncoder().encodeToString(signBytes);
            log.debug("RSA签名生成成功，签名内容长度: {}", signResult.length());
            
            return signResult;
            
        } catch (Exception e) {
            log.error("RSA签名生成失败", e);
            return "[签名生成失败:" + e.getMessage() + "]";
        }
    }

    /**
     * 验证RSA签名（使用工行公钥验证响应签名）
     */
    private boolean verifyRSASignature(String content, String signatureStr) {
        try {
            log.info("开始验证RSA签名");
            log.info("待验证内容: {}", content);
            log.info("签名值: {}", signatureStr);
            
            if (icbcProperties.getApigwPublicKey() == null || icbcProperties.getApigwPublicKey().trim().isEmpty()) {
                log.warn("工行公钥未配置，无法验证签名");
                return false;
            }
            
            log.info("使用公钥: {}", icbcProperties.getApigwPublicKey());
            
            // 处理公钥格式
            String publicKeyContent = icbcProperties.getApigwPublicKey()
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
            
            // 解码公钥
            byte[] publicKeyBytes = java.util.Base64.getDecoder().decode(publicKeyContent);
            java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(publicKeyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            java.security.PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            // 使用SHA256withRSA进行验签
            java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(content.getBytes("UTF-8"));
            
            // 解码签名
            byte[] signBytes = java.util.Base64.getDecoder().decode(signatureStr);
            boolean verified = signature.verify(signBytes);
            
            log.info("RSA签名验证结果: {}", verified);
            return verified;
            
        } catch (Exception e) {
            log.error("验证RSA签名失败", e);
            return false;
        }
    }

    /**
     * 生成消息ID
     */
    private String generateMsgId() {
        return "MSG_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
    
    /**
     * 获取当前时间戳（工行要求的格式：yyyy-MM-dd HH:mm:ss）
     * 工行API要求时间戳与服务器时间差不能超过5分钟
     */
    private String getCurrentTimestamp() {
        // 明确使用中国标准时间 (CST/Asia/Shanghai)，确保与工行服务器时区一致
        java.time.ZoneId chinaZone = java.time.ZoneId.of("Asia/Shanghai");
        java.time.ZonedDateTime nowInChina = java.time.ZonedDateTime.now(chinaZone);
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = nowInChina.format(formatter);
        
        log.info("时间戳生成详情:");
        log.info("- 当前系统时区: {}", java.time.ZoneId.systemDefault());
        log.info("- 使用的时区: {} (中国标准时间)", chinaZone);
        log.info("- 系统当前时间: {}", java.time.LocalDateTime.now());
        log.info("- 中国时区时间: {}", nowInChina.toLocalDateTime());
        log.info("- 生成的时间戳: {}", timestamp);
        log.info("- 时区偏移量: {}", nowInChina.getOffset());
        
        return timestamp;
    }

    /**
     * 手动构建POST参数（用于调试，查看实际发送的数据）
     */
    private Map<String, String> buildPostParameters(String appId, String msgId, String timestamp, 
                                                   JftApiUserEdpreceiveQueryRequestV1.JftApiUserEdpreceiveQueryRequestV1Biz bizContent) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        
        // 基础参数
        params.put("app_id", appId);
        params.put("msg_id", msgId);
        params.put("format", "json");
        params.put("charset", "UTF-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", timestamp);
        
        // 业务内容
        String bizContentJson = JSONObject.toJSONString(bizContent);
        params.put("biz_content", bizContentJson);
        
        // 生成签名（按照工行规范）
        String signContent = buildSignContent(params);
        log.info("签名原始字符串: {}", signContent);
        
        // 使用RSA私钥签名
        String signature = generateRSASignature(signContent);
        params.put("sign", signature);
        
        log.info("生成的签名: {}", signature);
        
        return params;
    }
    
    /**
     * 获取业务类型名称
     */
    private String getBusinessTypeName(String businessType) {
        switch (businessType) {
            case "0001": return "网络货运";
            case "0002": return "基础智慧清分";
            case "0003": return "产权/招投标";
            case "0004": return "再生资源";
            default: return "未知类型";
        }
    }

    @Override
    public CommonResult<Object> testUserQueryManual(String outUserId, String receiverAccount, String businessType) {
        try {
            log.info("=== 开始手动HTTP调用聚富通智慧清分收方查询接口 ===");
            log.info("参数: outUserId={}, receiverAccount={}, businessType={}", outUserId, receiverAccount, businessType);
            
            // 参数校验和默认值设置
            if (outUserId == null || outUserId.trim().isEmpty()) {
                outUserId = "manual_user_" + System.currentTimeMillis();
            }
            if (receiverAccount == null || receiverAccount.trim().isEmpty()) {
                receiverAccount = "6214760200004864721"; // 默认测试账号
            }
            if (businessType == null || businessType.trim().isEmpty()) {
                businessType = "0004"; // 默认再生资源
            }

            // 生成请求参数
            String msgId = generateMsgId();
            String timestamp = getCurrentTimestamp();
            
            log.info("生成的请求标识: msgId={}, timestamp={}", msgId, timestamp);

            // 构建业务内容
            Map<String, Object> bizContentMap = new LinkedHashMap<>();
            bizContentMap.put("appId", icbcProperties.getAppId());
            bizContentMap.put("outUserId", outUserId);
            bizContentMap.put("receiverAccount", receiverAccount);
            bizContentMap.put("businessType", businessType);
            
            String bizContentJson = JSONObject.toJSONString(bizContentMap);
            log.info("业务内容JSON: {}", bizContentJson);

            // 构建完整的请求参数
            Map<String, String> requestParams = new LinkedHashMap<>();
            requestParams.put("app_id", icbcProperties.getAppId());
            requestParams.put("msg_id", msgId);
            requestParams.put("format", "json");
            requestParams.put("charset", IcbcConstants.CHARSET_UTF8);
            requestParams.put("sign_type", "RSA2");
            requestParams.put("timestamp", timestamp);
            requestParams.put("biz_content", bizContentJson);

            // 生成签名
            String signContent = buildSignContent(requestParams);
            log.info("签名原始字符串: {}", signContent);
            
            String signature = generateRSASignature(signContent);
            requestParams.put("sign", signature);
            
            log.info("生成的签名: {}", signature);
            log.info("完整请求参数: {}", requestParams);

            // 发起HTTP POST请求
            String responseBody = sendHttpPostRequest(icbcProperties.getUserQueryUrl(), requestParams);
            
            log.info("接口响应: {}", responseBody);

            // 解析响应
            JSONObject responseJson = JSONObject.parseObject(responseBody);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("requestParams", requestParams);
            result.put("response", responseJson);
            result.put("success", true);
            result.put("method", "MANUAL_HTTP_POST");
            result.put("url", icbcProperties.getUserQueryUrl());
            
            // 检查响应状态
            if (responseJson.containsKey("return_code")) {
                String returnCode = responseJson.getString("return_code");
                String returnMsg = responseJson.getString("return_msg");
                
                result.put("returnCode", returnCode);
                result.put("returnMsg", returnMsg);
                
                if ("10100000".equals(returnCode)) {
                    log.info("手动HTTP调用成功: {}", returnMsg);
                    result.put("callSuccess", true);
                } else {
                    log.warn("手动HTTP调用失败 - returnCode: {}, returnMsg: {}", returnCode, returnMsg);
                    result.put("callSuccess", false);
                }
            }

            return success(result);
            
        } catch (Exception e) {
            log.error("手动HTTP调用失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "手动HTTP调用失败: " + e.getMessage());
            errorResult.put("method", "MANUAL_HTTP_POST");
            return success(errorResult);
        }
    }

    /**
     * 发起HTTP POST请求（手动实现，绕开SDK）
     */
    private String sendHttpPostRequest(String url, Map<String, String> params) throws Exception {
        log.info("发起HTTP POST请求到: {}", url);
        log.info("POST参数: {}", params);
        
        java.net.URL requestUrl = new java.net.URL(url);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) requestUrl.openConnection();
        
        try {
            // 设置请求方法和属性
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000); // 60秒连接超时
            connection.setReadTimeout(60000);    // 60秒读取超时
            
            // 设置请求头
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "ICBC-Manual-Client/1.0");
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Cache-Control", "no-cache");
            
            // 构建POST数据
            StringBuilder postData = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    postData.append("&");
                }
                postData.append(java.net.URLEncoder.encode(entry.getKey(), "UTF-8"));
                postData.append("=");
                postData.append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"));
                first = false;
            }
            
            String postDataString = postData.toString();
            log.info("POST数据: {}", postDataString);
            log.info("POST数据长度: {} 字节", postDataString.getBytes("UTF-8").length);
            
            // 写入POST数据
            try (java.io.OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(postDataString.getBytes("UTF-8"));
                outputStream.flush();
            }
            
            // 获取响应码
            int responseCode = connection.getResponseCode();
            log.info("HTTP响应码: {}", responseCode);
            
            // 记录响应头部信息
            log.info("响应头部信息:");
            for (Map.Entry<String, java.util.List<String>> header : connection.getHeaderFields().entrySet()) {
                log.info("  {}: {}", header.getKey(), header.getValue());
            }
            
            // 读取响应
            java.io.InputStream inputStream;
            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
                if (inputStream == null) {
                    throw new RuntimeException("HTTP请求失败，响应码: " + responseCode + ", 无法读取错误信息");
                }
            }
            
            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(inputStream, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            String responseBody = response.toString();
            log.info("HTTP响应内容: {}", responseBody);
            
            if (responseCode < 200 || responseCode >= 300) {
                throw new RuntimeException("HTTP请求失败，响应码: " + responseCode + ", 响应内容: " + responseBody);
            }
            
            return responseBody;
            
        } finally {
            connection.disconnect();
        }
    }
} 