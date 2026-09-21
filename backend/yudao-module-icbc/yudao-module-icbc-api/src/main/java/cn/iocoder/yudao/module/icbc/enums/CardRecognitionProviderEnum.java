package cn.iocoder.yudao.module.icbc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 卡证识别的供应商（#103，ADR 0037）。
 *
 * <p>它取代了 #93 的启动期供应商开关：现在这一栏可以在后台改、**保存后无需重启即生效**。
 * 两个取值各对应一种现场行为：
 * <ul>
 *   <li>{@link #STUB}：不启用真实识别，识别返回空 → 现场退化为手工录入（ADR 0037 的安静降级）；</li>
 *   <li>{@link #TENCENT}：走腾讯云 OCR；密钥不齐时仍返回空，页面要显著提示「未配齐」。</li>
 * </ul>
 * 后台保存时按枚举校验（{@code @InEnum}），不让一个拼错的供应商悄悄落库。
 */
@Getter
@AllArgsConstructor
public enum CardRecognitionProviderEnum implements ArrayValuable<String> {

    STUB("stub", "未启用（现场手工录入）"),
    TENCENT("tencent", "腾讯云 OCR"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(CardRecognitionProviderEnum::getCode).toArray(String[]::new);

    /** 落库取值 */
    private final String code;
    /** 展示名 */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
