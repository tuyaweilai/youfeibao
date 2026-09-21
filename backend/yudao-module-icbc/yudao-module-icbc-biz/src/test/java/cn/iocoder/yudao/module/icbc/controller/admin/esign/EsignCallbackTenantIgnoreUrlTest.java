package cn.iocoder.yudao.module.icbc.controller.admin.esign;

import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.security.TenantSecurityWebFilter;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.AntPathMatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 电子签章回调入口要真的能收通知：{@code POST /admin-api/icbc/esign/callback/notify} 必须登记进
 * 生产 {@code yudao.tenant.ignore-urls}（SPEC-1）。
 *
 * <p><b>为什么必须有这条测试</b>：Service 层单测（{@code EsignCallbackServiceTest}）全绿也照不见这个洞——
 * 第三方回调配不出我们的 {@code tenant-id} 请求头，而 {@code TenantSecurityWebFilter} 对不在
 * ignore-urls 里的 {@code /admin-api} 请求，会在进 Controller 之前就以 400 拦下；{@code @PermitAll}
 * 只解得出 Spring Security，解不出这个过滤器。这条测试因此不碰 Service，而是用**生产 application.yaml
 * 里的真实 ignore-urls 清单**装配真的 {@link TenantSecurityWebFilter}，走一遍它的 {@code doFilter}。
 *
 * <p>清单没登记回调路径时，链不会继续 → 断言失败，正好照出「回调收不到」这个洞。
 */
public class EsignCallbackTenantIgnoreUrlTest {

    private static final String CALLBACK_PATH = "/admin-api/icbc/esign/callback/notify";

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testCallbackPath_survivesTenantSecurityFilter() throws Exception {
        Set<String> ignoreUrls = loadProductionIgnoreUrls();
        assertFalse(ignoreUrls.isEmpty(), "生产 application.yaml 的 yudao.tenant.ignore-urls 不该为空");

        AntPathMatcher matcher = new AntPathMatcher();
        assertTrue(ignoreUrls.stream().anyMatch(url -> matcher.match(url, CALLBACK_PATH)),
                "回调路径 " + CALLBACK_PATH + " 必须登记进 yudao.tenant.ignore-urls，"
                        + "否则 TenantSecurityWebFilter 会在进 Controller 前以 400 拦下无 tenant-id 头的回调；"
                        + "当前清单=" + ignoreUrls);

        // 用真实清单装配过滤器，模拟第三方打来的一发「不带 tenant-id 头」的回调。
        // 忽略清单命中时两个兜底依赖（全局异常处理 / 租户框架服务）根本不会被触到，传 null 即可。
        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.setIgnoreUrls(ignoreUrls);
        TenantSecurityWebFilter filter = new TenantSecurityWebFilter(tenantProperties, new WebProperties(),
                null, null);

        TenantContextHolder.clear();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", CALLBACK_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedController = new AtomicBoolean(false);
        filter.doFilter(request, response, (req, res) -> reachedController.set(true));

        assertTrue(reachedController.get(),
                "无 tenant-id 头的回调请求必须穿过租户过滤器（当前 HTTP 状态=" + response.getStatus()
                        + "，响应=" + response.getContentAsString() + "）");
    }

    /**
     * 读取生产 {@code application.yaml} 的 {@code yudao.tenant.ignore-urls}：这是第三方回调能不能进来的
     * 真实判据，测的是配置产物本身（不是仓库里别处的副本）。
     */
    private static Set<String> loadProductionIgnoreUrls() {
        Path applicationYaml = findApplicationYaml();
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new FileSystemResource(applicationYaml.toFile()));
        Properties properties = factory.getObject();
        Set<String> ignoreUrls = new LinkedHashSet<>();
        for (int index = 0; ; index++) {
            String url = properties.getProperty("yudao.tenant.ignore-urls[" + index + "]");
            if (url == null) {
                break;
            }
            ignoreUrls.add(url);
        }
        return ignoreUrls;
    }

    /**
     * 从当前工作目录逐级上找 yudao-server 的 application.yaml，避免绑死某个 surefire 工作目录。
     */
    private static Path findApplicationYaml() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 6 && dir != null; depth++) {
            Path candidate = dir.resolve("yudao-server/src/main/resources/application.yaml");
            if (Files.exists(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("找不到 yudao-server/src/main/resources/application.yaml，"
                + "无法校验租户 ignore-urls（工作目录=" + Paths.get("").toAbsolutePath() + "）");
    }

}
