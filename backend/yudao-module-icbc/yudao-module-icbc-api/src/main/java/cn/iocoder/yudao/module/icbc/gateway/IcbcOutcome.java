package cn.iocoder.yudao.module.icbc.gateway;

/**
 * 一次工行出站调用的结果类型
 *
 * 工行的响应需要区分三种语义，而不是简单的成功 / 失败：
 *
 * <ul>
 *   <li>{@link #SUCCESS}：工行已受理本指令，响应码为该接口约定的成功码</li>
 *   <li>{@link #BUSINESS_FAILED}：工行明确拒绝，返回码是已知的业务错误码（可以修正后重试）</li>
 *   <li>{@link #UNKNOWN}：代理异常（-500041）、代理超时（-500042）、网关内部异常（-500099）
 *       或未知返回码。此时<strong>指令是否已到达工行未知</strong>，调用方不得重复提交，
 *       必须先调用对应的查询接口确认指令状态。见 {@link IcbcSubmitCoordinator}</li>
 * </ul>
 */
public enum IcbcOutcome {

    SUCCESS,
    BUSINESS_FAILED,
    UNKNOWN

}
