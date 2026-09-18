package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工行发票文件类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum InvoiceFileTypeEnum {

    PDF("PDF", "PDF文件"),
    XML("XML", "XML文件"),
    OFD("OFD", "OFD文件"),
    JPG("JPG", "JPG图片"),
    PNG("PNG", "PNG图片");

    /**
     * 类型
     */
    private final String type;
    /**
     * 类型名
     */
    private final String name;

} 