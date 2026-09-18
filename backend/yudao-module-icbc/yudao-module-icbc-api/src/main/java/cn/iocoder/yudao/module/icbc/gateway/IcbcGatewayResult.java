package cn.iocoder.yudao.module.icbc.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工行出站调用结果
 *
 * 这是一个把「工行的返回码」与「平台要做的决策」连接起来的信封：
 * 平台只依赖 {@link #outcome} 做分支，不再解析工行的返回码字典。
 *
 * @param <T> 业务数据（成功时有值）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcGatewayResult<T> {

    /**
     * 结果类型
     */
    private IcbcOutcome outcome;
    /**
     * 工行返回码（未拿到响应时为 0）
     */
    private int returnCode;
    /**
     * 工行返回信息
     */
    private String returnMsg;
    /**
     * 业务数据
     */
    private T data;

    public boolean isSuccess() {
        return outcome == IcbcOutcome.SUCCESS;
    }

    /**
     * 指令状态未知：不得重复提交，须先查询
     */
    public boolean isUnknown() {
        return outcome == IcbcOutcome.UNKNOWN;
    }

    public static <T> IcbcGatewayResult<T> success(T data, int returnCode, String returnMsg) {
        return IcbcGatewayResult.<T>builder()
                .outcome(IcbcOutcome.SUCCESS).returnCode(returnCode).returnMsg(returnMsg).data(data).build();
    }

    public static <T> IcbcGatewayResult<T> businessFailed(int returnCode, String returnMsg) {
        return IcbcGatewayResult.<T>builder()
                .outcome(IcbcOutcome.BUSINESS_FAILED).returnCode(returnCode).returnMsg(returnMsg).build();
    }

    public static <T> IcbcGatewayResult<T> unknown(int returnCode, String returnMsg) {
        return IcbcGatewayResult.<T>builder()
                .outcome(IcbcOutcome.UNKNOWN).returnCode(returnCode).returnMsg(returnMsg).build();
    }

}
