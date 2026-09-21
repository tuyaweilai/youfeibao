package cn.iocoder.yudao.module.icbc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 电子签章平台参数里的「环境」（#92，ADR 0036）。
 *
 * <p>真实联调与生产用的是两套腾讯电子签环境（endpoint、应用、密钥都不同），填错环境会把
 * 测试合同推到生产、或把生产合同推到测试，所以后台保存时按枚举校验，而不是只靠前端 select 拦。
 */
@Getter
@AllArgsConstructor
public enum EsignEnvironmentEnum implements ArrayValuable<String> {

    TEST("TEST", "测试"),
    PROD("PROD", "生产"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(EsignEnvironmentEnum::getCode).toArray(String[]::new);

    /** 落库取值 */
    private final String code;
    /** 展示名 */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
