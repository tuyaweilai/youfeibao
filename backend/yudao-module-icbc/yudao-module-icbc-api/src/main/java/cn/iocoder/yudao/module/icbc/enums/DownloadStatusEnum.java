package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工行发票下载状态枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum DownloadStatusEnum {

    PENDING(0, "待下载"),
    DOWNLOADING(1, "下载中"),
    SUCCESS(2, "下载成功"),
    FAILED(3, "下载失败");

    /**
     * 状态值
     */
    private final Integer status;
    
    /**
     * 状态名称
     */
    private final String name;

} 