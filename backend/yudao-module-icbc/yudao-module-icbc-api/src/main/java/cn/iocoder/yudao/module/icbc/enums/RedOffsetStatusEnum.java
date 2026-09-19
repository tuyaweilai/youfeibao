package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 红字确认单状态（工行 {@code redOffsetStatus}）。
 *
 * <p>红冲与普通开票一样，不是「成功 / 失败」两态，而是一条 {@code 00–11} 的状态线：
 * 申请 → 上传 → 撤销。平台把它收敛为可读的状态名，并回答两个问题：
 * <strong>这张红票有没有真正开出来</strong>（{@link #isRedInvoiceIssued()}）、
 * <strong>确认单还能不能撤销</strong>（{@link #isRevocable()}）。
 *
 * <p>映射表来自工行《聚富通开票信息预查询接口 V1》：
 * <pre>
 * 00 初始     01 申请中   02 申请成功   03 申请失败
 * 04 上传处理中 05 上传已受理 06 上传中   07 上传成功   08 上传失败
 * 09 撤销中   10 撤销成功 11 撤销失败
 * </pre>
 */
public enum RedOffsetStatusEnum {

    INITIAL(0, "00", "初始", "红冲申请已受理，等待工行处理"),
    APPLYING(1, "01", "申请中", "红冲申请处理中，可在「查状态」查看最新进度"),
    APPLIED(2, "02", "申请成功", "红字确认单已生成，等待工行上传红票"),
    APPLY_FAILED(3, "03", "申请失败", "红冲申请失败，请核对蓝票信息后重新发起"),
    UPLOAD_PROCESSING(4, "04", "上传处理中", "红票上传处理中，可在「查状态」查看最新进度"),
    UPLOAD_ACCEPTED(5, "05", "上传已受理", "红票上传已受理，等待工行处理"),
    UPLOADING(6, "06", "上传中", "红票上传中，可在「查状态」查看最新进度"),
    SUCCESS(7, "07", "红冲成功", null),
    UPLOAD_FAILED(8, "08", "上传失败", "红票上传失败，请重新发起红冲或联系工行"),
    REVOKING(9, "09", "撤销中", "红字确认单撤销中，可在「查状态」查看最新进度"),
    REVOKED(10, "10", "已撤销", null),
    REVOKE_FAILED(11, "11", "撤销失败", "红字确认单撤销失败，可再次撤销");

    private final Integer status;
    private final String code;
    private final String name;
    private final String nextAction;

    RedOffsetStatusEnum(Integer status, String code, String name, String nextAction) {
        this.status = status;
        this.code = code;
        this.name = name;
        this.nextAction = nextAction;
    }

    public Integer getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getNextAction() {
        return nextAction;
    }

    /**
     * 红票是否已真正开出（工行上传成功，红票可用）
     */
    public boolean isRedInvoiceIssued() {
        return this == SUCCESS;
    }

    /**
     * 是否失败 / 异常，需要人工关注
     */
    public boolean isException() {
        return this == APPLY_FAILED || this == UPLOAD_FAILED || this == REVOKE_FAILED;
    }

    /**
     * 是否已进入终态（无需再等待）
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == REVOKED;
    }

    /**
     * 确认单是否可撤销：红票尚未上传成功、且不处于撤销处理中 / 已撤销。
     * 撤销失败允许再次撤销。
     */
    public boolean isRevocable() {
        return this != SUCCESS && this != REVOKING && this != REVOKED;
    }

    public static Optional<RedOffsetStatusEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }

    public static Optional<RedOffsetStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    /**
     * 工行状态码转平台状态；未识别返回 {@link #INITIAL}（保留原始码另行落库，不丢信息）。
     */
    public static Integer toStatus(String code) {
        return ofCode(code).map(RedOffsetStatusEnum::getStatus).orElse(INITIAL.getStatus());
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(RedOffsetStatusEnum::getName).orElse("未知");
    }

    public static String nextActionOf(Integer status) {
        return ofStatus(status).map(RedOffsetStatusEnum::getNextAction).orElse(null);
    }

    /**
     * 已成功或已撤销的终态不被旧的进行中状态回退。
     */
    public static boolean shouldApply(Integer current, Integer next) {
        if (current == null) {
            return true;
        }
        Optional<RedOffsetStatusEnum> currentEnum = ofStatus(current);
        if (currentEnum.isPresent() && currentEnum.get().isTerminal()) {
            // 终态只接受同终态（幂等），不接受回退
            return next.equals(current);
        }
        return true;
    }
}
