package cn.iocoder.yudao.module.icbc.enums;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.TreeSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 一致性测试：{@code icbc} 侧 Controller 上真实使用的权限注解，必须与
 * {@link RecyclingRoleEnum#allPermissions()} 一一对应。
 *
 * <p>权限收口到菜单之后，「菜单有、接口拒」或「接口有、角色没挂」都会变成静默的 403。
 * 这条测试把两侧钉死：新增接口权限时若忘了登记进 {@link RecyclingPermission} /
 * {@link RecyclingRoleEnum}，或反过来登记了却没被任何接口使用，都会失败。
 */
public class RecyclingPermissionAnnotationConsistencyTest {

    private static final Pattern PERMISSION_PATTERN =
            Pattern.compile("@ss\\.hasPermission\\('([^']+)'\\)");

    @Test
    public void testAnnotationPermissionsMatchRoleEnum() throws Exception {
        Set<String> annotatedPermissions = new TreeSet<>();
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        for (BeanDefinition beanDefinition :
                scanner.findCandidateComponents("cn.iocoder.yudao.module.icbc.controller.admin")) {
            Class<?> controllerClass = Class.forName(beanDefinition.getBeanClassName());
            for (Method method : controllerClass.getDeclaredMethods()) {
                PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
                if (preAuthorize == null) {
                    continue;
                }
                Matcher matcher = PERMISSION_PATTERN.matcher(preAuthorize.value());
                if (matcher.find()) {
                    annotatedPermissions.add(matcher.group(1));
                }
            }
        }

        Set<String> registeredPermissions = new TreeSet<>(RecyclingRoleEnum.allPermissions());
        assertEquals(registeredPermissions, annotatedPermissions,
                "回收域权限注解必须与 RecyclingRoleEnum.allPermissions() 一一对应");
    }

}
