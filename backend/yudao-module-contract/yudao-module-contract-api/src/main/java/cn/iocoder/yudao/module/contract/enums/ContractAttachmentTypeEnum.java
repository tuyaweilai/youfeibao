package cn.iocoder.yudao.module.contract.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 合同附件类型枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum ContractAttachmentTypeEnum implements IntArrayValuable {

    MAIN_CONTRACT(0, "合同正文"),
    SCANNED_COPY(1, "扫描件"),
    SUPPLEMENTARY_DOCUMENT(2, "补充文件"),
    SIGNATURE_CERTIFICATE(3, "签署凭证");

    public static final int[] ARRAYS = Arrays.stream(values()).mapToInt(ContractAttachmentTypeEnum::getType).toArray();

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

} 