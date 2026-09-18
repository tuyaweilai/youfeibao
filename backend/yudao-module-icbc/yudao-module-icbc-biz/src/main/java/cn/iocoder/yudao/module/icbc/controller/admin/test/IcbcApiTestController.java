package cn.iocoder.yudao.module.icbc.controller.admin.test;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcConnectivity;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
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
 * 仅供连通性联调与排查使用。所有调用经 {@code IcbcGateway} 端口，
 * 不暴露工行网关地址、密钥或签名细节。
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
    @Operation(summary = "适配层运行信息", description = "查看适配层运行模式（不展示密钥与网关地址）")
    @PreAuthorize("@ss.hasPermission('icbc:test:query')")
    public CommonResult<Map<String, Object>> testConfig() {
        return icbcTestService.testConfig();
    }

    @GetMapping("/connectivity")
    @Operation(summary = "真实连通性校验", description = "打一条数据接口到工行网关，验证网络与签名配置")
    @PreAuthorize("@ss.hasPermission('icbc:test:query')")
    public CommonResult<IcbcConnectivity> checkConnectivity() {
        return icbcTestService.checkConnectivity();
    }

    @GetMapping("/invoice-query")
    @Operation(summary = "发票 / 预开票信息查询", description = "经适配层调用工行预查询接口")
    @PreAuthorize("@ss.hasPermission('icbc:test:query')")
    public CommonResult<InvoiceInfo> testInvoiceQuery(
            @RequestParam(value = "outOrderId") String outOrderId,
            @RequestParam(value = "outUserId", required = false) String outUserId) {
        return icbcTestService.queryInvoiceInfo(outOrderId, outUserId);
    }

    @GetMapping("/payment-form-json")
    @Operation(summary = "支付页面表单生成测试", description = "经适配层生成工行支付页面表单 HTML")
    @PreAuthorize("@ss.hasPermission('icbc:test:query')")
    public CommonResult<String> generatePaymentFormJson(
            @RequestParam(value = "outOrderId") String outOrderId,
            @RequestParam(value = "outUserId", required = false) String outUserId) {
        return icbcTestService.generatePaymentForm(outOrderId, outUserId);
    }

}
