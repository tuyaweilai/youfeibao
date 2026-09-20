package cn.iocoder.yudao.module.logistics;

import cn.iocoder.yudao.module.logistics.api.transport.LogisticsTransportApi;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 运费与「收购单 / 发票金额」的账本边界测试（V8 #75）。
 *
 * <p>CONTEXT.md「运费」：运费是回收企业向**承运商**支付的运输服务费用，**是另一笔账，
 * 不改变收购单金额与发票金额**。与收购单调整项里那个「运费」不是同一个东西：
 * 那是出售者货款上的加减项（ADR 0019）。这条约束靠**账本隔离**保证，而不是靠注释：
 * <ol>
 *   <li>{@code logistics-api} 是 icbc 唯一能看见的物流读取面，它**不含任何运费类型**——
 *       icbc 即便想拿运费去改收购单金额，也拿不到；</li>
 *   <li>icbc 的源码里不出现物流运费的类 / 表（防止有人日后把两个账本接起来）；</li>
 *   <li>运费 DO 里不出现收购 / 发票金额字段名（防止有人把「应付给承运商」与
 *       「给出售者的货款」混到一张表上）。</li>
 * </ol>
 *
 * <p>与 {@link LogisticsBoundaryTest}（ADR 0032）同一手法：扫描源码文本 + 反射，越界即红。
 */
public class LogisticsFreightLedgerBoundaryTest {

    @Test
    public void testReadFaceExposedToIcbcHasNoFreight() {
        List<String> violations = new ArrayList<>();
        for (Method method : LogisticsTransportApi.class.getDeclaredMethods()) {
            String signature = method.getName() + " -> " + method.getReturnType().getSimpleName();
            if (containsFreight(signature)) {
                violations.add(signature);
            }
            for (Class<?> parameterType : method.getParameterTypes()) {
                if (containsFreight(parameterType.getSimpleName())) {
                    violations.add(signature);
                }
            }
        }
        assertTrue(violations.isEmpty(),
                "logistics-api 的读取面里出现了运费类型：icbc 只能经它读运输事实，运费不该从这里漏出去。"
                        + "运费是另一笔账，不改变收购单金额与发票金额（CONTEXT.md「运费」）：" + violations);
    }

    @Test
    public void testIcbcNeverReferencesLogisticsFreight() throws IOException {
        Path icbcSources = Path.of("..", "..", "yudao-module-icbc", "yudao-module-icbc-biz",
                "src", "main", "java");
        assertTrue(Files.isDirectory(icbcSources), "找不到 icbc 源码目录：" + icbcSources.toAbsolutePath());

        List<String> tokens = Arrays.asList("LogisticsFreight", "logistics.freight", "logistics_freight");
        List<String> violations = scan(icbcSources, tokens);
        assertTrue(violations.isEmpty(),
                "icbc 里出现了物流运费的痕迹：运费不参与收购单 / 发票金额（CONTEXT.md「运费」）。"
                        + "要展示运费请在物流侧展示，不要把它接进收购金额：" + violations);
    }

    @Test
    public void testFreightTableHasNoAcquisitionOrInvoiceAmount() throws IOException {
        Path freightDo = Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module", "logistics",
                "dal", "dataobject", "freight", "LogisticsFreightOrderDO.java");
        assertTrue(Files.exists(freightDo), "找不到运费 DO：" + freightDo.toAbsolutePath());

        String content = new String(Files.readAllBytes(freightDo));
        // 允许注释里解释「不改变收购单金额」，但**不允许**出现收购 / 发票金额口径的字段名
        List<String> forbidden = Arrays.asList("acquisitionAmount", "invoiceAmount", "settlementAmount",
                "acquisition_id", "invoice_id", "settlement_id");
        List<String> violations = forbidden.stream().filter(content::contains).collect(Collectors.toList());
        assertTrue(violations.isEmpty(),
                "运费 DO 里出现了收购 / 发票金额口径：运费与货款是两本账，不能混到一张表上：" + violations);
    }

    private static boolean containsFreight(String text) {
        return text != null && text.toLowerCase().contains("freight");
    }

    private static List<String> scan(Path root, List<String> tokens) throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                String source = new String(Files.readAllBytes(path));
                for (String token : tokens) {
                    if (source.contains(token)) {
                        violations.add(path + " -> " + token);
                    }
                }
            }
        }
        return violations;
    }

}
