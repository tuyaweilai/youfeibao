package cn.iocoder.yudao.module.icbc.gateway;

/**
 * 工行返回码分类
 *
 * 工行文档反复强调：当返回 {@code -500041}（代理异常）、{@code -500042}（代理超时）
 * 或「其他未知异常」时**切勿重复提交**，须先查询指令最新状态。
 * 这里把「哪些返回码算未知」收成一处，避免各业务各自判断。
 *
 * 判据：负数返回码是网关 / 传输层错误（业务返回码均为正数），外加 {@code -500099}
 * 与 {@code 99999999} 两个明确的系统异常。
 */
public final class IcbcReturnCodes {

    /**
     * 代理异常
     */
    public static final int PROXY_EXCEPTION = -500041;
    /**
     * 代理超时
     */
    public static final int PROXY_TIMEOUT = -500042;
    /**
     * 网关内部异常
     */
    public static final int GATEWAY_INTERNAL_ERROR = -500099;
    /**
     * 系统异常
     */
    public static final int SYSTEM_ERROR = 99999999;

    private IcbcReturnCodes() {
    }

    /**
     * 是否为「指令状态未知」的返回码
     */
    public static boolean isUnknown(int returnCode) {
        return returnCode < 0
                || returnCode == GATEWAY_INTERNAL_ERROR
                || returnCode == SYSTEM_ERROR;
    }

    /**
     * 分类一次响应
     *
     * @param returnCode  工行返回码
     * @param successCode 该接口约定的成功码（不同接口不同，如收方接口为 0、发票接口为 10100000）
     */
    public static IcbcOutcome classify(int returnCode, int successCode) {
        if (returnCode == 0 || returnCode == successCode) {
            return IcbcOutcome.SUCCESS;
        }
        if (isUnknown(returnCode)) {
            return IcbcOutcome.UNKNOWN;
        }
        return IcbcOutcome.BUSINESS_FAILED;
    }

}
