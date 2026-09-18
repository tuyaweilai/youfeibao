package cn.iocoder.yudao.module.contract.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同附件类型枚举
 * 
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ContractAttachmentTypeEnum {

    CONTRACT_DOCUMENT(0, "合同正文"),
    SCANNED_COPY(1, "扫描件"),
    SUPPLEMENTARY_DOCUMENT(2, "补充文件"),
    SIGNATURE_CERTIFICATE(3, "签署凭证");

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 描述
     */
    private final String description;

} 