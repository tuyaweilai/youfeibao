package cn.iocoder.yudao.module.enterprise.framework.esign.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.TreeMap;

/**
 * e签宝RSA签名工具类
 * 
 * @author 芋道源码
 */
@Slf4j
public class EsignSignatureUtils {

    private static final String ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * 生成签名
     *
     * @param method 请求方法
     * @param accept 接受类型
     * @param contentMd5 内容MD5
     * @param contentType 内容类型  
     * @param date 日期
     * @param headers 请求头
     * @param url 请求URL
     * @param privateKeyStr 私钥字符串
     * @return 签名
     */
    public static String generateSignature(String method, String accept, String contentMd5, 
                                         String contentType, String date, Map<String, String> headers, 
                                         String url, String privateKeyStr) {
        try {
            // 1. 构建待签名字符串
            String stringToSign = buildStringToSign(method, accept, contentMd5, contentType, date, headers, url);
            log.debug("待签名字符串: {}", stringToSign);
            
            // 2. 使用RSA私钥签名
            PrivateKey privateKey = getPrivateKey(privateKeyStr);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(stringToSign.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = signature.sign();
            
            // 3. Base64编码
            return Base64.encode(signBytes);
        } catch (Exception e) {
            log.error("生成签名失败", e);
            throw new RuntimeException("生成签名失败", e);
        }
    }

    /**
     * 验证签名
     *
     * @param method 请求方法
     * @param accept 接受类型
     * @param contentMd5 内容MD5
     * @param contentType 内容类型
     * @param date 日期
     * @param headers 请求头
     * @param url 请求URL
     * @param signatureStr 签名字符串
     * @param publicKeyStr 公钥字符串
     * @return 验证结果
     */
    public static boolean verifySignature(String method, String accept, String contentMd5,
                                        String contentType, String date, Map<String, String> headers,
                                        String url, String signatureStr, String publicKeyStr) {
        try {
            // 1. 构建待签名字符串
            String stringToSign = buildStringToSign(method, accept, contentMd5, contentType, date, headers, url);
            
            // 2. 使用RSA公钥验签
            PublicKey publicKey = getPublicKey(publicKeyStr);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(stringToSign.getBytes(StandardCharsets.UTF_8));
            
            // 3. 验证签名
            byte[] signBytes = Base64.decode(signatureStr);
            return signature.verify(signBytes);
        } catch (Exception e) {
            log.error("验证签名失败", e);
            return false;
        }
    }

    /**
     * 构建待签名字符串
     */
    private static String buildStringToSign(String method, String accept, String contentMd5,
                                          String contentType, String date, Map<String, String> headers, String url) {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append("\n");
        sb.append(StrUtil.nullToEmpty(accept)).append("\n");
        sb.append(StrUtil.nullToEmpty(contentMd5)).append("\n");
        sb.append(StrUtil.nullToEmpty(contentType)).append("\n");
        sb.append(StrUtil.nullToEmpty(date)).append("\n");
        
        // 添加自定义请求头（按字典序排序）
        if (headers != null && !headers.isEmpty()) {
            TreeMap<String, String> sortedHeaders = new TreeMap<>(headers);
            for (Map.Entry<String, String> entry : sortedHeaders.entrySet()) {
                String key = entry.getKey().toLowerCase();
                if (key.startsWith("x-tsign-open-")) {
                    sb.append(key).append(":").append(entry.getValue()).append("\n");
                }
            }
        }
        
        sb.append(url);
        return sb.toString();
    }

    /**
     * 获取私钥对象
     */
    private static PrivateKey getPrivateKey(String privateKeyStr) throws Exception {
        // 移除PEM格式的头尾标识
        String privateKeyContent = privateKeyStr
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] keyBytes = Base64.decode(privateKeyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 获取公钥对象
     */
    private static PublicKey getPublicKey(String publicKeyStr) throws Exception {
        // 移除PEM格式的头尾标识
        String publicKeyContent = publicKeyStr
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] keyBytes = Base64.decode(publicKeyContent);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 计算内容MD5
     */
    public static String calculateContentMd5(String content) {
        if (StrUtil.isEmpty(content)) {
            return "";
        }
        return Base64.encode(DigestUtil.md5(content.getBytes(StandardCharsets.UTF_8)));
    }
} 