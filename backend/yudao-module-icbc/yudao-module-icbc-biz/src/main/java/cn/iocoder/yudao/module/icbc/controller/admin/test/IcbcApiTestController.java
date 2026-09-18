package cn.iocoder.yudao.module.icbc.controller.admin.test;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.config.IcbcProperties;
import cn.iocoder.yudao.module.icbc.service.IcbcTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 工商银行接口测试控制器
 * 
 * @author 芋道源码
 */
@Tag(name = "管理后台 - 工商银行接口测试")
@RestController
@RequestMapping("/icbc/test")
@Validated
@Slf4j
public class IcbcApiTestController {

    @Resource
    private IcbcTestService icbcTestService;
    
    @Resource
    private IcbcProperties icbcProperties;

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "无需权限，用于检查模块是否正常运行")
    public CommonResult<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("module", "工商银行反向开票模块");
        result.put("timestamp", LocalDateTime.now());
        return success(result);
    }

    @GetMapping("/config")
    @Operation(summary = "配置信息检查", description = "查看当前配置信息是否正确加载")
    @PreAuthorize("@ss.hasPermission('icbc:test:query')")
    public CommonResult<Object> testConfig() {
        return icbcTestService.testConfig();
    }

    @GetMapping("/connection")
    @Operation(summary = "SDK连接测试", description = "测试与工行API网关的连接")
    @PreAuthorize("@ss.hasPermission('icbc:test:query')")
    public CommonResult<String> testConnection() {
        return icbcTestService.testConnection();
    }

    @GetMapping("/signature")
    @Operation(summary = "签名验证测试", description = "验证RSA私钥和公钥配置是否正确")
    @PreAuthorize("@ss.hasPermission('icbc:test:query')")
    public CommonResult<String> testSignature() {
        return icbcTestService.testSignature();
    }

    @GetMapping("/invoice-query")
    @Operation(summary = "发票查询接口测试", description = "测试工行发票查询接口调用")
    @PreAuthorize("@ss.hasPermission('icbc:test:query')")
    public CommonResult<Object> testInvoiceQuery(
            @RequestParam(value = "outOrderId", required = false) String outOrderId,
            @RequestParam(value = "outUserId", required = false) String outUserId) {
        return icbcTestService.testInvoiceQuery(outOrderId, outUserId);
    }

    @GetMapping("/user-query")
    @Operation(summary = "聚富通智慧清分收方查询接口测试", description = "测试工行聚富通智慧清分收方查询接口调用")
    @PreAuthorize("@ss.hasPermission('icbc:test:query')")
    public CommonResult<Object> testUserQuery(
            @RequestParam(value = "outUserId", required = false) String outUserId,
            @RequestParam(value = "receiverAccount", required = false) String receiverAccount,
            @RequestParam(value = "businessType", defaultValue = "0001") String businessType) {
        return icbcTestService.testUserQuery(outUserId, receiverAccount, businessType);
    }

    @GetMapping("/user-query-manual")
    @Operation(summary = "聚富通智慧清分收方查询接口测试（手动HTTP调用）", description = "绕开官方SDK，使用手动HTTP POST方式调用工行接口")
    @PreAuthorize("@ss.hasPermission('icbc:test:query')")
    public CommonResult<Object> testUserQueryManual(
            @RequestParam(value = "outUserId", required = false) String outUserId,
            @RequestParam(value = "receiverAccount", required = false) String receiverAccount,
            @RequestParam(value = "businessType", defaultValue = "0004") String businessType) {
        return icbcTestService.testUserQueryManual(outUserId, receiverAccount, businessType);
    }

    @GetMapping("/payment-form-json")
    @Operation(summary = "支付表单生成测试(JSON格式)", description = "生成工行支付表单HTML并以JSON格式返回")
    @PreAuthorize("@ss.hasPermission('icbc:test:query')")
    public CommonResult<String> generatePaymentFormJson(
            @RequestParam(value = "outOrderId", required = false) String outOrderId,
            @RequestParam(value = "outUserId", required = false) String outUserId) {
        return icbcTestService.generatePaymentForm(outOrderId, outUserId);
    }

    @GetMapping("/urls")
    @Operation(summary = "查看URL配置", description = "显示工行接口URL配置")
    public CommonResult<Map<String, String>> getUrls() {
        Map<String, String> urls = new HashMap<>();
        urls.put("baseUrl", icbcProperties.getBaseUrl());
        urls.put("paymentUrl", icbcProperties.getPaymentUrl());
        urls.put("invoiceQueryUrl", icbcProperties.getInvoiceQueryUrl());
        urls.put("userQueryUrl", icbcProperties.getUserQueryUrl());
        return success(urls);
    }

    @GetMapping("/health-test")
    @Operation(summary = "健康检查测试", description = "无需权限，用于检查模块是否正常运行")
    public CommonResult<Map<String, Object>> healthTest() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("module", "工商银行反向开票模块");
        result.put("timestamp", LocalDateTime.now());
        result.put("baseUrl", icbcProperties.getBaseUrl());
        result.put("paymentUrl", icbcProperties.getPaymentUrl());
        result.put("invoiceQueryUrl", icbcProperties.getInvoiceQueryUrl());
        result.put("userQueryUrl", icbcProperties.getUserQueryUrl());
        return success(result);
    }
} 