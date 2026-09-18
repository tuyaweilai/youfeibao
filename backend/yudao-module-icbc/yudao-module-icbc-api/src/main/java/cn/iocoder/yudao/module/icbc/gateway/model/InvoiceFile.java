package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 发票文件（PDF 字节流）
 */
@Data
@Builder
public class InvoiceFile {

    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件内容
     */
    private byte[] content;

}
