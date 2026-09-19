package cn.iocoder.yudao.module.icbc.util;

import cn.hutool.core.util.StrUtil;

/**
 * 展示用脱敏工具。
 */
public final class MaskUtils {

    private MaskUtils() {
    }

    /**
     * 身份证号脱敏：保留前 6 位与后 4 位。
     *
     * @param idCardNo 身份证号；为空或长度不足 8 位时原样返回
     */
    public static String maskIdCard(String idCardNo) {
        if (StrUtil.isBlank(idCardNo) || idCardNo.length() < 8) {
            return idCardNo;
        }
        return idCardNo.substring(0, 6)
                + StrUtil.repeat('*', idCardNo.length() - 10)
                + idCardNo.substring(idCardNo.length() - 4);
    }

}
