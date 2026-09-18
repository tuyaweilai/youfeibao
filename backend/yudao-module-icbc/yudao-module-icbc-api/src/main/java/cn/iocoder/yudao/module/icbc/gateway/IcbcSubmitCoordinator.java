package cn.iocoder.yudao.module.icbc.gateway;

/**
 * 不重复提交策略：提交结果未知时，先查询指令状态
 *
 * 工行明确要求：遇到代理异常、代理超时或未知返回码时**不得重复提交**，
 * 必须先调对应查询接口确认指令状态。这个协调器把该策略固化成一条代码路径：
 *
 * <pre>
 * 提交 → SUCCESS / BUSINESS_FAILED：直接返回
 *      → UNKNOWN：调用查询接口（只调用查询，绝不再次提交 submit）
 *                 查询成功：用 confirm 把状态翻译成提交结果
 *                 查询也未知：保持 UNKNOWN，交由调用方处置
 * </pre>
 *
 * 注意：本类只保证「不重复提交」，业务侧的幂等（同一业务单号只产生一笔业务）
 * 仍由各业务按业务单号保证。
 */
public final class IcbcSubmitCoordinator {

    private IcbcSubmitCoordinator() {
    }

    public static <T, Q> IcbcGatewayResult<T> execute(IcbcSubmitCommand<T, Q> command) {
        // 1. 提交一次，拿到结果
        IcbcGatewayResult<T> submitResult = command.submit();
        if (!submitResult.isUnknown()) {
            return submitResult;
        }
        // 2. 结果未知：查，不重复提交
        IcbcGatewayResult<Q> queryResult = command.query();
        if (queryResult.isSuccess()) {
            return command.confirm(queryResult.getData());
        }
        // 3. 查询也没确认：保持 UNKNOWN，由调用方决定下一步（例如人工介入）
        return submitResult;
    }

}
