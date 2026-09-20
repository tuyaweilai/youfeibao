package cn.iocoder.yudao.module.logistics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模块边界测试：把 ADR 0032 的两条硬约束固化下来。
 *
 * <ol>
 *   <li><b>物流不依赖 icbc</b>——只在单据上存 icbc 侧的编号，不引用它的类，也不在 pom 里依赖它；
 *       追溯方向反过来，由 icbc 经 {@code logistics-api} 的读取面拉取。</li>
 *   <li><b>物流不承载工行语义</b>——没有工行 SDK、网关地址、客户端与签名/加解密痕迹。</li>
 * </ol>
 *
 * <p>与 {@code IcbcSeamBoundaryTest}（ADR 0009 的唯一缝）同一手法：扫源码文本，越界即红。
 *
 * <p><b>为什么不用「icbc」这个词做 token</b>：注释里说清「这个编号是 icbc 侧的」是正当说明，
 * 不是耦合。要锁的是**代码引用与依赖**，不是把邻居的名字当禁忌词——因此 token 取包名、
 * 类名与工件名。裸词检查会逼出「把话说糊涂」的注释，反而更难维护。
 */
public class LogisticsBoundaryTest {

    /**
     * 出现即越界的 icbc 代码痕迹（包名、类名、工件名）
     */
    private static final List<String> ICBC_CODE_TOKENS = Arrays.asList(
            "cn.iocoder.yudao.module.icbc",
            "yudao-module-icbc",
            "IcbcGateway",
            "IcbcSdk");

    /**
     * 出现即越界的工行通道痕迹（SDK、网关地址、客户端、签名与加解密属性名）
     */
    private static final List<String> ICBC_CHANNEL_TOKENS = Arrays.asList(
            "com.icbc.api",
            "gw.open.icbc.com.cn",
            "IcbcProperties",
            "DefaultIcbcClient",
            "UiIcbcClient",
            "buildPostForm",
            "sm2PrivateKey",
            "aesKey");

    @Test
    public void testNoIcbcCodeCoupling() throws IOException {
        List<String> violations = scanSources(
                concat(ICBC_CODE_TOKENS, ICBC_CHANNEL_TOKENS));

        assertTrue(violations.isEmpty(),
                "物流模块出现了 icbc 的类或工行通道痕迹（ADR 0032 要求物流只存 icbc 侧编号，不引用它的类）：" + violations);
    }

    @Test
    public void testNoIcbcDependencyInPoms() throws IOException {
        List<Path> poms = Arrays.asList(
                Path.of("pom.xml"),
                Path.of("../yudao-module-logistics-api/pom.xml"),
                Path.of("../pom.xml"));
        List<String> violations = new ArrayList<>();
        for (Path pom : poms) {
            if (!Files.exists(pom)) {
                continue;
            }
            String content = new String(Files.readAllBytes(pom));
            for (String token : ICBC_CODE_TOKENS) {
                if (content.contains(token)) {
                    violations.add(pom + " -> " + token);
                }
            }
        }
        assertTrue(violations.isEmpty(),
                "物流模块的 pom 里出现了 icbc 工件依赖（依赖方向恒为 icbc → 物流，不允许反向）：" + violations);
    }

    private List<String> scanSources(List<String> tokens) throws IOException {
        List<Path> roots = Arrays.asList(
                Path.of("src/main/java"),
                Path.of("../yudao-module-logistics-api/src/main/java"));
        List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
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
        }
        return violations;
    }

    private List<String> concat(List<String> first, List<String> second) {
        List<String> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

}
