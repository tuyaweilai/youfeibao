package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * 发票上传状态（发票文件上传至税务端）。
 *
 * <p>对应工行 {@code uploadStatus}：
 * {@code 00} 未上传 / {@code 01} 处理中 / {@code 02} 已受理 / {@code 03} 上传中 / {@code 04} 上传成功 / {@code 05} 上传失败。
 * 上传是开票、缴税之外的第三条独立状态线，与缴税互不阻塞，失败时给出下一步动作。
 */
public enum UploadStatusEnum {

    NOT_UPLOADED(0, "未上传", "开票完成后由工行自动上传，无需操作"),
    PROCESSING(1, "处理中", "上传处理中，可在「查状态」看到最新进度"),
    ACCEPTED(2, "已受理", "工行已受理，等待上传完成"),
    UPLOADING(3, "上传中", "上传进行中，可稍后在「查状态」查询"),
    SUCCESS(4, "上传成功", null),
    FAILED(5, "上传失败", "发票上传失败，请在「查状态」确认后重新上传，或联系工行处理");

    /** 需人工关注的异常状态：上传失败 */
    private static final Set<Integer> EXCEPTION = Set.of(FAILED.status);

    private final Integer status;
    private final String name;
    private final String nextAction;

    UploadStatusEnum(Integer status, String name, String nextAction) {
        this.status = status;
        this.name = name;
        this.nextAction = nextAction;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getNextAction() {
        return nextAction;
    }

    /**
     * 工行 {@code uploadStatus} 码转平台状态；未识别返回空，调用方应保持本地快照不动。
     */
    public static Integer toStatus(String code) {
        return ofCode(code).map(UploadStatusEnum::getStatus).orElse(null);
    }

    public static Optional<UploadStatusEnum> ofCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        switch (code) {
            case "00":
                return Optional.of(NOT_UPLOADED);
            case "01":
                return Optional.of(PROCESSING);
            case "02":
                return Optional.of(ACCEPTED);
            case "03":
                return Optional.of(UPLOADING);
            case "04":
                return Optional.of(SUCCESS);
            case "05":
                return Optional.of(FAILED);
            default:
                return Optional.empty();
        }
    }

    public static boolean isSuccess(Integer status) {
        return SUCCESS.status.equals(status);
    }

    /**
     * 是否为需要人工关注的异常态
     */
    public static boolean isException(Integer status) {
        return status != null && EXCEPTION.contains(status);
    }

    /**
     * 全部异常状态。工作台等聚合场景按集合一次性取数，不逐个状态拼条件。
     */
    public static Set<Integer> exceptionStatuses() {
        return EXCEPTION;
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(UploadStatusEnum::getName).orElse("未知");
    }

    public static String nextActionOf(Integer status) {
        return ofStatus(status).map(UploadStatusEnum::getNextAction).orElse(null);
    }

    public static Optional<UploadStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }
}
