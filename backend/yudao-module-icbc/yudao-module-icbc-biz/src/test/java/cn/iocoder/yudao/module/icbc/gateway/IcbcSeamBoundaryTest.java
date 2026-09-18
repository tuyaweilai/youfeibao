package cn.iocoder.yudao.module.icbc.gateway;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「唯一缝」的边界测试
 *
 * 固化 issue #3 的两条验收：
 * <ol>
 *   <li>端口覆盖全部出站操作</li>
 *   <li>平台其余部分不出现工行的 SDK、网关地址、签名或加解密逻辑——
 *       这些只允许出现在 {@code gateway} 包内</li>
 * </ol>
 */
public class IcbcSeamBoundaryTest {

    /**
     * 仅允许出现在 gateway 包内的工行痕迹
     */
    private static final List<String> GATEWAY_ONLY_TOKENS = Arrays.asList(
            "com.icbc.api",
            "IcbcProperties",
            "DefaultIcbcClient",
            "UiIcbcClient",
            "gw.open.icbc.com.cn",
            "buildPostForm",
            "privateKey",
            "aesKey",
            "sm2PrivateKey");

    @Test
    public void testGatewayCoversAllOutboundOperations() {
        List<String> methods = Arrays.stream(IcbcGateway.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toList());

        assertEquals(true, methods.containsAll(Arrays.asList(
                "submitPayeeOnboarding",
                "queryPayeeOnboarding",
                "submitEnterpriseAuthorization",
                "submitPreOrder",
                "queryInvoiceInfo",
                "submitPayment",
                "downloadInvoice",
                "cancelInvoice",
                "applyRedInvoice",
                "revokeRedInvoice",
                "checkConnectivity")));
    }

    @Test
    public void testNoIcbcTracesOutsideGatewayPackage() throws IOException {
        List<Path> roots = Arrays.asList(
                Path.of("src/main/java"),
                Path.of("../yudao-module-icbc-api/src/main/java"));
        List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                    if (path.toString().contains("/gateway/")) {
                        continue;
                    }
                    String source = new String(Files.readAllBytes(path));
                    for (String token : GATEWAY_ONLY_TOKENS) {
                        if (source.contains(token)) {
                            violations.add(path + " -> " + token);
                        }
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), "工行痕迹越出 gateway 包：" + violations);
    }

}
