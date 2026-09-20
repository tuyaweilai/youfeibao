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

    /**
     * 手机号脱敏：保留前 3 位与后 4 位。
     *
     * @param mobile 手机号；为空或长度不足 8 位时原样返回
     */
    public static String maskMobile(String mobile) {
        if (StrUtil.isBlank(mobile) || mobile.length() < 8) {
            return mobile;
        }
        return mobile.substring(0, 3)
                + StrUtil.repeat('*', mobile.length() - 7)
                + mobile.substring(mobile.length() - 4);
    }

    /**
     * 银行卡尾号：只给尾号，不整段展示。
     *
     * @param bankCardNo 银行卡号；为空时返回 {@code null}
     */
    public static String cardTail(String bankCardNo) {
        if (StrUtil.isBlank(bankCardNo)) {
            return null;
        }
        return bankCardNo.length() <= 4 ? bankCardNo : bankCardNo.substring(bankCardNo.length() - 4);
    }

    /**
     * 银行卡脱敏：只给尾号（前面补星号），用于关联单据查询 / 导出（#55 T17）。
     *
     * @param bankCardNo 银行卡号；为空时返回 {@code null}
     */
    public static String maskBankCard(String bankCardNo) {
        if (StrUtil.isBlank(bankCardNo)) {
            return null;
        }
        return "****" + cardTail(bankCardNo);
    }

    /**
     * 税号（统一社会信用代码 / 纳税人识别号）脱敏：保留前 4 位与后 4 位。
     * 长度不足 8 位时原样返回（现实中不会出现，但不要在这里抛异常把页面打挂）。
     *
     * @param taxNo 税号；为空时原样返回
     */
    public static String maskTaxNo(String taxNo) {
        if (StrUtil.isBlank(taxNo) || taxNo.length() < 8) {
            return taxNo;
        }
        return taxNo.substring(0, 4)
                + StrUtil.repeat('*', taxNo.length() - 8)
                + taxNo.substring(taxNo.length() - 4);
    }

}
