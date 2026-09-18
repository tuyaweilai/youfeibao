package cn.iocoder.yudao.module.contract.util;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.contract.enums.ContractStatusEnum;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 合同工具类
 *
 * @author 芋道源码
 */
public class ContractUtils {

    /**
     * 生成合同编号
     *
     * @param typeCode 合同类型编码
     * @param sequence 序号
     * @return 合同编号
     */
    public static String generateContractNo(String typeCode, int sequence) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return typeCode + dateStr + StrUtil.padPre(String.valueOf(sequence), 4, '0');
    }

    /**
     * 校验合同状态是否可以进行某个操作
     *
     * @param currentStatus 当前状态
     * @param operation 操作类型
     * @return 是否可以操作
     */
    public static boolean canPerformOperation(Integer currentStatus, String operation) {
        if (currentStatus == null) {
            return false;
        }

        switch (operation.toLowerCase()) {
            case "edit":
                return ContractStatusEnum.canEdit(currentStatus);
            case "delete":
                return ContractStatusEnum.canDelete(currentStatus);
            case "sign":
                return ContractStatusEnum.canSign(currentStatus);
            default:
                return false;
        }
    }

    /**
     * 获取合同状态描述
     *
     * @param status 状态值
     * @return 状态描述
     */
    public static String getStatusDescription(Integer status) {
        if (status == null) {
            return "未知状态";
        }

        for (ContractStatusEnum statusEnum : ContractStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum.getName();
            }
        }
        return "未知状态";
    }

    /**
     * 计算合同剩余天数
     *
     * @param expiryDate 到期日期
     * @return 剩余天数，负数表示已过期
     */
    public static long calculateRemainingDays(LocalDate expiryDate) {
        if (expiryDate == null) {
            return 0;
        }
        return LocalDate.now().until(expiryDate).getDays();
    }

    /**
     * 判断合同是否即将到期
     *
     * @param expiryDate 到期日期
     * @param warningDays 预警天数
     * @return 是否即将到期
     */
    public static boolean isExpiringSoon(LocalDate expiryDate, int warningDays) {
        if (expiryDate == null) {
            return false;
        }
        long remainingDays = calculateRemainingDays(expiryDate);
        return remainingDays >= 0 && remainingDays <= warningDays;
    }

    /**
     * 判断合同是否已过期
     *
     * @param expiryDate 到期日期
     * @return 是否已过期
     */
    public static boolean isExpired(LocalDate expiryDate) {
        if (expiryDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(expiryDate);
    }

    /**
     * 格式化合同金额（分转元）
     *
     * @param amountInCents 金额（分）
     * @return 格式化后的金额字符串
     */
    public static String formatAmount(Long amountInCents) {
        if (amountInCents == null) {
            return "0.00";
        }
        return String.format("%.2f", amountInCents / 100.0);
    }

    /**
     * 解析合同金额（元转分）
     *
     * @param amountInYuan 金额（元）
     * @return 金额（分）
     */
    public static Long parseAmount(String amountInYuan) {
        if (StrUtil.isBlank(amountInYuan)) {
            return 0L;
        }
        try {
            double yuan = Double.parseDouble(amountInYuan);
            return Math.round(yuan * 100);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

} 